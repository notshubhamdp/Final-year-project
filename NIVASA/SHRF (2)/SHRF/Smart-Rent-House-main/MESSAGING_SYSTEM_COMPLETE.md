# Tenant-Landlord Messaging System

## Overview

The messaging system enables real-time communication between tenants and landlords for a specific property. It includes message history, read status tracking, and unread message notifications.

## Features

- ✅ **Send Messages**: Tenants and landlords can send messages to each other
- ✅ **Message History**: View entire conversation history for a property
- ✅ **Read Status**: See which messages have been read
- ✅ **Unread Count**: Track unread messages with notification badge
- ✅ **Auto-refresh**: Unread count refreshes automatically every 30 seconds
- ✅ **AJAX Support**: Send messages without page refresh
- ✅ **Beautiful UI**: Modern, responsive chat interface

## Database Schema

Messages are stored in the `messages` table with the following structure:

```sql
CREATE TABLE messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    timestamp DATETIME NOT NULL,
    is_read BOOLEAN DEFAULT FALSE
);
```

## API Endpoints

### Web UI Routes

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/messages/conversations` | Display all conversations for current user |
| GET | `/messages/conversation/{receiverId}/{propertyId}` | View specific conversation |
| POST | `/messages/send` | Send a message (Form submission) |

### REST API Endpoints

| Method | Endpoint | Description | Request Params |
|--------|----------|-------------|-----------------|
| POST | `/messages/api/send` | Send message via AJAX | `receiverId`, `propertyId`, `content` |
| GET | `/messages/api/unread-count` | Get unread message count | - |
| POST | `/messages/api/mark-read/{messageId}` | Mark message as read | - |
| POST | `/messages/delete/{messageId}` | Delete a message | - |

## Service Methods

### MessageService

**sendMessage(Long senderId, Long receiverId, Long propertyId, String content)**
- Sends a message between two users for a specific property
- Validates content is not empty
- Automatically sets timestamp and read status to false
- Returns the saved message

**getConversation(Long user1, Long user2, Long propertyId)**
- Retrieves all messages between two users for a specific property
- Automatically marks messages as read for user1
- Returns messages sorted by timestamp (ascending)

**getUserConversations(Long userId)**
- Gets all conversations involving the user (sent or received)
- Returns messages sorted by timestamp (descending - newest first)

**getUnreadMessageCount(Long userId)**
- Returns count of unread messages for the user

**markMessageAsRead(Long messageId)**
- Marks a specific message as read

**deleteMessage(Long messageId)**
- Deletes a message by ID

## Usage Examples

### 1. View All Conversations (List)
```html
<a href="/messages/conversations">View Messages</a>
```
Display: `/messages/conversations` - Shows all conversations with preview text and unread badges

### 2. Open Specific Conversation
```html
<a href="/messages/conversation/2/5">Chat with Landlord</a>
```
Parameters:
- `2` = Receiver User ID
- `5` = Property ID

### 3. Send Message (Standard Form)
```html
<form method="post" action="/messages/send">
    <input type="hidden" name="receiverId" value="2">
    <input type="hidden" name="propertyId" value="5">
    <textarea name="content" required></textarea>
    <button type="submit">Send</button>
</form>
```

### 4. Send Message via AJAX
```javascript
fetch('/messages/api/send', {
    method: 'POST',
    body: new URLSearchParams({
        receiverId: 2,
        propertyId: 5,
        content: 'Hello, is the property available?'
    })
})
.then(response => response.json())
.then(data => {
    if (data.success) {
        console.log('Message sent!', data.message);
    }
});
```

### 5. Get Unread Count
```javascript
fetch('/messages/api/unread-count')
    .then(response => response.json())
    .then(data => {
        console.log('Unread messages:', data.unreadCount);
    });
```

## View Files

### conversations.html
- Lists all conversations for the logged-in user
- Shows message preview, timestamp, and read status
- Highlights unread conversations
- Displays unread count badge
- Empty state message if no conversations

### conversation.html
- Full chat UI with message history
- Auto-scrolling to latest messages
- Textarea that auto-resizes
- Read status indicators (✓ Read)
- AJAX message sending
- Auto-refresh for unread count

### error.html
- Error page for failed operations
- Displays error message and details
- Back button to home

## Integration with Users

To access the messaging system, users (tenants/landlords) must be:
1. **Authenticated** - Logged into the application
2. **Have an Email** - Used to identify current user
3. **Have a User ID** - Used to track sent/received messages

Example in controller:
```java
User user = userRepository.findByemail(authentication.getName()).orElseThrow();
```

## Security Considerations

1. **Authentication Required**: All routes require Spring Security authentication
2. **User Verification**: The system verifies the authenticated user before showing conversations
3. **Property Association**: Messages are tied to a specific property ID
4. **Input Validation**: Content is trimmed and checked for empty values

## Styling Features

- **Modern gradient design** with purple theme
- **Responsive layout** works on desktop and mobile
- **Smooth animations** for message arrival
- **Read/Unread visual indicators**
- **Toast-like notifications** for errors
- **Auto-resizing textarea** for better UX

## Future Enhancements

Potential features to add:
- Message search functionality
- Message deletion confirmation
- Typing indicators
- User online/offline status
- Image and file attachments
- Message encryption
- Conversation archiving
- Bulk message operations
- Push notifications for new messages

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "User not found" error | Ensure you're logged in and your email is in the database |
| Messages not appearing | Check if messages are saved in database and timestamps are correct |
| AJAX not working | Verify CSRF token is included if CSRF protection is enabled |
| Unread count not updating | Browser cache - try hard refresh or clear cache |

## Environment Configuration

Ensure your `application.yml` has proper Spring Security configuration:

```yaml
spring:
  security:
    filter:
      order: 5
```

Database must support `TEXT` column type for message content.
