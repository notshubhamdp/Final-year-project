package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PaymentRepository;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.service.EmailService;
import com.SRHF.SRHF.service.PaymentReceiptService;
import com.SRHF.SRHF.service.PaymentService;
import com.SRHF.SRHF.service.StripeConnectService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Payout;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final PaymentReceiptService paymentReceiptService;
    private final EmailService emailService;
    private final StripeConnectService stripeConnectService;

    @Value("${stripe.publishable.key}")
    private String stripePublishableKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    public PaymentController(PaymentService paymentService,
                             PaymentRepository paymentRepository,
                             PropertyRepository propertyRepository,
                             UserRepository userRepository,
                             PaymentReceiptService paymentReceiptService,
                             EmailService emailService,
                             StripeConnectService stripeConnectService) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.paymentReceiptService = paymentReceiptService;
        this.emailService = emailService;
        this.stripeConnectService = stripeConnectService;
    }

    private Optional<Property> resolveProperty(String propertyRef) {
        Optional<Property> byPublicId = propertyRepository.findByPropertyId(propertyRef);
        if (byPublicId.isPresent()) {
            return byPublicId;
        }
        try {
            return propertyRepository.findById(Long.valueOf(propertyRef));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private String resolvePropertyPublicId(Property property, Long fallbackId) {
        if (property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank()) {
            return property.getPropertyId();
        }
        return fallbackId != null ? String.valueOf(fallbackId) : "";
    }

    private List<Payment> sortPaymentsNewestFirst(List<Payment> payments) {
        return payments.stream()
                .sorted((left, right) -> {
                    LocalDateTime leftTime = left.getCreatedAt() != null ? left.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime rightTime = right.getCreatedAt() != null ? right.getCreatedAt() : LocalDateTime.MIN;
                    return rightTime.compareTo(leftTime);
                })
                .toList();
    }

    private Map<Long, Property> loadPropertiesById(List<Payment> payments) {
        List<Long> propertyIds = payments.stream()
                .map(Payment::getPropertyId)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Long, Property> propertyById = new HashMap<>();
        propertyRepository.findAllById(propertyIds)
                .forEach(property -> propertyById.put(property.getId(), property));
        return propertyById;
    }

    private String formatPaymentTypeLabel(String paymentType) {
        if (paymentType == null || paymentType.isBlank()) {
            return "Payment";
        }
        return switch (paymentType.toUpperCase()) {
            case "ADVANCE" -> "Advance Payment";
            case "RENT" -> "Monthly Rent";
            case "DEPOSIT" -> "Security Deposit";
            default -> paymentType.substring(0, 1).toUpperCase() + paymentType.substring(1).toLowerCase();
        };
    }

    private String formatPaymentMethodLabel(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "Online";
        }
        return paymentMethod.substring(0, 1).toUpperCase() + paymentMethod.substring(1).toLowerCase();
    }

    private String formatBookingStatusLabel(String bookingApprovalStatus) {
        if (bookingApprovalStatus == null || bookingApprovalStatus.isBlank() || "NOT_APPLICABLE".equalsIgnoreCase(bookingApprovalStatus)) {
            return null;
        }
        return switch (bookingApprovalStatus.toUpperCase()) {
            case "APPROVED" -> "Booking Approved";
            case "PENDING_APPROVAL" -> "Awaiting Landlord Approval";
            case "REJECTED" -> "Booking Rejected";
            default -> bookingApprovalStatus.replace('_', ' ');
        };
    }

    private List<PaymentHistoryItemView> buildPaymentHistoryItems(List<Payment> payments, Map<Long, Property> propertyById) {
        return sortPaymentsNewestFirst(payments).stream()
                .map(payment -> {
                    Property property = propertyById.get(payment.getPropertyId());
                    String propertyName = property != null && property.getName() != null && !property.getName().isBlank()
                            ? property.getName()
                            : "Property #" + payment.getPropertyId();

                    String propertyAddress = property != null && property.getAddress() != null
                            ? property.getAddress() + ((property.getCity() != null && !property.getCity().isBlank()) ? ", " + property.getCity() : "")
                            : "Property reference: " + resolvePropertyPublicId(property, payment.getPropertyId());

                    return new PaymentHistoryItemView(
                            payment.getId(),
                            propertyName,
                            resolvePropertyPublicId(property, payment.getPropertyId()),
                            propertyAddress,
                            payment.getAmount() != null ? payment.getAmount() : 0.0,
                            payment.getStatus(),
                            formatPaymentTypeLabel(payment.getPaymentType()),
                            formatPaymentMethodLabel(payment.getPaymentMethod()),
                            formatBookingStatusLabel(payment.getBookingApprovalStatus()),
                            payment.getDescription(),
                            payment.getCreatedAt()
                    );
                })
                .toList();
    }

    @GetMapping("/checkout")
    public String showCheckout(@RequestParam String propertyId,
                               @RequestParam Double amount,
                               @RequestParam String paymentType,
                               Authentication authentication,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("error", "Please login to make a payment.");
            return "redirect:/login";
        }

        try {
            Property property = resolveProperty(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));
            if ("ADVANCE".equalsIgnoreCase(paymentType) &&
                    ("NOT_AVAILABLE".equalsIgnoreCase(property.getAvailabilityStatus()) ||
                            "BOOKED_PENDING_APPROVAL".equalsIgnoreCase(property.getAvailabilityStatus()))) {
                redirectAttributes.addFlashAttribute("error", "Property already booked");
                return "redirect:/property/" + (property.getPropertyId() != null ? property.getPropertyId() : property.getId());
            }

            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            model.addAttribute("property", property);
            model.addAttribute("user", user);
            model.addAttribute("amount", amount);
            model.addAttribute("paymentType", paymentType);
            model.addAttribute("stripePublishableKey", stripePublishableKey);
            return "payment-checkout";
        } catch (Exception e) {
            logger.error("Error loading checkout page", e);
            redirectAttributes.addFlashAttribute("error", "Error loading payment page.");
            return "redirect:/home";
        }
    }

    @PostMapping("/create-intent")
    @ResponseBody
    public ResponseEntity<?> createPaymentIntent(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            if (authentication == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }

            String propertyRef = request.get("propertyId").toString();
            String paymentType = request.get("paymentType").toString().trim();
            String paymentMethod = request.get("paymentMethod") != null ? request.get("paymentMethod").toString() : "card";

            Property property = resolveProperty(propertyRef)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));
            if ("ADVANCE".equalsIgnoreCase(paymentType) &&
                    ("NOT_AVAILABLE".equalsIgnoreCase(property.getAvailabilityStatus()) ||
                            "BOOKED_PENDING_APPROVAL".equalsIgnoreCase(property.getAvailabilityStatus()))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Property already rented"));
            }

            User tenant = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Double amount;
            String description;
            switch (paymentType.toUpperCase()) {
                case "ADVANCE":
                    amount = property.getPrice() != null ? property.getPrice() : 0.0;
                    description = "Advance payment for property: " + property.getName();
                    break;
                case "RENT":
                    amount = property.getPrice() != null ? property.getPrice() : 0.0;
                    description = "Monthly rent for property: " + property.getName();
                    break;
                case "DEPOSIT":
                    amount = property.getPrice() != null ? property.getPrice() * 2 : 0.0;
                    description = "Security deposit for property: " + property.getName();
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid payment type"));
            }

            Long amountInPaise = Math.round(amount * 100);
            PaymentIntent paymentIntent = paymentService.createPaymentIntent(amountInPaise, "INR", description);

            Payment payment = paymentService.createPayment(
                    paymentIntent.getId(),
                    amount,
                    "INR",
                    "PENDING",
                    property.getId(),
                    tenant.getId(),
                    property.getLandlordId(),
                    paymentType.toUpperCase(),
                    paymentMethod,
                    description
            );

            Map<String, Object> response = new HashMap<>();
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("paymentId", payment.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating payment intent", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/success")
    @ResponseBody
    public ResponseEntity<?> handlePaymentSuccess(@RequestBody Map<String, Object> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId").toString();
            finalizePaymentSuccess(paymentIntentId, "callback");
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            logger.error("Error handling payment success", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> stripeWebhook(@RequestBody String payload,
                                                @RequestHeader(name = "Stripe-Signature", required = false) String sigHeader) {
        try {
            if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank() || sigHeader == null || sigHeader.isBlank()) {
                return ResponseEntity.status(400).body("missing_webhook_signature_or_secret");
            }
            Event event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow(() -> new IllegalArgumentException("Invalid payment intent payload"));
                finalizePaymentSuccess(paymentIntent.getId(), "webhook");
            } else if ("payment_intent.payment_failed".equals(event.getType()) || "payment_intent.canceled".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                        .getObject().orElseThrow(() -> new IllegalArgumentException("Invalid payment intent payload"));
                paymentService.updatePaymentStatus(paymentIntent.getId(), "FAILED");
            } else if ("payout.paid".equals(event.getType()) || "payout.failed".equals(event.getType())) {
                Payout payout = (Payout) event.getDataObjectDeserializer()
                        .getObject().orElseThrow(() -> new IllegalArgumentException("Invalid payout payload"));
                syncPayoutEvent(event.getType(), payout);
            }

            return ResponseEntity.ok("received");
        } catch (Exception e) {
            logger.error("Stripe webhook failed", e);
            return ResponseEntity.status(400).body("webhook_error");
        }
    }

    @PostMapping("/failure")
    @ResponseBody
    public ResponseEntity<?> handlePaymentFailure(@RequestBody Map<String, Object> request) {
        try {
            String paymentIntentId = request.get("paymentIntentId").toString();
            Payment payment = paymentService.updatePaymentStatus(paymentIntentId, "FAILED");
            if (payment == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Payment not found"));
            }
            return ResponseEntity.ok(Map.of("status", "failed"));
        } catch (Exception e) {
            logger.error("Error handling payment failure", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit/{paymentId}/refund-request")
    @ResponseBody
    public ResponseEntity<?> requestDepositRefund(@PathVariable Long paymentId,
                                                  @RequestBody(required = false) Map<String, Object> body,
                                                  Authentication authentication) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Double amount = null;
            if (body != null && body.get("amount") != null) {
                amount = Double.valueOf(body.get("amount").toString());
            }
            Payment payment = paymentService.requestDepositRefund(paymentId, user.getId(), amount);
            return ResponseEntity.ok(Map.of("status", payment.getDepositRefundStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit/{paymentId}/refund-approve")
    @ResponseBody
    public ResponseEntity<?> approveDepositRefund(@PathVariable Long paymentId, Authentication authentication) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            Payment payment = paymentService.approveDepositRefund(paymentId, user.getId());
            return ResponseEntity.ok(Map.of("status", payment.getDepositRefundStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit/{paymentId}/refund-complete")
    @ResponseBody
    public ResponseEntity<?> completeDepositRefund(@PathVariable Long paymentId,
                                                   @RequestBody(required = false) Map<String, Object> body) {
        try {
            String payoutRef = body != null && body.get("payoutReference") != null
                    ? body.get("payoutReference").toString()
                    : "REFUND_" + paymentId + "_" + System.currentTimeMillis();
            Payment payment = paymentService.completeDepositRefund(paymentId, payoutRef);
            return ResponseEntity.ok(Map.of("status", payment.getDepositRefundStatus(), "reference", payment.getPayoutReference()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/payouts")
    public String landlordPayouts(Authentication authentication, Model model) {
        User user = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!"LANDLORD".equalsIgnoreCase(user.getRole())) {
            return "redirect:/home";
        }

        List<Payment> payouts = paymentService.getScheduledPayouts(user.getId());
        Map<Long, String> propertyPublicIdMap = new HashMap<>();
        for (Payment payout : payouts) {
            if (!propertyPublicIdMap.containsKey(payout.getPropertyId())) {
                String publicId = propertyRepository.findById(payout.getPropertyId())
                        .map(Property::getPropertyId)
                        .filter(id -> id != null && !id.isBlank())
                        .orElse(String.valueOf(payout.getPropertyId()));
                propertyPublicIdMap.put(payout.getPropertyId(), publicId);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("payouts", payouts);
        model.addAttribute("propertyPublicIdMap", propertyPublicIdMap);
        return "payout-history";
    }

    @PostMapping("/payouts/{paymentId}/settle")
    public String settlePayoutToBank(@PathVariable Long paymentId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            User actor = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!"LANDLORD".equalsIgnoreCase(actor.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Only landlords can settle payouts.");
                return "redirect:/payment/payouts";
            }

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
            if (!actor.getId().equals(payment.getLandlordId())) {
                redirectAttributes.addFlashAttribute("error", "You cannot settle this payment.");
                return "redirect:/payment/payouts";
            }
            if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                redirectAttributes.addFlashAttribute("error", "Only completed payments can be settled.");
                return "redirect:/payment/payouts";
            }
            if ("PAID".equalsIgnoreCase(payment.getPayoutStatus())) {
                redirectAttributes.addFlashAttribute("message", "This payment is already settled.");
                return "redirect:/payment/payouts";
            }

            User landlord = stripeConnectService.syncConnectedAccountStatus(actor);
            if (!Boolean.TRUE.equals(landlord.getStripeOnboardingComplete())
                    || !Boolean.TRUE.equals(landlord.getStripePayoutsEnabled())) {
                redirectAttributes.addFlashAttribute("error", "Complete Stripe onboarding and bank setup first.");
                return "redirect:/payment/payouts";
            }

            StripeConnectService.SettlementResult settlement = stripeConnectService.settlePaymentToLandlordBank(payment, landlord);
            payment.setPayoutStatus("SCHEDULED");
            payment.setPayoutAmount(payment.getAmount());
            payment.setPayoutReference(settlement.payoutId() != null ? settlement.payoutId() : settlement.transferId());
            payment.setPayoutScheduledAt(java.time.LocalDateTime.now());
            if ("paid".equalsIgnoreCase(settlement.payoutStatus())) {
                payment.setPayoutStatus("PAID");
                payment.setPayoutProcessedAt(java.time.LocalDateTime.now());
            }
            paymentRepository.save(payment);

            redirectAttributes.addFlashAttribute("message", "Settlement initiated successfully.");
            return "redirect:/payment/payouts";
        } catch (Exception e) {
            logger.error("Failed to settle payout {}", paymentId, e);
            redirectAttributes.addFlashAttribute("error", "Settlement failed: " + e.getMessage());
            return "redirect:/payment/payouts";
        }
    }

    @PostMapping("/connect/onboard")
    public String startConnectOnboarding(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!"LANDLORD".equalsIgnoreCase(user.getRole())) {
                redirectAttributes.addFlashAttribute("error", "Only landlords can connect payouts.");
                return "redirect:/dashboard";
            }
            String onboardingUrl = stripeConnectService.createOnboardingLink(user);
            return "redirect:" + onboardingUrl;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unable to start onboarding: " + e.getMessage());
            return "redirect:/payment/payouts";
        }
    }

    @GetMapping("/connect/status")
    @ResponseBody
    public ResponseEntity<?> connectStatus(Authentication authentication) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            User synced = stripeConnectService.syncConnectedAccountStatus(user);
            Map<String, Object> response = new HashMap<>();
            response.put("connectedAccountId", synced.getStripeConnectedAccountId());
            response.put("onboardingComplete", Boolean.TRUE.equals(synced.getStripeOnboardingComplete()));
            response.put("payoutsEnabled", Boolean.TRUE.equals(synced.getStripePayoutsEnabled()));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/connect/return")
    public String connectReturn(Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            stripeConnectService.syncConnectedAccountStatus(user);
            redirectAttributes.addFlashAttribute("message", "Stripe onboarding status refreshed.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Could not refresh onboarding status.");
        }
        return "redirect:/payment/payouts";
    }

    @GetMapping("/connect/refresh")
    public String connectRefresh(Authentication authentication, RedirectAttributes redirectAttributes) {
        return startConnectOnboarding(authentication, redirectAttributes);
    }

    @GetMapping("/history")
    public String showPaymentHistory(Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }
        User user = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Payment> payments = sortPaymentsNewestFirst(paymentService.getPaymentsByTenant(user.getId()));
        Map<Long, Property> propertyById = loadPropertiesById(payments);
        List<PaymentHistoryItemView> paymentItems = buildPaymentHistoryItems(payments, propertyById);

        double totalPaidAmount = payments.stream()
                .filter(payment -> "COMPLETED".equalsIgnoreCase(payment.getStatus()))
                .mapToDouble(payment -> payment.getAmount() != null ? payment.getAmount() : 0.0)
                .sum();
        long completedPaymentCount = payments.stream()
                .filter(payment -> "COMPLETED".equalsIgnoreCase(payment.getStatus()))
                .count();

        model.addAttribute("paymentItems", paymentItems);
        model.addAttribute("user", user);
        model.addAttribute("paymentCount", payments.size());
        model.addAttribute("completedPaymentCount", completedPaymentCount);
        model.addAttribute("totalPaidAmount", totalPaidAmount);
        return "payment-history";
    }

    @GetMapping("/config")
    @ResponseBody
    public ResponseEntity<?> getStripeConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripePublishableKey));
    }

    @GetMapping("/details/{paymentId}")
    public String showPaymentDetails(@PathVariable Long paymentId, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
        if (paymentOpt.isEmpty()) {
            return "redirect:/payment/history";
        }

        Payment payment = paymentOpt.get();
        User user = userRepository.findByemail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!payment.getTenantId().equals(user.getId()) && !payment.getLandlordId().equals(user.getId())) {
            return "redirect:/payment/history";
        }

        propertyRepository.findById(payment.getPropertyId()).ifPresent(property -> model.addAttribute("property", property));
        model.addAttribute("payment", payment);
        model.addAttribute("user", user);
        return "payment-details";
    }

    @GetMapping("/success-page")
    public String showPaymentSuccess(@RequestParam String propertyId, Authentication authentication, Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Property property = resolveProperty(propertyId)
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Payment latestPayment = paymentRepository.findTopByTenantIdAndPropertyIdOrderByCreatedAtDesc(user.getId(), property.getId())
                    .orElse(null);

            model.addAttribute("property", property);
            model.addAttribute("user", user);
            model.addAttribute("payment", latestPayment);
            return "payment-success";
        } catch (Exception e) {
            logger.error("Error loading success page", e);
            return "redirect:/home";
        }
    }

    @GetMapping("/download-receipt/{paymentId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId, Authentication authentication) {
        try {
            User user = userRepository.findByemail(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Optional<Payment> paymentOpt = paymentService.getPaymentById(paymentId);
            if (paymentOpt.isEmpty() || !paymentOpt.get().getTenantId().equals(user.getId())) {
                return ResponseEntity.status(403).build();
            }
            Payment payment = paymentOpt.get();
            Property property = propertyRepository.findById(payment.getPropertyId())
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));

            byte[] pdfBytes = paymentReceiptService.generatePaymentReceipt(payment, user, property);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Payment_Receipt_" + paymentId + ".pdf");
            return ResponseEntity.ok().headers(headers).body(pdfBytes);
        } catch (Exception e) {
            logger.error("Error downloading receipt for payment {}", paymentId, e);
            return ResponseEntity.status(500).build();
        }
    }

    public static class PaymentHistoryItemView {
        private final Long paymentId;
        private final String propertyName;
        private final String propertyPublicId;
        private final String propertyAddress;
        private final Double amount;
        private final String status;
        private final String paymentTypeLabel;
        private final String paymentMethodLabel;
        private final String bookingStatusLabel;
        private final String description;
        private final LocalDateTime createdAt;

        public PaymentHistoryItemView(Long paymentId,
                                      String propertyName,
                                      String propertyPublicId,
                                      String propertyAddress,
                                      Double amount,
                                      String status,
                                      String paymentTypeLabel,
                                      String paymentMethodLabel,
                                      String bookingStatusLabel,
                                      String description,
                                      LocalDateTime createdAt) {
            this.paymentId = paymentId;
            this.propertyName = propertyName;
            this.propertyPublicId = propertyPublicId;
            this.propertyAddress = propertyAddress;
            this.amount = amount;
            this.status = status;
            this.paymentTypeLabel = paymentTypeLabel;
            this.paymentMethodLabel = paymentMethodLabel;
            this.bookingStatusLabel = bookingStatusLabel;
            this.description = description;
            this.createdAt = createdAt;
        }

        public Long getPaymentId() {
            return paymentId;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getPropertyPublicId() {
            return propertyPublicId;
        }

        public String getPropertyAddress() {
            return propertyAddress;
        }

        public Double getAmount() {
            return amount;
        }

        public String getStatus() {
            return status;
        }

        public String getPaymentTypeLabel() {
            return paymentTypeLabel;
        }

        public String getPaymentMethodLabel() {
            return paymentMethodLabel;
        }

        public String getBookingStatusLabel() {
            return bookingStatusLabel;
        }

        public String getDescription() {
            return description;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }

    private void finalizePaymentSuccess(String paymentIntentId, String source) {
        Payment payment = paymentService.updatePaymentStatus(paymentIntentId, "COMPLETED");
        if (payment == null) {
            throw new IllegalArgumentException("Payment not found");
        }

        try {
            User tenant = userRepository.findById(payment.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
            Property property = propertyRepository.findById(payment.getPropertyId())
                    .orElseThrow(() -> new IllegalArgumentException("Property not found"));

            if ("ADVANCE".equalsIgnoreCase(payment.getPaymentType())) {
                payment.setBookingApprovalStatus("PENDING_APPROVAL");
                paymentRepository.save(payment);
                property.setAvailabilityStatus("BOOKED_PENDING_APPROVAL");
                propertyRepository.save(property);

                userRepository.findById(property.getLandlordId()).ifPresent(landlord -> {
                    String landlordName = (landlord.getFirstName() + " " + landlord.getLastName()).trim();
                    String tenantName = (tenant.getFirstName() + " " + tenant.getLastName()).trim();
                    emailService.sendLandlordPropertyBookedEmail(
                            landlord.getEmail(),
                            landlordName,
                            property.getName(),
                            property.getPropertyId(),
                            tenantName,
                            payment.getAmount()
                    );
                });
            }

            paymentReceiptService.generatePaymentReceipt(payment, tenant, property);
            String fullName = tenant.getFirstName() + " " + tenant.getLastName();
            emailService.sendPaymentSuccessEmail(tenant.getEmail(), fullName, payment.getPaymentType(), payment.getAmount());
            logger.info("Payment confirmed via {} for payment {}", source, paymentIntentId);
        } catch (Exception e) {
            logger.error("Post-payment processing failed for {}", paymentIntentId, e);
        }
    }

    private void syncPayoutEvent(String eventType, Payout payout) {
        if (payout == null) {
            return;
        }
        String paymentIdRaw = payout.getMetadata() != null ? payout.getMetadata().get("paymentId") : null;
        Optional<Payment> paymentOpt = Optional.empty();
        if (paymentIdRaw != null && !paymentIdRaw.isBlank()) {
            try {
                paymentOpt = paymentRepository.findById(Long.parseLong(paymentIdRaw));
            } catch (NumberFormatException ignored) {
            }
        }
        if (paymentOpt.isEmpty()) {
            paymentOpt = paymentRepository.findByPayoutReference(payout.getId());
        }
        if (paymentOpt.isEmpty()) {
            return;
        }

        Payment payment = paymentOpt.get();
        payment.setPayoutReference(payout.getId());
        if ("payout.paid".equals(eventType)) {
            payment.setPayoutStatus("PAID");
            payment.setPayoutProcessedAt(java.time.LocalDateTime.now());
        } else if ("payout.failed".equals(eventType)) {
            payment.setPayoutStatus("HOLD");
        }
        paymentRepository.save(payment);
    }
}
