package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.VisitSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitScheduleRepository extends JpaRepository<VisitSchedule, Long> {
    List<VisitSchedule> findByTenantIdOrderByRequestedStartDesc(Long tenantId);

    List<VisitSchedule> findByLandlordIdOrderByRequestedStartDesc(Long landlordId);

    List<VisitSchedule> findByStatusInAndReminderSentFalseAndApprovedStartBetween(
            List<String> statuses,
            LocalDateTime from,
            LocalDateTime to
    );
}
