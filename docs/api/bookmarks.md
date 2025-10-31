# Bookmarks API

## Overview

Save and manage favorite flights for quick access to seat maps and flight details. **Requires user authentication (not guest tokens).**

---

## Create Bookmark

Save a flight offer as a bookmark for future reference.

**Endpoint**: `POST /bookmarks`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "LAX to JFK - Dec 15",
  "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
}
```

**Parameters**:
- `title` (required): Descriptive name for the bookmark
- `flightOfferData` (required): Complete flight offer object as JSON string from flight search

**Response**:
```json
{
  "success": true,
  "data": {
    "bookmarkId": "bm_abc123xyz",
    "userId": "user_456def",
    "title": "LAX to JFK - Dec 15",
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}",
    "createdAt": "2025-10-30T12:00:00Z",
    "expiresAt": "2025-12-16T08:00:00Z"
  }
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}" \
  -d '{
    "title": "LAX to JFK - Dec 15",
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
  }'
```

---

## Get User Bookmarks

Retrieve all bookmarks for the authenticated user.

**Endpoint**: `GET /bookmarks`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Response**:
```json
{
  "success": true,
  "data": [
    {
      "bookmarkId": "bm_abc123xyz",
      "userId": "user_456def",
      "title": "LAX to JFK - Dec 15",
      "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}",
      "createdAt": "2025-10-30T12:00:00Z",
      "expiresAt": "2025-12-16T08:00:00Z"
    },
    {
      "bookmarkId": "bm_def456uvw",
      "userId": "user_456def",
      "title": "SFO to ORD - Dec 20",
      "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"SABRE\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-15\",\"numberOfBookableSeats\":5,\"itineraries\":[{\"duration\":\"PT4H20M\",\"segments\":[{\"departure\":{\"iataCode\":\"SFO\",\"terminal\":\"1\",\"at\":\"2025-12-20T14:30:00\"},\"arrival\":{\"iataCode\":\"ORD\",\"terminal\":\"2\",\"at\":\"2025-12-20T20:50:00\"},\"carrierCode\":\"UA\",\"number\":\"456\",\"aircraft\":{\"code\":\"737\"},\"operating\":{\"carrierCode\":\"UA\"},\"duration\":\"PT4H20M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"189.00\",\"base\":\"159.00\"}}",
      "createdAt": "2025-10-30T10:30:00Z",
      "expiresAt": "2025-12-21T14:30:00Z"
    }
  ]
}
```

**Example cURL**:
```bash
curl -X GET {BASE_URL}/bookmarks \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Delete Bookmark

Remove a specific bookmark from the user's saved flights.

**Endpoint**: `DELETE /bookmarks/{bookmarkId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `bookmarkId` (required): Unique bookmark identifier

**Response**:
```json
{
  "success": true,
  "message": "Bookmark deleted successfully"
}
```

**Example cURL**:
```bash
curl -X DELETE {BASE_URL}/bookmarks/bm_abc123xyz \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Bookmark Data Structure

### Bookmark Fields
- `bookmarkId`: Unique identifier for the bookmark
- `userId`: ID of the user who created the bookmark
- `title`: User-defined descriptive name
- `flightOfferData`: Complete flight offer object as JSON string
- `createdAt`: Timestamp when bookmark was created
- `expiresAt`: Automatic expiration based on flight departure

### Automatic Expiration
Bookmarks automatically expire based on the flight's departure time:
- **Expiration**: 24 hours after scheduled departure
- **Cleanup**: Expired bookmarks are automatically removed
- **Purpose**: Prevents accumulation of outdated flight data

### Flight Offer Data
The `flightOfferData` field contains the complete flight offer object exactly as received from flight search, including:
- Flight details (carrier, flight number, times)
- Pricing information
- Aircraft and route data
- Data source information for seat map routing

---

## Integration with Seat Maps

Bookmarks provide a streamlined path to seat maps:

**Direct Seat Map Access**:
```bash
# Get seat map directly from bookmark
curl -X GET {BASE_URL}/seat-map/bookmark/{bookmarkId} \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

**Traditional Workflow**:
1. **Create Bookmark**: Save flight offer from search results
2. **List Bookmarks**: Retrieve user's saved flights
3. **Get Seat Map**: Use bookmark ID for direct seat map access

**Alternative Workflow**:
1. **Get Bookmark**: Retrieve specific bookmark
2. **Extract Flight Data**: Use `flightOfferData` from bookmark
3. **Manual Seat Map**: Post flight data to `/seat-map` endpoint

---

## Rate Limiting

Bookmark operations are subject to user tier limits:

### Bookmark Limits
- **Storage**: Varies by user tier
- **Creation**: Rate limited per timeframe
- **Access**: Unlimited retrieval for owned bookmarks

### Automatic Management
- **Expiration**: Bookmarks auto-expire after flight departure
- **Cleanup**: System automatically removes expired entries
- **Storage Optimization**: Prevents unlimited accumulation

---

## Error Responses

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Validation errors: Title is required; Flight offer data is required;"
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
  "message": "Bookmark access requires user authentication"
}
```

**404 Not Found**:
```json
{
  "success": false,
  "message": "Bookmark not found or access denied"
}
```

**409 Conflict** (Bookmark limits exceeded):
```json
{
  "success": false,
  "message": "Bookmark limit reached for your account tier"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Error processing bookmark request"
}
```