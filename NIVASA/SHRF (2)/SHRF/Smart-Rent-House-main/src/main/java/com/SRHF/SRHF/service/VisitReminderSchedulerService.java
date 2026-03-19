package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.entity.VisitSchedule;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.repository.VisitScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class VisitReminderSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(VisitReminderSchedulerService.class);

    private final VisitScheduleService visitScheduleService;
    private final VisitScheduleRepository visitScheduleRepository;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final EmailService emailService;

    public VisitReminderSchedulerService(VisitScheduleService visitScheduleService,
                                         VisitScheduleRepository visitScheduleRepository,
                                         UserRepository userRepository,
                                         PropertyRepository propertyRepository,
                                         EmailService emailService) {
        this.visitScheduleService = visitScheduleService;
        this.visitScheduleRepository = visitScheduleRepository;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.visit-reminder.cron:0 0 * * * *}", zone = "${app.visit-reminder.zone:Asia/Kolkata}")
    public void sendVisitReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24h = now.plusHours(24);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

        visitScheduleService.getUpcomingForReminder(now, next24h).forEach(visit -> {
            try {
                Optional<User> tenantOpt = userRepository.findById(visit.getTenantId());
                Optional<User> landlordOpt = userRepository.findById(visit.getLandlordId());
                Optional<Property> propertyOpt = propertyRepository.findById(visit.getPropertyId());
                if (tenantOpt.isEmpty() || landlordOpt.isEmpty() || propertyOpt.isEmpty()) {
                    return;
                }

                String slot = visit.getApprovedStart() != null ? visit.getApprovedStart().format(fmt) : "scheduled time";
                String subject = "Visit Reminder - NIVASA";
                String body = "Reminder: Property visit for " + propertyOpt.get().getName() + " is scheduled at " + slot + ".";

                emailService.sendSimpleNotification(tenantOpt.get().getEmail(), subject, body);
                emailService.sendSimpleNotification(landlordOpt.get().getEmail(), subject, body);

                visit.setReminderSent(true);
                visitScheduleRepository.save(visit);
            } catch (Exception ex) {
                logger.error("Failed to send visit reminder for visit {}", visit.getId(), ex);
            }
        });
    }
}
