# Messaging System Documentation

## Overview
The Smart Rent House platform now includes a secure messaging system that enables direct communication between tenants and landlords regarding specific properties.

## Features
- **Direct Messaging**: Send messages between tenants and landlords
- **Property Context**: Messages can be linked to specific properties
- **Read Status**: Track which messages have been read
- **Conversation History**: View full conversation thread with timestamps
- **Unread Count**: Get count of unread messages
- **Message Timestamps**: Automatic timestamp management for sent and read times

## Architecture

### Database Schema

#### Messages Table
```sql
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    property_id BIGINT,
    message_text TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    FOREIGN KEY (sender_id) REFERENCES user(id),
    FOREIGN KEY (recipient_id) REFERENCES user(id),
    FOREIGN KEY (property_id) REFERENCES property(id),
    INDEX idx_sender_recipient (sender_id, recipient_id),
    INDEX idx_property (property_id),
    INDEX idx_created_at (created_at)
);
```

### Entity Classes

#### Message.java
- **Location**: `src/main/java/com/SRHF/SRHF/entity/Message.java`
- **Fields**:
  - `id`: Primary key (Long)
  - `sender`: User who sent the message (ManyToOne with User)
  - `recipient`: User who received the message (ManyToOne with User)
  - `property`: Property the message relates to (ManyToOne with Property, optional)
  - `messageText`: The message content (String, TEXT column)
  - `isRead`: Whether the message has been read (Boolean, default: false)
  - `createdAt`: Timestamp when message was sent (LocalDateTime, auto-set on @PrePersist)
  - `readAt`: Timestamp when message was read (LocalDateTime, nullable)

### Repository Layer

#### MessageRepository
- **Location**: `src/main/java/com/SRHF/SRHF/repository/MessageRepository.java`
- **Methods**:
  - `findConversation(User sender, User recipient)`: Get all messages between two users
  - `findConversationByProperty(User sender, User recipient, Property property)`: Get messages for a specific property
  - `findByRecipientOrderByCreatedAtDesc(User recipient)`: Get all received messages
  - `findUnreadMessages(User user)`: Get unread messages
  - `findBySenderOrderByCreatedAtDesc(User sender)`: Get all sent messages
  - `countUnreadMessages(User user)`: Count unread messages
  - `findConversationPartners(User user)`: Get list of users to have conversations with

### Service Layer

#### MessageService
- **Location**: `src/main/java/com/SRHF/SRHF/service/MessageService.java`
- **Methods**:
  - `sendMessage(User sender, User recipient, String messageText, Property property)`: Send a new message
  - `getConversation(User user1, User user2)`: Get full conversation between two users
  - `getPropertyConversation(User user1, User user2, Property property)`: Get property-specific conversation
  - `getUserMessages(User user)`: Get all messages (sent and received)
  - `getInbox(User user)`: Get received messages only
  - `getSentMessages(User user)`: Get sent messages only
  - `getUnreadMessages(User user)`: Get unread messages
  - `getUnreadCount(User user)`: Count unread messages
  - `markAsRead(Long messageId, User currentUser)`: Mark message as read
  - `getMessage(Long messageId, User currentUser)`: Get single message with permission check
  - `getConversationPartners(User user)`: Get unique users to message
  - `getConversationPreview(User user, User partner)`: Get last message and metadata for a conversation
  - `getAllConversationPreviews(User user)`: Get all conversation previews for inbox
  - `deleteMessage(Long messageId, User currentUser)`: Delete message

### Controller Layer

#### MessagingController
- **Location**: `src/main/java/com/SRHF/SRHF/controller/MessagingController.java`
- **Routes**:

##### View Routes
- `GET /messages/conversations` - Display conversations list
- `GET /messages/conversation/{userId}` - View conversation with specific user
- `GET /messages/conversation/{userId}/{propertyId}` - View conversation about specific property

##### Form Routes
- `POST /messages/send` - Send message via form

##### API Routes
- `POST /messages/api/send` - Send message via JSON (returns JSON response)
- `GET /messages/api/unread-count` - Get unread message count (JSON)
- `POST /messages/api/{messageId}/read` - Mark message as read (JSON)
- `DELETE /messages/api/{messageId}` - Delete message (JSON)

### Templates

#### messages-conversations.html
- **Location**: `src/main/resources/templates/messages-conversations.html`
- **Features**:
  - List of all conversations
  - Unread message count badge
  - Last message preview
  - Conversation partners with avatars
  - Timestamp of last message
  - Link to detailed conversation view
  - No conversations state

#### messages-detail.html
- **Location**: `src/main/resources/templates/messages-detail.html`
- **Features**:
  - Full message thread
  - Message bubbles (sent/received styling)
  - Timestamps for each message
  - Compose form to send new message
  - Auto-expanding textarea
  - Auto-scroll to bottom
  - Property context display (if applicable)

## Security Features

### Access Control
- Users can only view their own messages
- Users can only mark their own received messages as read
- Users can only delete messages they sent or received
- API endpoints require authentication

