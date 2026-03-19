package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTenantId(Long tenantId);

    List<Payment> findByLandlordId(Long landlordId);

    List<Payment> findByPropertyId(Long propertyId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByStripePaymentId(String stripePaymentId);

    List<Payment> findByTenantIdAndStatus(Long tenantId, String status);

    List<Payment> findByLandlordIdAndStatus(Long landlordId, String status);

    List<Payment> findByLandlordIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
            Long landlordId,
            String status,
            String paymentType
    );

    List<Payment> findByLandlordIdAndStatusAndPaymentTypeAndBookingApprovalStatusOrderByCreatedAtDesc(
            Long landlordId,
            String status,
            String paymentType,
            String bookingApprovalStatus
    );

    List<Payment> findByStatusAndPaymentTypeOrderByCreatedAtAsc(String status, String paymentType);

    List<Payment> findByTenantIdAndPropertyIdAndStatusAndPaymentTypeOrderByCreatedAtDesc(
            Long tenantId,
            Long propertyId,
            String status,
            String paymentType
    );

    Optional<Payment> findTopByTenantIdAndPropertyIdOrderByCreatedAtDesc(Long tenantId, Long propertyId);
    Optional<Payment> findByPayoutReference(String payoutReference);

    List<Payment> findByPaymentTypeAndStatusAndDueDateBeforeAndLateFeeAppliedFalse(
            String paymentType,
            String status,
            LocalDate dueDate
    );

    List<Payment> findByStatusAndPayoutStatusAndPayoutScheduledAtBefore(
            String status,
            String payoutStatus,
            LocalDateTime cutoff
    );

    List<Payment> findByPaymentTypeAndStatusAndBookingApprovalStatusAndBookingRefundStatusAndBookingRejectedAtBefore(
            String paymentType,
            String status,
            String bookingApprovalStatus,
            String bookingRefundStatus,
            LocalDateTime rejectedBefore
    );

    void deleteByTenantIdOrLandlordId(Long tenantId, Long landlordId);
}
