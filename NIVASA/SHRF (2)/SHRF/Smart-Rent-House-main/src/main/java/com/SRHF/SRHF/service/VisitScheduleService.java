package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.VisitSchedule;
import com.SRHF.SRHF.repository.VisitScheduleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VisitScheduleService {

    private final VisitScheduleRepository visitScheduleRepository;

    public VisitScheduleService(VisitScheduleRepository visitScheduleRepository) {
        this.visitScheduleRepository = visitScheduleRepository;
    }

    public VisitSchedule createRequest(Long propertyId,
                                       Long tenantId,
                                       Long landlordId,
                                       LocalDateTime start,
                                       LocalDateTime end,
                                       String tenantNote) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("Invalid visit slot");
        }
        VisitSchedule visit = new VisitSchedule();
        visit.setPropertyId(propertyId);
        visit.setTenantId(tenantId);
        visit.setLandlordId(landlordId);
        visit.setRequestedStart(start);
        visit.setRequestedEnd(end);
        visit.setTenantNote(tenantNote);
        visit.setStatus("REQUESTED");
        return visitScheduleRepository.save(visit);
    }

    public List<VisitSchedule> getForTenant(Long tenantId) {
        return visitScheduleRepository.findByTenantIdOrderByRequestedStartDesc(tenantId);
    }

    public List<VisitSchedule> getForLandlord(Long landlordId) {
        return visitScheduleRepository.findByLandlordIdOrderByRequestedStartDesc(landlordId);
    }

    public Optional<VisitSchedule> getById(Long id) {
        return visitScheduleRepository.findById(id);
    }

    public VisitSchedule approve(VisitSchedule visit, Long landlordId, String note) {
        if (!landlordId.equals(visit.getLandlordId())) {
            throw new IllegalArgumentException("You cannot approve this visit");
        }
        visit.setStatus("APPROVED");
        visit.setApprovedStart(visit.getRequestedStart());
        visit.setApprovedEnd(visit.getRequestedEnd());
        visit.setLandlordNote(note);
        visit.setReminderSent(false);
        return visitScheduleRepository.save(visit);
    }

    public VisitSchedule reschedule(VisitSchedule visit,
                                    Long landlordId,
                                    LocalDateTime newStart,
                                    LocalDateTime newEnd,
                                    String note) {
        if (!landlordId.equals(visit.getLandlordId())) {
            throw new IllegalArgumentException("You cannot reschedule this visit");
        }
        if (newStart == null || newEnd == null || !newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("Invalid reschedule slot");
        }
        visit.setStatus("RESCHEDULED");
        visit.setApprovedStart(newStart);
        visit.setApprovedEnd(newEnd);
        visit.setLandlordNote(note);
        visit.setReminderSent(false);
        return visitScheduleRepository.save(visit);
    }

    public VisitSchedule reject(VisitSchedule visit, Long landlordId, String note) {
        if (!landlordId.equals(visit.getLandlordId())) {
            throw new IllegalArgumentException("You cannot reject this visit");
        }
        visit.setStatus("REJECTED");
        visit.setLandlordNote(note);
        return visitScheduleRepository.save(visit);
    }

    public VisitSchedule cancelByTenant(VisitSchedule visit, Long tenantId, String note) {
        if (!tenantId.equals(visit.getTenantId())) {
            throw new IllegalArgumentException("You cannot cancel this visit");
        }
        String status = visit.getStatus() != null ? visit.getStatus().toUpperCase() : "";
        if (!status.equals("REQUESTED") && !status.equals("APPROVED") && !status.equals("RESCHEDULED")) {
            throw new IllegalArgumentException("This visit cannot be cancelled");
        }
        visit.setStatus("CANCELLED");
        visit.setTenantNote(note != null && !note.isBlank() ? note : visit.getTenantNote());
        return visitScheduleRepository.save(visit);
    }

    public List<VisitSchedule> getUpcomingForReminder(LocalDateTime from, LocalDateTime to) {
        return visitScheduleRepository.findByStatusInAndReminderSentFalseAndApprovedStartBetween(
                List.of("APPROVED", "RESCHEDULED"),
                from,
                to
        );
    }
}
