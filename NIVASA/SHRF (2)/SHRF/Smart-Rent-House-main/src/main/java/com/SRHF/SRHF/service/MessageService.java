package com.SRHF.SRHF.service;

import com.SRHF.SRHF.entity.Message;
import com.SRHF.SRHF.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Send a message between tenant and landlord
     */
    public Message sendMessage(Long senderId, Long receiverId, Long propertyId, String content) {
        
        logger.info("Attempting to send message - Sender: {}, Receiver: {}, Property: {}, Content length: {}", 
                   senderId, receiverId, propertyId, content != null ? content.length() : 0);
        
        if (content == null || content.trim().isEmpty()) {
            logger.warn("Message content is empty");
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        if (senderId == null || senderId <= 0) {
            logger.error("Invalid sender ID: {}", senderId);
            throw new IllegalArgumentException("Invalid sender ID");
        }

        if (receiverId == null || receiverId <= 0) {
            logger.error("Invalid receiver ID: {}", receiverId);
            throw new IllegalArgumentException("Invalid receiver ID");
        }

        if (propertyId == null || propertyId <= 0) {
            logger.error("Invalid property ID: {}", propertyId);
            throw new IllegalArgumentException("Invalid property ID");
        }
        
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setPropertyId(propertyId);
        message.setContent(content.trim());
        message.setTimestamp(LocalDateTime.now());
        message.setRead(false);

        try {
            Message savedMessage = messageRepository.save(message);
            logger.info("Message saved successfully with ID: {}", savedMessage.getId());
            return savedMessage;
        } catch (Exception e) {
            logger.error("Error saving message to database", e);
            throw new RuntimeException("Failed to save message: " + e.getMessage(), e);
        }
    }

    /**
     * Get conversation between two users for a specific property
     */
    public List<Message> getConversation(Long user1, Long user2, Long propertyId) {
        
        logger.info("Fetching conversation - User1: {}, User2: {}, Property: {}", user1, user2, propertyId);
        
        if (propertyId == null || propertyId <= 0) {
            logger.error("Invalid property ID for conversation: {}", propertyId);
            throw new IllegalArgumentException("Invalid property ID");
        }
        
        // Mark messages as read when viewing conversation
        List<Message> messages = messageRepository.findConversationMessages(propertyId, user1, user2);
        
        logger.info("Found {} messages in conversation", messages.size());
        
        // Mark all messages directed to user1 as read
        messages.stream()
                .filter(m -> m.getReceiverId().equals(user1) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                    logger.debug("Marked message {} as read", m.getId());
                });
        
        return messages;
    }

    /**
     * Get all conversations for a user
     */
    public List<Message> getUserConversations(Long userId) {
        logger.info("Fetching all conversations for user: {}", userId);
        return messageRepository.findBySenderIdOrReceiverIdOrderByTimestampDesc(userId, userId);
    }

    /**
     * Get unread message count for a user
     */
    public Long getUnreadMessageCount(Long userId) {
        Long count = messageRepository.countByReceiverIdAndIsReadFalse(userId);
        logger.info("Unread message count for user {}: {}", userId, count);
        return count;
    }

    /**
     * Mark a message as read
     */
    public void markMessageAsRead(Long messageId) {
        logger.info("Marking message {} as read", messageId);
        messageRepository.findById(messageId).ifPresent(message -> {
            message.setRead(true);
            messageRepository.save(message);
            logger.info("Message {} marked as read", messageId);
        });
    }

    /**
     * Delete a message
     */
    public void deleteMessage(Long messageId) {
        logger.info("Deleting message: {}", messageId);
        messageRepository.deleteById(messageId);
        logger.info("Message {} deleted", messageId);
    }
}
