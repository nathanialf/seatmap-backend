# Bookmark Alerts API

## Overview

The Bookmark Alerts feature allows users to set up automated email notifications for their bookmarks when seat availability conditions are met. Alerts work differently for flight bookmarks vs saved searches and are processed automatically every 3 hours.

**Key Features:**
- **Threshold-based notifications**: Set specific seat count or percentage thresholds
- **Automated monitoring**: Scheduled processing every 3 hours via CloudWatch Events
- **Email notifications**: Rich HTML and text email alerts with flight details
- **Dual alert types**: Different logic for flight bookmarks vs saved searches

**Requires user authentication (not guest tokens).**

---

## Alert Configuration Types

### Flight Bookmark Alerts
- **Trigger Condition**: When seat count falls **below** the threshold (absolute count)
- **Use Case**: Monitor a specific flight's seat availability decline
- **Threshold Unit**: Number of seats (e.g., 10 = alert when fewer than 10 seats available)
- **Example**: Alert when AA123 from LAX→JFK drops below 5 available seats

### Saved Search Alerts  
- **Trigger Condition**: When any matching flight has availability **above** the threshold (percentage)
- **Use Case**: Monitor route for flights with good availability
- **Threshold Unit**: Percentage of estimated capacity (e.g., 25.0 = alert when >25% seats available)
- **Example**: Alert when any LAX→JFK flight has >20% availability

---

## Configure Alert for Bookmark

Add or update alert configuration for an existing bookmark.

**Endpoint**: `PATCH /bookmarks/{bookmarkId}/alert`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
Content-Type: application/json
```

**Path Parameters**:
- `bookmarkId` (required): The bookmark ID to configure alerts for

**Request Body**:
```json
{
  "alertThreshold": 10.0
}
```

**Parameters**:
- `alertThreshold` (required): 
  - For **BOOKMARK** items: Absolute seat count (e.g., 5 = alert when <5 seats)
  - For **SAVED_SEARCH** items: Percentage of capacity (e.g., 20.0 = alert when >20%)
  - Must be a positive number

**Response**:
```json
{
  "success": true,
  "data": {
    "bookmarkId": "bm_abc123xyz",
    "alertThreshold": 10.0,
    "alertEnabled": true,
    "message": "Alert configuration saved successfully"
  }
}
```

**Example cURL**:
```bash
curl -X PATCH {BASE_URL}/bookmarks/bm_abc123xyz/alert \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}" \
  -d '{
    "alertThreshold": 10.0
  }'
```

---

## Remove Alert Configuration

Remove alert configuration from a bookmark to stop notifications.

**Endpoint**: `DELETE /bookmarks/{bookmarkId}/alert`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `bookmarkId` (required): The bookmark ID to remove alerts from

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Alert configuration removed successfully"
  }
}
```

