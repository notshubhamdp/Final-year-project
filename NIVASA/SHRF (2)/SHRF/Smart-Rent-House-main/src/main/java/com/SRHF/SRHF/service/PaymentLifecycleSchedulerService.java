package com.SRHF.SRHF.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PaymentLifecycleSchedulerService {

    private final PaymentService paymentService;

    public PaymentLifecycleSchedulerService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "${app.payment.late-fee.cron:0 0 1 * * *}", zone = "${app.payment.zone:Asia/Kolkata}")
    public void applyLateFees() {
        paymentService.applyLateFeesForAllDuePayments();
    }

    @Scheduled(cron = "${app.payment.payout.cron:0 */30 * * * *}", zone = "${app.payment.zone:Asia/Kolkata}")
    public void processPayouts() {
        paymentService.processDuePayouts();
    }

    @Scheduled(cron = "${app.payment.booking-refund.cron:0 */1 * * * *}", zone = "${app.payment.zone:Asia/Kolkata}")
    public void processBookingRefunds() {
        paymentService.processPendingBookingRefunds();
    }
}
