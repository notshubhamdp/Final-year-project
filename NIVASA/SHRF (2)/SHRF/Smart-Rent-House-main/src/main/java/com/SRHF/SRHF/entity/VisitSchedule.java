package com.SRHF.SRHF.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "visit_schedules")
public class VisitSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "landlord_id", nullable = false)
    private Long landlordId;

    @Column(name = "requested_start", nullable = false)
    private LocalDateTime requestedStart;

    @Column(name = "requested_end", nullable = false)
    private LocalDateTime requestedEnd;

    @Column(name = "approved_start")
    private LocalDateTime approvedStart;

    @Column(name = "approved_end")
    private LocalDateTime approvedEnd;

    @Column(name = "status", nullable = false)
    private String status; // REQUESTED, APPROVED, RESCHEDULED, REJECTED, CANCELLED

    @Column(name = "tenant_note", columnDefinition = "TEXT")
    private String tenantNote;

    @Column(name = "landlord_note", columnDefinition = "TEXT")
    private String landlordNote;

    @Column(name = "reminder_sent")
    private Boolean reminderSent;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public VisitSchedule() {
        this.status = "REQUESTED";
        this.reminderSent = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "REQUESTED";
        }
        if (reminderSent == null) {
            reminderSent = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getRequestedStart() {
        return requestedStart;
    }

    public void setRequestedStart(LocalDateTime requestedStart) {
        this.requestedStart = requestedStart;
    }

    public LocalDateTime getRequestedEnd() {
        return requestedEnd;
    }

    public void setRequestedEnd(LocalDateTime requestedEnd) {
        this.requestedEnd = requestedEnd;
    }

    public LocalDateTime getApprovedStart() {
        return approvedStart;
    }

    public void setApprovedStart(LocalDateTime approvedStart) {
        this.approvedStart = approvedStart;
    }

    public LocalDateTime getApprovedEnd() {
        return approvedEnd;
    }

    public void setApprovedEnd(LocalDateTime approvedEnd) {
        this.approvedEnd = approvedEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTenantNote() {
        return tenantNote;
    }

    public void setTenantNote(String tenantNote) {
        this.tenantNote = tenantNote;
    }

    public String getLandlordNote() {
        return landlordNote;
    }

    public void setLandlordNote(String landlordNote) {
        this.landlordNote = landlordNote;
    }

    public Boolean getReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
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
