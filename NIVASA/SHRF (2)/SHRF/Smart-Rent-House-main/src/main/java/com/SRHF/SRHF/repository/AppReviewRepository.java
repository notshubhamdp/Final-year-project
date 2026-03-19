package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.AppReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppReviewRepository extends JpaRepository<AppReview, Long> {
    Optional<AppReview> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    List<AppReview> findTop3ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(r.rating), 0) FROM AppReview r")
    Long sumAllRatings();
}