**Example cURL**:
```bash
curl -X DELETE {BASE_URL}/bookmarks/bm_abc123xyz/alert \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Create Bookmark with Alert

You can also configure alerts when creating a bookmark by including `alertConfig` in the creation request.

**Endpoint**: `POST /bookmarks`

**Request Body Example**:
```json
{
  "itemType": "BOOKMARK",
  "title": "LAX to JFK - Dec 15",
  "flightOfferData": "{...flight offer JSON...}",
  "alertConfig": {
    "alertThreshold": 5.0
  }
}
```

**Request Body Example (Saved Search)**:
```json
{
  "itemType": "SAVED_SEARCH", 
  "title": "Weekly LAX to JFK search",
  "searchRequest": {
    "origin": "LAX",
    "destination": "JFK", 
    "departureDate": "2025-12-15",
    "travelClass": "ECONOMY"
  },
  "alertConfig": {
    "alertThreshold": 25.0
  }
}
```

---

## Alert Processing System

### Scheduled Processing
- **Frequency**: Every 3 hours via CloudWatch Events
- **Lambda Function**: `AlertProcessorHandler`
- **Timeout**: 10 minutes for batch processing
- **Memory**: 512MB

### Processing Logic

1. **Discovery Phase**:
   - Scan all bookmarks with enabled alerts (`alertThreshold` not null)
   - Filter out expired bookmarks
   - Group by similar flight search criteria for efficiency

2. **Evaluation Phase**:
   - Execute flight searches for each bookmark/group
   - Use `AlertEvaluationService` to check threshold conditions
   - Track last evaluation time to prevent duplicate alerts

3. **Notification Phase**:
   - Send email notifications for triggered alerts
   - Update bookmark alert metadata (last evaluated, last triggered)
   - Record trigger history for debugging

### Alert Evaluation Rules

**For Flight Bookmarks (BOOKMARK type)**:
- Extract flight identifier from stored `flightOfferData`
- Match against current flight search results using:
  - Carrier code + flight number
  - Departure date, origin, destination
- Trigger when `currentSeats < alertThreshold`

**For Saved Searches (SAVED_SEARCH type)**:
- Execute search using stored search parameters
- Filter by airline code if specified
- Calculate percentage: `(availableSeats / estimatedCapacity) * 100`
- Trigger when `percentage > alertThreshold` for any matching flight

---

## Email Notifications

### Email Content
- **Subject Line**: Includes route, date, and flight details
- **HTML Template**: Rich formatting with flight details, threshold comparison, action buttons
- **Text Template**: Plain text fallback with same information
- **Action Links**: Direct links to view bookmark and manage alert settings

### Email Template Variables
- User's first name
- Bookmark title and flight details
- Current vs threshold values with appropriate units
- Alert message describing what triggered the alert
- Deep links to frontend bookmark and alert management pages

### From Address
- Emails sent from: `no-reply@myseatmap.com`
- Uses AWS SES for reliable delivery

---

## Response Data Format

When alerts are configured, bookmark responses include additional fields:

```json
{
  "bookmarkId": "bm_abc123xyz",
  "userId": "user_456def",
  "title": "LAX to JFK - Dec 15", 
  "itemType": "BOOKMARK",
  "hasAlert": true,
  "alertConfig": {
    "alertThreshold": 10.0,
    "lastEvaluated": "2025-11-28T10:00:00Z",
    "lastTriggered": "2025-11-27T14:30:00Z"
  }
}
```

**Alert Config Fields**:
- `alertThreshold`: The configured threshold value
- `lastEvaluated`: Last time alert was checked (ISO 8601)
- `lastTriggered`: Last time alert was triggered (ISO 8601, null if never)
- `triggerHistory`: JSON string of trigger events (for debugging)

---

## Error Responses

**400 Bad Request** (Missing threshold):
```json
{
  "success": false,
  "message": "Alert threshold is required"
}
```

**400 Bad Request** (Invalid threshold):
```json
{
  "success": false,
  "message": "Alert threshold must be positive"
}
```

**404 Not Found** (Bookmark not found):
```json
{
  "success": false,
  "message": "Bookmark not found"
}
```

**404 Not Found** (No alert configured):
```json
{
  "success": false,
  "message": "No alert configured for this bookmark"
}
```

**401 Unauthorized**:
```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

**403 Forbidden** (Guest tokens):
```json
{
  "success": false,
  "message": "Alert configuration requires user authentication"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Failed to configure alert"
}
```

---

## Usage Guidelines

### Best Practices
- **Flight Bookmarks**: Use low thresholds (1-10 seats) for immediate notification
- **Saved Searches**: Use percentage thresholds (15-30%) to find flights with good availability
- **Monitor expiration**: Alerts automatically stop when bookmarks expire
- **Email management**: Users can disable alerts without deleting bookmarks

### Threshold Recommendations
**For Individual Flights (BOOKMARK)**:
- `1-3 seats`: Very urgent, last few seats
- `5-10 seats`: Moderate urgency, limited availability
- `15+ seats`: Early warning for popular flights

**For Route Monitoring (SAVED_SEARCH)**:
- `10-20%`: Conservative, looking for any decent availability
- `25-40%`: Moderate, waiting for good options
- `50%+`: Very selective, only high availability flights

### Limitations
- **Processing frequency**: 3-hour intervals (not real-time)
- **Capacity estimation**: Uses simplified 150-seat estimate for percentage calculations
- **Email delivery**: Depends on AWS SES limits and user email provider
- **Flight matching**: Requires exact match on carrier, flight number, date, route

---

## Integration with Existing Bookmarks

### Migration
- Existing bookmarks without alerts continue to work normally
- Alerts can be added to any existing bookmark at any time
- No disruption to bookmark functionality when adding/removing alerts

### Compatibility
- Works with both BOOKMARK and SAVED_SEARCH item types
- Integrates with existing tier limits (alerts don't count separately)
- Compatible with bookmark expiration and cleanup logic

### Frontend Integration
Alert management URLs referenced in emails:
- **Development**: `https://dev.myseatmap.com/bookmarks/{bookmarkId}`
- **Production**: `https://myseatmap.com/bookmarks/{bookmarkId}`