### Ownership Verification
All endpoints verify that the current user is either:
1. The sender of the message
2. The recipient of the message

Attempting to access/modify messages you don't own returns a 400 Bad Request with error message.

### Input Validation
- Message text is trimmed and validated to not be empty
- Message text has 5000 character limit
- Cannot send messages to yourself
- Required fields (sender, recipient, messageText) are validated

## Usage Workflows

### Sending a Message from Property Detail Page
1. Tenant clicks "💬 Message Landlord" button on property detail page
2. System routes to conversation page with pre-filled recipient (landlord) and property
3. Tenant types message and clicks "Send"
4. Message is saved to database and conversation view is updated

### Viewing Conversations
1. Tenant/Landlord clicks "Messages" in navigation
2. System loads `/messages/conversations`
3. All conversation partners are displayed with preview of last message
4. Unread count is displayed
5. Click on conversation to open full thread

### Reading Messages
1. Click on a conversation to open detailed view
2. All messages in conversation appear with timestamps
3. Unread messages from the recipient are automatically marked as read
4. Current user's messages appear on right (blue), other user's on left (gray)

## Integration Points

### Property Detail Page
- Added "💬 Message Landlord" button
- Routes to `/messages/conversation/{landlordId}/{propertyId}`
- Allows tenants to message landlord about specific property

### Tenant Dashboard
- Added "💬 Messages" link in navigation
- Routes to `/messages/conversations`
- Shows unread badge if there are unread messages

### Landlord Integration
- Landlords can receive messages from tenants
- Can respond to tenant inquiries about their properties
- Can view all conversations

## Database Migration

The Message table is automatically created by Hibernate when the application starts (due to `ddl-auto: update` in application.yml).

### Manual SQL (for reference)
```sql
CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    recipient_id BIGINT NOT NULL,
    property_id BIGINT,
    message_text TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME,
    FOREIGN KEY (sender_id) REFERENCES user(id),
    FOREIGN KEY (recipient_id) REFERENCES user(id),
    FOREIGN KEY (property_id) REFERENCES property(id),
    INDEX idx_sender_recipient (sender_id, recipient_id),
    INDEX idx_property (property_id),
    INDEX idx_created_at (created_at)
);
```

## Error Handling

All service methods include validation and throw `IllegalArgumentException` for:
- Missing required parameters
- Permission violations
- Invalid message content
- Non-existent messages

Controller endpoints catch these exceptions and return appropriate error responses.

## Future Enhancements

### Planned Features
1. **Message Encryption**: Encrypt sensitive message content at rest
2. **Real-time Notifications**: WebSocket support for instant message notifications
3. **File Attachments**: Allow users to attach documents/images to messages
4. **Message Search**: Full-text search across messages
5. **Conversation Archiving**: Archive conversations without deletion
6. **Message Reactions**: Add emoji reactions to messages
7. **Typing Indicators**: Show when recipient is typing
8. **Message Receipts**: Show when message was delivered vs read
9. **Message Drafts**: Save draft messages before sending
10. **Bulk Actions**: Delete multiple messages at once

### Possible Improvements
- Add message content validation/sanitization
- Implement rate limiting to prevent spam
- Add conversation muting/blocking
- Implement read receipts (double checkmarks)
- Add message forwarding capability
- Create message templates for common inquiries

## Testing

### Unit Tests (to create)
```java
@SpringBootTest
@Transactional
public class MessageServiceTest {
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PropertyRepository propertyRepository;
    
    // Test cases for all MessageService methods
}
```

### Integration Tests (to create)
```java
@SpringBootTest
@WithMockUser(username = "tenant@test.com")
public class MessagingControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    // Test cases for all MessagingController endpoints
}
```

## Deployment Checklist

- [x] Message entity created with JPA annotations
- [x] MessageRepository created with custom queries
- [x] MessageService created with business logic
- [x] MessagingController created with all endpoints
- [x] messages-conversations.html template created
- [x] messages-detail.html template created
- [x] Property detail page updated with message button
- [x] Tenant dashboard updated with messages link
- [ ] Database migration tested on staging
- [ ] Unit tests created and passing
- [ ] Integration tests created and passing
- [ ] Load testing for concurrent messaging
- [ ] Security audit for access control
- [ ] Documentation completed

## Support & Troubleshooting

### Common Issues

**Issue**: Messages not sending
- **Solution**: Verify users exist in database, check server logs for validation errors

**Issue**: Unread count not updating
- **Solution**: Clear browser cache, verify user is authenticated

**Issue**: Cannot see conversation partners
- **Solution**: Ensure you have sent or received messages, check database connection

### Debug Mode

Enable detailed logging by setting in application.yml:
```yaml
logging:
  level:
    com.SRHF.SRHF.service.MessageService: DEBUG
    com.SRHF.SRHF.controller.MessagingController: DEBUG
```

## Contact

For issues or feature requests regarding the messaging system, contact the development team.
