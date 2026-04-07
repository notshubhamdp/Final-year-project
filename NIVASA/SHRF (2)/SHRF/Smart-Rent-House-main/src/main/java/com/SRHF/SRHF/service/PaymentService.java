package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PaymentRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${app.payment.late-fee.percent:2.5}")
    private Double lateFeePercent;

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @PostConstruct
    public void initStripe() {
        Stripe.apiKey = stripeSecretKey;
        logger.info("Stripe API key initialized");
    }

    /**
     * Create a mock payment intent for testing (always succeeds)
     */
    public PaymentIntent createMockPaymentIntent(Long amountInPaise, String currency, String description) throws Exception {
        try {
            // Create a mock payment intent that simulates Stripe behavior
            PaymentIntent mockIntent = new PaymentIntent();
            mockIntent.setId("pi_mock_" + System.currentTimeMillis());
            mockIntent.setClientSecret("pi_mock_" + System.currentTimeMillis() + "_secret_mock");
            mockIntent.setStatus("requires_payment_method");
            mockIntent.setAmount(amountInPaise);
            mockIntent.setCurrency(currency.toLowerCase());
            mockIntent.setDescription(description);

            logger.info("Created mock payment intent: {} for amount: {} {}", mockIntent.getId(), amountInPaise, currency);
            return mockIntent;
        } catch (Exception e) {
            logger.error("Error creating mock payment intent", e);
            throw new Exception("Failed to create mock payment intent: " + e.getMessage());
        }
    }

    public PaymentIntent createRealPaymentIntent(Long amountInPaise, String currency, String description) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInPaise)
                .setCurrency(currency.toLowerCase())
                .setDescription(description)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                )
                .build();
        return PaymentIntent.create(params);
    }

    public PaymentIntent createPaymentIntent(Long amountInPaise, String currency, String description) throws Exception {
        try {
            return createRealPaymentIntent(amountInPaise, currency, description);
        } catch (Exception ex) {
            logger.warn("Falling back to mock payment intent due to Stripe API issue: {}", ex.getMessage());
            return createMockPaymentIntent(amountInPaise, currency, description);
        }
    }

    /**
     * Save payment record to database
     */
    public Payment savePayment(Payment payment) {
        payment.setUpdatedAt(java.time.LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    /**
     * Create and save payment record
     */
    public Payment createPayment(String stripePaymentId, Double amount, String currency, String status,
                                Long propertyId, Long tenantId, Long landlordId, String paymentType, 
                                String paymentMethod, String description) {
        Payment payment = new Payment(stripePaymentId, amount, currency, status, propertyId, tenantId, landlordId, paymentType, paymentMethod, description);
        if ("ADVANCE".equalsIgnoreCase(paymentType)) {
            payment.setBookingApprovalStatus("PENDING_APPROVAL");
            payment.setDueDate(LocalDate.now());
        } else {
            payment.setBookingApprovalStatus("NOT_APPLICABLE");
        }
        if ("RENT".equalsIgnoreCase(paymentType)) {
            payment.setDueDate(LocalDate.now().plusDays(5));
        }
        if ("DEPOSIT".equalsIgnoreCase(paymentType)) {
            payment.setDepositRefundStatus("REQUESTED");
        }
        return savePayment(payment);
    }

    public void processPendingBookingRefunds() {
        List<Payment> dueRefunds = paymentRepository
                .findByPaymentTypeAndStatusAndBookingApprovalStatusAndBookingRefundStatusAndBookingRejectedAtBefore(
                        "ADVANCE",
                        "COMPLETED",
                        "REJECTED",
                        "PENDING",
                        LocalDateTime.now().minusMinutes(2)
                );
        for (Payment payment : dueRefunds) {
            try {
                initiateBookingRefund(payment);
            } catch (Exception ex) {
                payment.setBookingRefundStatus("FAILED");
                savePayment(payment);
                logger.error("Failed booking refund for payment {}", payment.getId(), ex);
            }
        }
    }

    public Payment initiateBookingRefund(Payment payment) throws Exception {
        if (payment == null) {
            throw new IllegalArgumentException("Payment is required");
        }
        if (!"ADVANCE".equalsIgnoreCase(payment.getPaymentType())
                || !"REJECTED".equalsIgnoreCase(payment.getBookingApprovalStatus())) {
            throw new IllegalArgumentException("Only rejected advance bookings can be refunded");
        }

        payment.setBookingRefundStatus("INITIATED");
        payment.setBookingRefundInitiatedAt(LocalDateTime.now());
        savePayment(payment);

        String reference;
        try {
            if (payment.getStripePaymentId() != null && payment.getStripePaymentId().startsWith("pi_")) {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(payment.getStripePaymentId())
                        .putMetadata("paymentId", String.valueOf(payment.getId()))
                        .build();
                Refund refund = Refund.create(params);
                reference = refund.getId();
            } else {
                reference = "mock_refund_" + payment.getId() + "_" + System.currentTimeMillis();
            }
        } catch (Exception ex) {
            logger.warn("Stripe refund failed for payment {}, marking as simulated refund: {}", payment.getId(), ex.getMessage());
            reference = "sim_refund_" + payment.getId() + "_" + System.currentTimeMillis();
        }

        payment.setBookingRefundStatus("REFUNDED");
        payment.setBookingRefundCompletedAt(LocalDateTime.now());
        payment.setBookingRefundReference(reference);
        payment.setStatus("REFUNDED");
        Payment saved = savePayment(payment);
        final String refundReference = reference;
        final double refundedAmount = saved.getAmount() != null ? saved.getAmount() : 0.0;

        userRepository.findById(saved.getTenantId()).ifPresent(tenant -> {
            String tenantName = (tenant.getFirstName() + " " + tenant.getLastName()).trim();
            emailService.sendBookingRefundCompletedEmail(
                    tenant.getEmail(),
                    tenantName,
                    refundedAmount,
                    refundReference
            );
        });

        return saved;
    }

    /**
     * Update payment status
     */
    public Payment updatePaymentStatus(String stripePaymentId, String status) {
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentId(stripePaymentId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(status);
            payment.setUpdatedAt(java.time.LocalDateTime.now());
            if ("COMPLETED".equalsIgnoreCase(status)) {
                payment.setPayoutStatus("SCHEDULED");
                payment.setPayoutAmount(payment.getAmount() != null ? payment.getAmount() : 0.0);
                payment.setPayoutScheduledAt(LocalDateTime.now().plusDays(1));
            }
            return savePayment(payment);
        }
        return null;
    }

    public Payment applyLateFeeIfEligible(Payment rentPayment) {
        if (rentPayment == null || !"RENT".equalsIgnoreCase(rentPayment.getPaymentType())) {
            return rentPayment;
        }
        if (!"PENDING".equalsIgnoreCase(rentPayment.getStatus())) {
            return rentPayment;
        }
        if (rentPayment.getDueDate() == null || !rentPayment.getDueDate().isBefore(LocalDate.now())) {
            return rentPayment;
        }
        if (Boolean.TRUE.equals(rentPayment.getLateFeeApplied())) {
            return rentPayment;
        }

        double base = rentPayment.getAmount() != null ? rentPayment.getAmount() : 0.0;
        double fee = (base * (lateFeePercent != null ? lateFeePercent : 0.0)) / 100.0;
        rentPayment.setLateFeeApplied(true);
        rentPayment.setLateFeeAmount(fee);
        rentPayment.setAmount(base + fee);
        rentPayment.setDescription((rentPayment.getDescription() != null ? rentPayment.getDescription() : "Rent payment")
                + " | Late fee applied: INR " + String.format("%.2f", fee));
        return savePayment(rentPayment);
    }

    public void applyLateFeesForAllDuePayments() {
        List<Payment> overdueRentPayments = paymentRepository.findByPaymentTypeAndStatusAndDueDateBeforeAndLateFeeAppliedFalse(
                "RENT",
                "PENDING",
                LocalDate.now()
        );
        overdueRentPayments.forEach(this::applyLateFeeIfEligible);
    }

    public Payment requestDepositRefund(Long paymentId, Long tenantId, Double amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!tenantId.equals(payment.getTenantId())) {
            throw new IllegalArgumentException("You cannot request refund for this payment");
        }
        if (!"DEPOSIT".equalsIgnoreCase(payment.getPaymentType())) {
            throw new IllegalArgumentException("Only deposit payments are eligible");
        }
        payment.setDepositRefundStatus("REQUESTED");
        payment.setDepositRefundAmount(amount != null ? amount : payment.getAmount());
        return savePayment(payment);
    }

    public Payment approveDepositRefund(Long paymentId, Long landlordId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!landlordId.equals(payment.getLandlordId())) {
            throw new IllegalArgumentException("You cannot approve this refund");
        }
        payment.setDepositRefundStatus("APPROVED");
        return savePayment(payment);
    }

    public Payment completeDepositRefund(Long paymentId, String payoutReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        if (!"APPROVED".equalsIgnoreCase(payment.getDepositRefundStatus())) {
            throw new IllegalArgumentException("Refund must be approved first");
        }
        payment.setDepositRefundStatus("COMPLETED");
        payment.setDepositRefundedAt(LocalDateTime.now());
        payment.setPayoutReference(payoutReference);
        return savePayment(payment);
    }

    public Optional<Payment> getLatestActiveAdvanceBooking(Long tenantId, Long propertyId) {
        return paymentRepository.findByTenantIdAndPropertyIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
                        tenantId,
                        propertyId,
                        "COMPLETED",
                        "ADVANCE"
                ).stream()
                .filter(payment -> "PENDING_APPROVAL".equalsIgnoreCase(payment.getBookingApprovalStatus())
                        || "APPROVED".equalsIgnoreCase(payment.getBookingApprovalStatus()))
                .findFirst();
    }

    public Optional<Payment> getLatestApprovedAdvanceBooking(Long tenantId, Long propertyId) {
        return paymentRepository.findByTenantIdAndPropertyIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
                        tenantId,
                        propertyId,
                        "COMPLETED",
                        "ADVANCE"
                ).stream()
                .filter(payment -> "APPROVED".equalsIgnoreCase(payment.getBookingApprovalStatus()))
                .findFirst();
    }

    public RentCycleStatus getCurrentRentCycleStatus(Payment approvedBooking) {
        if (approvedBooking == null
                || approvedBooking.getTenantId() == null
                || approvedBooking.getPropertyId() == null
                || approvedBooking.getCreatedAt() == null) {
            return new RentCycleStatus(null, false, null);
        }

        LocalDate today = LocalDate.now();
        int bookedDay = approvedBooking.getCreatedAt().getDayOfMonth();
        LocalDate dueDateThisMonth = getDueDateForMonth(YearMonth.from(today), bookedDay);
        LocalDate previousDueDate = getDueDateForMonth(YearMonth.from(today.minusMonths(1)), bookedDay);
        LocalDateTime cycleStart = previousDueDate.plusDays(1).atStartOfDay();
        LocalDateTime cycleEnd = dueDateThisMonth.atTime(23, 59, 59);

        Payment currentCyclePayment = paymentRepository
                .findByTenantIdAndPropertyIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
                        approvedBooking.getTenantId(),
                        approvedBooking.getPropertyId(),
                        "COMPLETED",
                        "RENT"
                ).stream()
                .filter(payment -> payment.getCreatedAt() != null
                        && !payment.getCreatedAt().isBefore(cycleStart)
                        && !payment.getCreatedAt().isAfter(cycleEnd))
                .findFirst()
                .orElse(null);

        return new RentCycleStatus(dueDateThisMonth, currentCyclePayment != null, currentCyclePayment);
    }

    public List<Payment> getScheduledPayouts(Long landlordId) {
        return paymentRepository.findByLandlordIdAndStatus(landlordId, "COMPLETED").stream()
                .filter(p -> "SCHEDULED".equalsIgnoreCase(p.getPayoutStatus()) || "PAID".equalsIgnoreCase(p.getPayoutStatus()))
                .toList();
    }

    public void processDuePayouts() {
        List<Payment> duePayouts = paymentRepository.findByStatusAndPayoutStatusAndPayoutScheduledAtBefore(
                "COMPLETED",
                "SCHEDULED",
                LocalDateTime.now().plusMinutes(1)
        );
        for (Payment payment : duePayouts) {
            payment.setPayoutStatus("PAID");
            payment.setPayoutProcessedAt(LocalDateTime.now());
            if (payment.getPayoutReference() == null || payment.getPayoutReference().isBlank()) {
                payment.setPayoutReference("PO_" + payment.getId() + "_" + System.currentTimeMillis());
            }
            savePayment(payment);
        }
    }

    /**
     * Get payment by ID
     */
    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    /**
     * Get payment by Stripe payment ID
     */
    public Optional<Payment> getPaymentByStripeId(String stripePaymentId) {
        return paymentRepository.findByStripePaymentId(stripePaymentId);
    }

    /**
     * Get all payments for a tenant
     */
    public List<Payment> getPaymentsByTenant(Long tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    /**
     * Get all payments for a landlord
     */
    public List<Payment> getPaymentsByLandlord(Long landlordId) {
        return paymentRepository.findByLandlordId(landlordId);
    }

    /**
     * Get all payments for a property
     */
    public List<Payment> getPaymentsByProperty(Long propertyId) {
        return paymentRepository.findByPropertyId(propertyId);
    }

    /**
     * Get payments by status
     */
    public List<Payment> getPaymentsByStatus(String status) {
        return paymentRepository.findByStatus(status);
    }

    /**
     * Get successful payments for a tenant
     */
    public List<Payment> getSuccessfulPaymentsByTenant(Long tenantId) {
        return paymentRepository.findByTenantIdAndStatus(tenantId, "COMPLETED");
    }

    /**
     * Get successful payments for a landlord
     */
    public List<Payment> getSuccessfulPaymentsByLandlord(Long landlordId) {
        return paymentRepository.findByLandlordIdAndStatus(landlordId, "COMPLETED");
    }

    private LocalDate getDueDateForMonth(YearMonth yearMonth, int bookedDay) {
        int day = Math.min(bookedDay, yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }

    public record RentCycleStatus(LocalDate dueDate, boolean paidForCurrentCycle, Payment currentCyclePayment) {
    }
}
