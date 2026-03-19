package com.SRHF.SRHF.controller;

import com.SRHF.SRHF.entity.Property;
import com.SRHF.SRHF.entity.User;
import com.SRHF.SRHF.entity.Message;
import com.SRHF.SRHF.repository.PropertyRepository;
import com.SRHF.SRHF.repository.UserRepository;
import com.SRHF.SRHF.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/messages")
public class MessageController {

    private static final Logger logger = LoggerFactory.getLogger(MessageController.class);
    private final MessageService messageService;
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;

    public MessageController(MessageService messageService,
                             UserRepository userRepository,
                             PropertyRepository propertyRepository) {
        this.messageService = messageService;
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
    }

    /**
     * Display all conversations for the current user
     */
    @GetMapping("/conversations")
    public String conversations(Authentication authentication, Model model) {
        try {
            if (authentication == null) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userRepository.findByemail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            List<Message> messages = messageService.getUserConversations(user.getId());
            Map<String, ConversationSummary> summaryMap = new LinkedHashMap<>();

            for (Message msg : messages) {
                if (msg == null || msg.getSenderId() == null || msg.getReceiverId() == null) {
                    logger.warn("Skipping malformed message record while building conversations");
                    continue;
                }
                Long otherUserId = msg.getSenderId().equals(user.getId()) ? msg.getReceiverId() : msg.getSenderId();
                if (otherUserId == null) {
                    logger.warn("Skipping message {} due to null otherUserId", msg.getId());
                    continue;
                }

                Long messagePropertyId = msg.getPropertyId();
                if (messagePropertyId == null || messagePropertyId <= 0) {
                    logger.warn("Skipping message {} due to invalid propertyId {}", msg.getId(), messagePropertyId);
                    continue;
                }
                String key = messagePropertyId + ":" + otherUserId;

                // Keep only the newest message per (property, other user) conversation.
                if (summaryMap.containsKey(key)) {
                    continue;
                }

                Property property = propertyRepository.findById(messagePropertyId).orElse(null);
                User otherUser = userRepository.findById(otherUserId).orElse(null);

                String propertyName = (property != null && property.getName() != null && !property.getName().isBlank())
                        ? property.getName()
                        : "Property #" + messagePropertyId;
                String propertyPublicId = (property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank())
                        ? property.getPropertyId()
                        : String.valueOf(messagePropertyId);

                String ownerName = (property != null && property.getOwnerName() != null && !property.getOwnerName().isBlank())
                        ? property.getOwnerName()
                        : formatUserName(otherUser);

                boolean unread = !msg.isRead() && msg.getReceiverId().equals(user.getId());
                boolean sentByCurrentUser = msg.getSenderId().equals(user.getId());
                String lastMessage = (msg.getContent() == null || msg.getContent().isBlank())
                        ? "(No message content)"
                        : msg.getContent();
                LocalDateTime lastMessageAt = msg.getTimestamp() != null
                        ? msg.getTimestamp()
                        : (msg.getCreatedAt() != null ? msg.getCreatedAt() : LocalDateTime.now());

                summaryMap.put(key, new ConversationSummary(
                        otherUserId,
                        messagePropertyId,
                        propertyName,
                        propertyPublicId,
                        ownerName,
                        formatUserName(otherUser),
                        lastMessage,
                        lastMessageAt,
                        unread,
                        sentByCurrentUser
                ));
            }

            model.addAttribute("conversations", new ArrayList<>(summaryMap.values()));
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("unreadCount",
                    messageService.getUnreadMessageCount(user.getId()));

            return "conversations";
        } catch (Exception e) {
            logger.error("Error loading conversations", e);
            model.addAttribute("error", "Failed to load conversations: " + e.getMessage());
            model.addAttribute("conversations", new ArrayList<ConversationSummary>());
            model.addAttribute("unreadCount", 0L);
            return "conversations";
        }
    }

