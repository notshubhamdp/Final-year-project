package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Payment;
import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.repository.PaymentRepository;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class RentReminderSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(RentReminderSchedulerService.class);

    private final PaymentRepository paymentRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public RentReminderSchedulerService(PaymentRepository paymentRepository,
                                        PropertyRepository propertyRepository,
                                        UserRepository userRepository,
                                        EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    // Runs once daily at 09:00 IST to send monthly rent reminders.
    @Scheduled(cron = "${app.rent-reminder.cron:0 0 9 * * *}", zone = "${app.rent-reminder.zone:Asia/Kolkata}")
    public void sendMonthlyRentReminders() {
        LocalDate today = LocalDate.now();

        List<Payment> completedAdvancePayments = paymentRepository
                .findByStatusAndPaymentTypeOrderByCreatedAtAsc("COMPLETED", "ADVANCE");

        // Keep latest booking per tenant-property pair.
        Map<String, Payment> latestBookingByKey = new HashMap<>();
        for (Payment payment : completedAdvancePayments) {
            if (payment.getTenantId() == null || payment.getPropertyId() == null || payment.getCreatedAt() == null) {
                continue;
            }
            if (!"APPROVED".equalsIgnoreCase(payment.getBookingApprovalStatus())) {
                continue;
            }
            String key = payment.getTenantId() + ":" + payment.getPropertyId();
            Payment existing = latestBookingByKey.get(key);
            if (existing == null || payment.getCreatedAt().isAfter(existing.getCreatedAt())) {
                latestBookingByKey.put(key, payment);
            }
        }

        int sentCount = 0;
        for (Payment booking : latestBookingByKey.values()) {
            try {
                int bookedDay = booking.getCreatedAt().getDayOfMonth();
                LocalDate dueDateThisMonth = getDueDateForMonth(YearMonth.from(today), bookedDay);

                if (!today.equals(dueDateThisMonth)) {
                    continue;
                }

                LocalDate previousDueDate = getDueDateForMonth(YearMonth.from(today.minusMonths(1)), bookedDay);
                LocalDateTime cycleStart = previousDueDate.plusDays(1).atStartOfDay();
                LocalDateTime cycleEnd = dueDateThisMonth.atTime(23, 59, 59);

                List<Payment> rentPayments = paymentRepository
                        .findByTenantIdAndPropertyIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
                                booking.getTenantId(),
                                booking.getPropertyId(),
                                "COMPLETED",
                                "RENT"
                        );

                boolean rentAlreadyPaidForCycle = rentPayments.stream()
                        .map(Payment::getCreatedAt)
                        .filter(Objects::nonNull)
                        .anyMatch(paidAt -> !paidAt.isBefore(cycleStart) && !paidAt.isAfter(cycleEnd));

                if (rentAlreadyPaidForCycle) {
                    continue;
                }

                Optional<User> tenantOpt = userRepository.findById(booking.getTenantId());
                Optional<Property> propertyOpt = propertyRepository.findById(booking.getPropertyId());
                if (tenantOpt.isEmpty() || propertyOpt.isEmpty()) {
                    continue;
                }

                User tenant = tenantOpt.get();
                Property property = propertyOpt.get();

                String tenantName = (tenant.getFirstName() + " " + tenant.getLastName()).trim();
                String propertyPublicId = property.getPropertyId() != null && !property.getPropertyId().isBlank()
                        ? property.getPropertyId()
                        : String.valueOf(property.getId());

                emailService.sendMonthlyRentReminderEmail(
                        tenant.getEmail(),
                        tenantName,
                        property.getName(),
                        propertyPublicId,
                        dueDateThisMonth,
                        property.getPrice()
                );
                sentCount++;
            } catch (Exception e) {
                logger.error("Failed to process rent reminder for payment {}", booking.getId(), e);
            }
        }

        logger.info("Monthly rent reminder job completed. Emails sent: {}", sentCount);
    }

    private LocalDate getDueDateForMonth(YearMonth yearMonth, int bookedDay) {
        int day = Math.min(bookedDay, yearMonth.lengthOfMonth());
        return yearMonth.atDay(day);
    }
}
