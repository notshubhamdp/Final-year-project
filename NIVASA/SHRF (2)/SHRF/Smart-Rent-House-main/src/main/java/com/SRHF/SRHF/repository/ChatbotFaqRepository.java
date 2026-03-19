package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.ChatbotFaq;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotFaqRepository extends JpaRepository<ChatbotFaq, Long> {
    List<ChatbotFaq> findByEnabledTrueOrderBySortOrderAscIdAsc();
}

