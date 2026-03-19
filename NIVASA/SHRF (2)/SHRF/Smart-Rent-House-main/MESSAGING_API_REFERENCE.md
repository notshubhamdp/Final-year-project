# Smart Rent House - Messaging System API Reference

## Quick Start

### Access Messaging
1. Tenants can click "💬 Messages" in the tenant dashboard navigation
2. Or click "💬 Message Landlord" on any property detail page
3. Landlords can view messages at `/messages/conversations`

## API Endpoints

### Web Routes (Form-based)

#### Get Conversations List
```
GET /messages/conversations
```
**Authentication**: Required (any authenticated user)
**Returns**: HTML page with all conversations
**Features**:
- Shows unread count badge
- Last message preview
- Conversation partners
- Link to open conversation

#### Open Conversation
```
GET /messages/conversation/{userId}
```
**Parameters**:
- `userId`: ID of the user to message (Path parameter)

**Authentication**: Required
**Returns**: HTML page with message thread
**Features**:
- Full message history
- Auto-scroll to latest message
- Compose form
- Read status indicators

#### Open Property Conversation
```
GET /messages/conversation/{userId}/{propertyId}
```
**Parameters**:
- `userId`: ID of the user to message (Path parameter)
- `propertyId`: ID of the property context (Path parameter)

**Authentication**: Required
**Returns**: HTML page with property-specific message thread
**Features**:
- Messages filtered to property context
- Property details in header
- Reply with property reference

#### Send Message (Form)
```
POST /messages/send
```
**Form Parameters**:
- `recipientId`: ID of recipient (Long, required)
- `messageText`: Message content (String, required, max 5000 chars)
- `propertyId`: ID of property context (Long, optional)

**Authentication**: Required
**Redirects To**: Conversation detail page
**Validation**:
- Cannot message yourself
- Message text cannot be empty
- Recipient must exist

---

### REST API Endpoints (JSON)

#### Send Message (API)
```
POST /messages/api/send
Content-Type: application/json

{
  "recipientId": 2,
  "messageText": "Is this property still available?",
  "propertyId": 5
}
```
**Returns**:
```json
{
  "success": true,
  "message": "Message sent successfully",
  "messageId": 123
}
```
**Error Response**:
```json
{
  "success": false,
  "message": "Error description"
}
```

#### Get Unread Count
```
GET /messages/api/unread-count
```
**Returns**:
```json
{
  "success": true,
  "unreadCount": 5
}
```
**Use Case**: Update badge count in UI without full page reload

#### Mark Message as Read
```
POST /messages/api/{messageId}/read
```
**Parameters**:
- `messageId`: ID of message to mark as read (Path parameter)

**Returns**:
```json
{
  "success": true,
  "message": "Message marked as read"
}
```

#### Delete Message
```
DELETE /messages/api/{messageId}
```
**Parameters**:
- `messageId`: ID of message to delete (Path parameter)

**Returns**:
```json
{
  "success": true,
  "message": "Message deleted successfully"
}
```
**Note**: Only sender or recipient can delete a message

---

## Data Models

### Message Object
```json
{
  "id": 1,
  "sender": {
    "id": 10,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com"
  },
  "recipient": {
    "id": 5,
    "firstName": "Jane",
    "lastName": "Smith",
    "email": "jane@example.com"
  },
  "property": {
    "id": 25,
    "name": "Cozy Apartment"
  },
  "messageText": "Is this property available?",
  "isRead": false,
  "createdAt": "2024-01-15T10:30:00",
  "readAt": null
}
```

### Conversation Preview
```json
{
  "partner": {
    "id": 5,
    "firstName": "Jane",
    "lastName": "Smith"
  },
  "partnerName": "Jane Smith",
  "unreadCount": 2,
  "lastMessage": "Yes, it's available!",
  "lastMessageTime": "2024-01-15T10:35:00",
  "lastMessageFrom": 5
}
```

---

## Error Codes

| Status | Error | Meaning |
|--------|-------|---------|
| 400 | Bad Request | Invalid parameters or validation failed |
| 401 | Unauthorized | Authentication required |
| 403 | Forbidden | Permission denied (not sender/recipient) |
| 404 | Not Found | User, message, or property not found |
| 500 | Server Error | Internal server error |

---

## Rate Limiting (Future)

Currently no rate limiting is implemented. Recommended limits:
- 100 messages per hour per user
- 10 messages per minute per user
- 1000 character minimum wait between rapid messages

---

## Code Examples

