package com.SRHF.SRHF.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "stripe_payment_id", unique = true)
    private String stripePaymentId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "status", nullable = false)
    private String status; // "PENDING", "COMPLETED", "FAILED", "CANCELLED"

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "landlord_id", nullable = false)
    private Long landlordId;

    @Column(name = "payment_type", nullable = false)
    private String paymentType; // "ADVANCE", "RENT", "DEPOSIT"

    @Column(name = "payment_method")
    private String paymentMethod; // "card", "upi", "netbanking"

    @Column(name = "description")
    private String description;

    @Column(name = "booking_approval_status")
    private String bookingApprovalStatus; // PENDING_APPROVAL, APPROVED, REJECTED, NOT_APPLICABLE

    @Column(name = "booking_refund_status")
    private String bookingRefundStatus; // NOT_APPLICABLE, PENDING, INITIATED, REFUNDED, FAILED

    @Column(name = "booking_rejected_at")
    private LocalDateTime bookingRejectedAt;

    @Column(name = "booking_refund_initiated_at")
    private LocalDateTime bookingRefundInitiatedAt;

    @Column(name = "booking_refund_completed_at")
    private LocalDateTime bookingRefundCompletedAt;

    @Column(name = "booking_refund_reference")
    private String bookingRefundReference;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "late_fee_applied")
    private Boolean lateFeeApplied;

    @Column(name = "late_fee_amount")
    private Double lateFeeAmount;

    @Column(name = "deposit_refund_status")
    private String depositRefundStatus; // NOT_APPLICABLE, REQUESTED, APPROVED, COMPLETED, REJECTED

    @Column(name = "deposit_refund_amount")
    private Double depositRefundAmount;

    @Column(name = "deposit_refunded_at")
    private LocalDateTime depositRefundedAt;

    @Column(name = "payout_status")
    private String payoutStatus; // PENDING, SCHEDULED, PAID, HOLD

    @Column(name = "payout_amount")
    private Double payoutAmount;

    @Column(name = "payout_scheduled_at")
    private LocalDateTime payoutScheduledAt;

    @Column(name = "payout_processed_at")
    private LocalDateTime payoutProcessedAt;

    @Column(name = "payout_reference")
    private String payoutReference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Payment() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lateFeeApplied = false;
        this.lateFeeAmount = 0.0;
        this.depositRefundStatus = "NOT_APPLICABLE";
        this.depositRefundAmount = 0.0;
        this.payoutStatus = "PENDING";
        this.payoutAmount = 0.0;
        this.bookingRefundStatus = "NOT_APPLICABLE";
    }

    public Payment(String stripePaymentId, Double amount, String currency, String status,
                   Long propertyId, Long tenantId, Long landlordId, String paymentType, String paymentMethod, String description) {
        this.stripePaymentId = stripePaymentId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.propertyId = propertyId;
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.paymentType = paymentType;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.bookingApprovalStatus = "NOT_APPLICABLE";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.lateFeeApplied = false;
        this.lateFeeAmount = 0.0;
        this.depositRefundStatus = "NOT_APPLICABLE";
        this.depositRefundAmount = 0.0;
        this.payoutStatus = "PENDING";
        this.payoutAmount = 0.0;
        this.bookingRefundStatus = "NOT_APPLICABLE";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStripePaymentId() {
        return stripePaymentId;
    }

    public void setStripePaymentId(String stripePaymentId) {
        this.stripePaymentId = stripePaymentId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(Long landlordId) {
        this.landlordId = landlordId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBookingApprovalStatus() {
        return bookingApprovalStatus;
    }

    public void setBookingApprovalStatus(String bookingApprovalStatus) {
        this.bookingApprovalStatus = bookingApprovalStatus;
    }

    public String getBookingRefundStatus() {
        return bookingRefundStatus;
    }

    public void setBookingRefundStatus(String bookingRefundStatus) {
        this.bookingRefundStatus = bookingRefundStatus;
    }

    public LocalDateTime getBookingRejectedAt() {
        return bookingRejectedAt;
    }

    public void setBookingRejectedAt(LocalDateTime bookingRejectedAt) {
        this.bookingRejectedAt = bookingRejectedAt;
    }

    public LocalDateTime getBookingRefundInitiatedAt() {
        return bookingRefundInitiatedAt;
    }

    public void setBookingRefundInitiatedAt(LocalDateTime bookingRefundInitiatedAt) {
        this.bookingRefundInitiatedAt = bookingRefundInitiatedAt;
    }

    public LocalDateTime getBookingRefundCompletedAt() {
        return bookingRefundCompletedAt;
    }

    public void setBookingRefundCompletedAt(LocalDateTime bookingRefundCompletedAt) {
        this.bookingRefundCompletedAt = bookingRefundCompletedAt;
    }

    public String getBookingRefundReference() {
        return bookingRefundReference;
    }

    public void setBookingRefundReference(String bookingRefundReference) {
        this.bookingRefundReference = bookingRefundReference;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Boolean getLateFeeApplied() {
        return lateFeeApplied;
    }

    public void setLateFeeApplied(Boolean lateFeeApplied) {
        this.lateFeeApplied = lateFeeApplied;
    }

    public Double getLateFeeAmount() {
        return lateFeeAmount;
    }

    public void setLateFeeAmount(Double lateFeeAmount) {
        this.lateFeeAmount = lateFeeAmount;
    }

    public String getDepositRefundStatus() {
        return depositRefundStatus;
    }

    public void setDepositRefundStatus(String depositRefundStatus) {
        this.depositRefundStatus = depositRefundStatus;
    }

    public Double getDepositRefundAmount() {
        return depositRefundAmount;
    }

    public void setDepositRefundAmount(Double depositRefundAmount) {
        this.depositRefundAmount = depositRefundAmount;
    }

    public LocalDateTime getDepositRefundedAt() {
        return depositRefundedAt;
    }

    public void setDepositRefundedAt(LocalDateTime depositRefundedAt) {
        this.depositRefundedAt = depositRefundedAt;
    }

    public String getPayoutStatus() {
        return payoutStatus;
    }

    public void setPayoutStatus(String payoutStatus) {
        this.payoutStatus = payoutStatus;
    }

    public Double getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(Double payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public LocalDateTime getPayoutScheduledAt() {
        return payoutScheduledAt;
    }

    public void setPayoutScheduledAt(LocalDateTime payoutScheduledAt) {
        this.payoutScheduledAt = payoutScheduledAt;
    }

    public LocalDateTime getPayoutProcessedAt() {
        return payoutProcessedAt;
    }

    public void setPayoutProcessedAt(LocalDateTime payoutProcessedAt) {
        this.payoutProcessedAt = payoutProcessedAt;
    }

    public String getPayoutReference() {
        return payoutReference;
    }

    public void setPayoutReference(String payoutReference) {
        this.payoutReference = payoutReference;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