    /**
     * Display conversation between tenant and landlord for a specific property
     */
    @GetMapping("/conversation/{receiverId}/{propertyRef}")
    public String conversation(@PathVariable Long receiverId,
                               @PathVariable String propertyRef,
                               Authentication authentication,
                               Model model) {
        try {
            if (authentication == null) {
                return "redirect:/login";
            }

            String email = authentication.getName();
            User user = userRepository.findByemail(email)
                    .orElseThrow(() -> new RuntimeException("Current user not found with email: " + email));

            User receiver = userRepository.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found with ID: " + receiverId));

            Property property = propertyRepository.findByPropertyId(propertyRef).orElse(null);
            if (property == null) {
                try {
                    Long internalPropertyId = Long.valueOf(propertyRef);
                    property = propertyRepository.findById(internalPropertyId).orElse(null);
                } catch (NumberFormatException ignored) {
                    // no-op
                }
            }
            if (property == null) {
                throw new RuntimeException("Invalid property ID: " + propertyRef);
            }

            Long propertyId = property.getId();
            String displayPropertyId = (property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank())
                    ? property.getPropertyId()
                    : String.valueOf(propertyId);

            model.addAttribute("messages",
                    messageService.getConversation(user.getId(), receiverId, propertyId));
            model.addAttribute("receiver", receiver);
            model.addAttribute("receiverId", receiverId);
            model.addAttribute("propertyId", propertyId);
            model.addAttribute("displayPropertyId", displayPropertyId);
            model.addAttribute("currentUserId", user.getId());

            return "conversation";
        } catch (RuntimeException e) {
            logger.error("Error loading conversation", e);
            return "redirect:/messages/conversations?error=" + encodeErrorMessage("Failed to load conversation: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error loading conversation", e);
            return "redirect:/messages/conversations?error=" + encodeErrorMessage("An unexpected error occurred while loading the conversation");
        }
    }

    /**
     * Send a message between tenant and landlord
     */
    @PostMapping("/send")
    public String sendMessage(@RequestParam(required = false) Long receiverId,
                              @RequestParam(required = false) Long propertyId,
                              @RequestParam(required = false) String content,
                              Authentication authentication) {
        try {
            logger.info("POST /messages/send - received request");
            logger.info("Parameters - receiverId: {}, propertyId: {}, contentLength: {}",
                    receiverId, propertyId, content != null ? content.length() : 0);

            if (authentication == null) {
                logger.warn("Authentication is null, redirecting to login");
                return "redirect:/login";
            }

            if (receiverId == null || propertyId == null || content == null) {
                logger.error("Missing required parameters - receiverId: {}, propertyId: {}, content is null: {}",
                        receiverId, propertyId, content == null);
                return "redirect:/messages/conversations?error=Missing required parameters";
            }

            String email = authentication.getName();
            logger.info("Authenticated user email: {}", email);

            User sender = userRepository.findByemail(email)
                    .orElseThrow(() -> {
                        logger.error("User not found with email: {}", email);
                        return new RuntimeException("User not found");
                    });

            logger.info("Found sender user - ID: {}, Name: {} {}", sender.getId(), sender.getFirstName(), sender.getLastName());

            messageService.sendMessage(
                    sender.getId(),
                    receiverId,
                    propertyId,
                    content
            );

            logger.info("Message sent successfully from {} to {} for property {}", sender.getId(), receiverId, propertyId);
            Property property = propertyRepository.findById(propertyId).orElse(null);
            String propertyRef = (property != null && property.getPropertyId() != null && !property.getPropertyId().isBlank())
                    ? property.getPropertyId()
                    : String.valueOf(propertyId);
            return "redirect:/messages/conversation/" + receiverId + "/" + propertyRef;

        } catch (IllegalArgumentException e) {
            logger.warn("Validation error: {}", e.getMessage());
            return "redirect:/messages/conversations?error=" + encodeErrorMessage(e.getMessage());
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return "redirect:/messages/conversations?error=Failed to send message: " + encodeErrorMessage(e.getMessage());
        }
    }