### JavaScript - Send Message via API
```javascript
async function sendMessage(recipientId, messageText, propertyId = null) {
  const payload = {
    recipientId: recipientId,
    messageText: messageText
  };
  
  if (propertyId) {
    payload.propertyId = propertyId;
  }
  
  try {
    const response = await fetch('/messages/api/send', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(payload)
    });
    
    const data = await response.json();
    
    if (data.success) {
      console.log('Message sent:', data.messageId);
      // Refresh conversation
      location.reload();
    } else {
      alert('Error: ' + data.message);
    }
  } catch (error) {
    console.error('Failed to send message:', error);
  }
}

// Usage
sendMessage(5, "Is this property still available?", 25);
```

### JavaScript - Update Unread Badge
```javascript
async function updateUnreadBadge() {
  try {
    const response = await fetch('/messages/api/unread-count');
    const data = await response.json();
    
    if (data.success) {
      const badge = document.querySelector('.unread-badge');
      if (badge) {
        if (data.unreadCount > 0) {
          badge.textContent = data.unreadCount;
          badge.style.display = 'block';
        } else {
          badge.style.display = 'none';
        }
      }
    }
  } catch (error) {
    console.error('Failed to update unread count:', error);
  }
}

// Update every 30 seconds
setInterval(updateUnreadBadge, 30000);
```

### JavaScript - Auto-refresh Conversations
```javascript
// Auto-refresh conversations list every minute
setInterval(() => {
  fetch('/messages/conversations')
    .then(response => response.text())
    .then(html => {
      // Parse and update conversations list
      const parser = new DOMParser();
      const doc = parser.parseFromString(html, 'text/html');
      const newList = doc.querySelector('.conversations-list');
      if (newList) {
        document.querySelector('.conversations-list').innerHTML = newList.innerHTML;
      }
    });
}, 60000);
```

---

## Thymeleaf Template Usage

### Link to Open Conversation
```html
<a th:href="@{/messages/conversation/{id}(id=${user.id})}">
  Message <span th:text="${user.firstName}"></span>
</a>
```

### Link to Message About Property
```html
<a th:href="@{/messages/conversation/{userId}/{propId}(userId=${landlord.id}, propId=${property.id})}">
  Ask About <span th:text="${property.name}"></span>
</a>
```

### Display Message (in conversation template)
```html
<div th:each="msg : ${messages}" 
     th:classappend="${msg.sender.id == currentUser.id ? 'sent' : 'received'}">
  <div class="message-bubble" th:text="${msg.messageText}"></div>
  <div class="message-time" th:text="${#temporals.format(msg.createdAt, 'dd MMM, HH:mm')}"></div>
</div>
```

---

## Frontend Integration

### Adding Messaging Button to Any Page
```html
<a class="btn btn-primary" 
   th:href="@{/messages/conversation/{id}(id=${landlord.id})}">
  💬 Message
</a>
```

### Adding Unread Badge to Navigation
```html
<a th:href="@{/messages/conversations}" class="nav-link">
  Messages
  <span class="badge" th:if="${unreadCount > 0}" 
        th:text="${unreadCount}"></span>
</a>
```

---

## Performance Considerations

### Database Indexes
The messages table has the following indexes for optimal query performance:
- `idx_sender_recipient (sender_id, recipient_id)` - For conversation queries
- `idx_property (property_id)` - For property-specific messages
- `idx_created_at (created_at)` - For sorting by timestamp

### Optimization Tips
1. Limit conversation thread to last 100 messages for initial load
2. Implement pagination for older messages
3. Cache conversation previews
4. Debounce unread count updates (30 seconds)
5. Use lazy loading for large conversation lists

### Pagination Example (Future)
```java
@GetMapping("/conversation/{userId}")
public String getConversation(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        Authentication authentication,
        Model model) {
    // Implement pagination
}
```

---

## Security Best Practices

✅ **Implemented**:
- User authentication required for all endpoints
- Ownership verification for all message operations
- Input validation and sanitization
- No database injection possible (JPA parameterized queries)
- CSRF protection via Thymeleaf forms

⚠️ **To Implement**:
- Message content encryption at rest
- Rate limiting to prevent spam
- Message content XSS prevention in templates
- GDPR compliance for message deletion
- Audit logging for sensitive operations

---

## Version History

### v1.0 (Current)
- Basic messaging between users
- Property context support
- Read status tracking
- Conversation history
- API endpoints for integration

### Planned Releases
- v1.1: Message encryption
- v1.2: Real-time notifications (WebSocket)
- v1.3: File attachments
- v2.0: Advanced search and filtering
