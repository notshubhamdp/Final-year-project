package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, Long> {
    List<ChatbotMessage> findTop20ByConversationIdOrderByCreatedAtDesc(Long conversationId);
    List<ChatbotMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}