    /**
     * Encode error message for URL
     */
    private String encodeErrorMessage(String message) {
        try {
            return java.net.URLEncoder.encode(message, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return "Error";
        }
    }

    /**
     * API endpoint: Send message (AJAX support)
     */
    @PostMapping("/api/send")
    @ResponseBody
    public ResponseEntity<?> sendMessageApi(@RequestParam Long receiverId,
                                            @RequestParam Long propertyId,
                                            @RequestParam String content,
                                            Authentication authentication) {
        try {
            if (authentication == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "User not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            String email = authentication.getName();
            User sender = userRepository.findByemail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            logger.info("Sending message - Sender: {}, Receiver: {}, Property: {}", sender.getId(), receiverId, propertyId);

            var message = messageService.sendMessage(
                    sender.getId(),
                    receiverId,
                    propertyId,
                    content
            );

            logger.info("Message sent successfully via API - Message ID: {}", message.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", message);
            response.put("messageId", message.getId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid message content: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            logger.error("Error sending message via API", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to send message: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * API endpoint: Get unread message count
     */
    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<?> getUnreadCount(Authentication authentication) {
        try {
            if (authentication == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "User not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            String email = authentication.getName();
            User user = userRepository.findByemail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Long unreadCount = messageService.getUnreadMessageCount(user.getId());
            logger.info("Retrieved unread message count for user {}: {}", user.getId(), unreadCount);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("unreadCount", unreadCount);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting unread count", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to get unread count");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * API endpoint: Mark message as read
     */
    @PostMapping("/api/mark-read/{messageId}")
    @ResponseBody
    public ResponseEntity<?> markAsRead(@PathVariable Long messageId) {
        try {
            if (messageId == null || messageId <= 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Invalid message ID");
                return ResponseEntity.badRequest().body(error);
            }

            messageService.markMessageAsRead(messageId);
            logger.info("Message {} marked as read", messageId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messageId", messageId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error marking message as read", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to mark message as read");
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a message
     */
    @PostMapping("/delete/{messageId}")
    public String deleteMessage(@PathVariable Long messageId) {
        try {
            if (messageId == null || messageId <= 0) {
                logger.warn("Invalid message ID for deletion: {}", messageId);
                return "redirect:/messages/conversations?error=Invalid message ID";
            }

            logger.info("Deleting message: {}", messageId);
            messageService.deleteMessage(messageId);
            logger.info("Message {} deleted successfully", messageId);
            return "redirect:/messages/conversations";
        } catch (Exception e) {
            logger.error("Error deleting message", e);
            return "redirect:/messages/conversations?error=Failed to delete message";
        }
    }

    private String formatUserName(User user) {
        if (user == null) {
            return "Unknown User";
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? "User #" + user.getId() : fullName;
    }

    public static class ConversationSummary {
        private final Long otherUserId;
        private final Long propertyId;
        private final String propertyPublicId;
        private final String propertyName;
        private final String ownerName;
        private final String otherUserName;
        private final String lastMessage;
        private final LocalDateTime lastMessageAt;
        private final boolean unread;
        private final boolean sentByCurrentUser;

        public ConversationSummary(Long otherUserId,
                                   Long propertyId,
                                   String propertyName,
                                   String propertyPublicId,
                                   String ownerName,
                                   String otherUserName,
                                   String lastMessage,
                                   LocalDateTime lastMessageAt,
                                   boolean unread,
                                   boolean sentByCurrentUser) {
            this.otherUserId = otherUserId;
            this.propertyId = propertyId;
            this.propertyName = propertyName;
            this.propertyPublicId = propertyPublicId;
            this.ownerName = ownerName;
            this.otherUserName = otherUserName;
            this.lastMessage = lastMessage;
            this.lastMessageAt = lastMessageAt;
            this.unread = unread;
            this.sentByCurrentUser = sentByCurrentUser;
        }

        public Long getOtherUserId() { return otherUserId; }
        public Long getPropertyId() { return propertyId; }
        public String getPropertyPublicId() { return propertyPublicId; }
        public String getPropertyName() { return propertyName; }
        public String getOwnerName() { return ownerName; }
        public String getOtherUserName() { return otherUserName; }
        public String getLastMessage() { return lastMessage; }
        public LocalDateTime getLastMessageAt() { return lastMessageAt; }
        public boolean isUnread() { return unread; }
        public boolean isSentByCurrentUser() { return sentByCurrentUser; }
    }
}
