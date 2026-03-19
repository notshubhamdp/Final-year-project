package com.SRHF.SRHF.repository;

import com.SRHF.SRHF.entity.ChatbotConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatbotConversationRepository extends JpaRepository<ChatbotConversation, Long> {
    Optional<ChatbotConversation> findByConversationKey(String conversationKey);
}

