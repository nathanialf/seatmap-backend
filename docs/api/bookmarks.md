# Unified Bookmarks & Saved Searches API

## Overview

Save and manage both flight bookmarks and saved searches through a unified API. **Requires user authentication (not guest tokens).**

This unified API handles two types of saved items:
- **Bookmarks**: Save specific flight offers for quick access to seat maps and flight details
- **Saved Searches**: Save search parameters for repeated flight discovery

---

## Create Bookmark or Saved Search

Save a flight offer as a bookmark OR save search parameters as a saved search.

**Endpoint**: `POST /bookmarks`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
Content-Type: application/json
```

### Creating a Flight Bookmark

**Request Body**:
```json
{
  "itemType": "BOOKMARK",
  "title": "LAX to JFK - Dec 15",
  "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
}
```

**Parameters**:
- `itemType` (required): Must be `"BOOKMARK"`
- `title` (required): Descriptive name for the bookmark
- `flightOfferData` (required): Complete flight offer object as JSON string from flight search

### Creating a Saved Search

**Request Body**:
```json
{
  "itemType": "SAVED_SEARCH",
  "title": "Weekly LAX to JFK search",
  "searchRequest": {
    "origin": "LAX",
    "destination": "JFK",
    "departureDate": "2025-12-15",
    "travelClass": "ECONOMY",
    "airlineCode": "AA",
    "flightNumber": "123",
    "maxResults": 10
  }
}
```

**Parameters**:
- `itemType` (required): Must be `"SAVED_SEARCH"`
- `title` (required): Descriptive name for the saved search
- `searchRequest` (required): Flight search parameters object with separated airline/flight fields

**Response** (same for both types):
```json
{
  "success": true,
  "data": {
    "bookmarkId": "bm_abc123xyz",
    "userId": "user_456def",
    "itemType": "BOOKMARK",
    "title": "LAX to JFK - Dec 15",
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}",
    "createdAt": "2025-10-30T12:00:00Z",
    "expiresAt": "2025-12-16T08:00:00Z"
  }
}
```

**Example cURL for Bookmark**:
```bash
curl -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}" \
  -d '{
    "itemType": "BOOKMARK",
    "title": "LAX to JFK - Dec 15",
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
  }'
```

**Example cURL for Saved Search**:
```bash
curl -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}" \
  -d '{
    "itemType": "SAVED_SEARCH",
    "title": "Weekly LAX to JFK search",
    "searchRequest": {
      "origin": "LAX",
      "destination": "JFK",
      "departureDate": "2025-12-15",
      "travelClass": "ECONOMY",
      "airlineCode": "AA",
      "flightNumber": "123"
    }
  }'
```

---

## Get User Items (Bookmarks & Saved Searches)

Retrieve all saved items for the authenticated user, with optional filtering by type.

**Endpoint**: `GET /bookmarks[?type=BOOKMARK|SAVED_SEARCH]`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Query Parameters** (optional):
- `type`: Filter by item type (`BOOKMARK` or `SAVED_SEARCH`)

### Get All Items (no filter)

**Example**: `GET /bookmarks`

**Response**:
```json
{
  "success": true,
  "data": {
    "bookmarks": [
      {
        "bookmarkId": "bm_abc123xyz",
        "userId": "user_456def",
        "itemType": "BOOKMARK",
        "title": "LAX to JFK - Dec 15",
        "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}",
        "createdAt": "2025-10-30T12:00:00Z",
        "expiresAt": "2025-12-16T08:00:00Z"
      },
      {
        "bookmarkId": "bm_def456uvw",
        "userId": "user_456def",
        "itemType": "SAVED_SEARCH",
        "title": "Weekly LAX to JFK search",
        "origin": "LAX",
        "destination": "JFK",
        "departureDate": "2025-12-15",
        "travelClass": "ECONOMY",
        "airlineCode": "AA",
        "flightNumber": "123",
        "maxResults": 10,
        "createdAt": "2025-10-30T10:30:00Z",
        "expiresAt": "2025-11-29T10:30:00Z"
      }
    ],
    "total": 2,
    "tier": "PRO",
    "remaining": 8
  }
}
```

### Get Only Bookmarks

**Example**: `GET /bookmarks?type=BOOKMARK`

**Response**:
```json
{
  "success": true,
  "data": {
    "bookmarks": [
      {
        "bookmarkId": "bm_abc123xyz",
        "userId": "user_456def",
        "itemType": "BOOKMARK",
        "title": "LAX to JFK - Dec 15",
        "flightOfferData": "{...}",
        "createdAt": "2025-10-30T12:00:00Z",
        "expiresAt": "2025-12-16T08:00:00Z"
      }
    ],
    "total": 1,
    "tier": "PRO",
    "remaining": 9
  }
}
```

### Get Only Saved Searches

**Example**: `GET /bookmarks?type=SAVED_SEARCH`

**Response**:
```json
{
  "success": true,
  "data": {
    "savedSearches": [
      {
        "bookmarkId": "bm_def456uvw",
        "userId": "user_456def",
        "itemType": "SAVED_SEARCH",
        "title": "Weekly LAX to JFK search",
        "origin": "LAX",
        "destination": "JFK",
        "departureDate": "2025-12-15",
        "travelClass": "ECONOMY",
        "airlineCode": "AA",
        "flightNumber": "123",
        "maxResults": 10,
        "createdAt": "2025-10-30T10:30:00Z",
        "expiresAt": "2025-11-29T10:30:00Z"
      }
    ],
    "total": 1,
    "tier": "PRO",
    "remaining": 9
  }
}
```

**Example cURL**:
```bash
# Get all items
curl -X GET {BASE_URL}/bookmarks \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"

# Get only bookmarks
curl -X GET {BASE_URL}/bookmarks?type=BOOKMARK \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"

# Get only saved searches
curl -X GET {BASE_URL}/bookmarks?type=SAVED_SEARCH \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Get Specific Item

Retrieve a specific bookmark or saved search by ID.

**Endpoint**: `GET /bookmarks/{bookmarkId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `bookmarkId` (required): Unique identifier

**Response**:
```json
{
  "success": true,
  "data": {
    "bookmarkId": "bm_abc123xyz",
    "userId": "user_456def",
    "itemType": "BOOKMARK",
    "title": "LAX to JFK - Dec 15",
    "flightOfferData": "{...}",
    "createdAt": "2025-10-30T12:00:00Z",
    "expiresAt": "2025-12-16T08:00:00Z"
  }
}
```

---

## Delete Item

Remove a specific bookmark or saved search from the user's saved items.

**Endpoint**: `DELETE /bookmarks/{bookmarkId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `bookmarkId` (required): Unique identifier

**Response**:
```json
{
  "success": true,
  "data": {
    "message": "Bookmark deleted successfully"
  }
}
```

**Example cURL**:
```bash
curl -X DELETE {BASE_URL}/bookmarks/bm_abc123xyz \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Data Structure

### Unified Item Fields
- `bookmarkId`: Unique identifier for the item
- `userId`: ID of the user who created the item
- `itemType`: Type of item (`"BOOKMARK"` or `"SAVED_SEARCH"`)
- `title`: User-defined descriptive name
- `createdAt`: Timestamp when item was created
- `updatedAt`: Timestamp when item was last modified
- `expiresAt`: Automatic expiration timestamp
- `lastAccessedAt`: Timestamp when item was last accessed

### Type-Specific Fields

**For Bookmarks** (`itemType: "BOOKMARK"`):
- `flightOfferData`: Complete flight offer object as JSON string

**For Saved Searches** (`itemType: "SAVED_SEARCH"`):
- `origin`: 3-letter airport code for departure
- `destination`: 3-letter airport code for arrival
- `departureDate`: Date in YYYY-MM-DD format
- `travelClass`: Optional cabin class filter
- `airlineCode`: Optional 2-3 letter airline code
- `flightNumber`: Optional 1-4 digit flight number
- `maxResults`: Optional maximum results (1-50)

### Automatic Expiration

Both bookmarks and saved searches automatically expire:

**Bookmarks**:
- **Expiration**: 24 hours after scheduled flight departure
- **Purpose**: Prevents accumulation of outdated flight data

**Saved Searches**:
- **Expiration**: Based on departure date (end of departure day)
- **Fallback**: 30 days after creation if date parsing fails

**Cleanup**: Expired items are automatically removed by TTL

### Flight Search Request Structure

When creating saved searches, the `searchRequest` object supports these fields:

```json
{
  "origin": "LAX",                    // Required: 3-letter airport code
  "destination": "JFK",               // Required: 3-letter airport code  
  "departureDate": "2025-12-15",      // Required: YYYY-MM-DD format
  "travelClass": "ECONOMY",           // Optional: ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST
  "airlineCode": "AA",                // Optional: 2-3 letter airline code  
  "flightNumber": "123",              // Optional: 1-4 digit flight number (requires airlineCode)
  "maxResults": 10                    // Optional: Maximum results (1-50), defaults to 10
}
```

**Validation Rules**:
- `flightNumber` can only be provided when `airlineCode` is also specified
- `airlineCode` can be provided alone to search all flights for that airline
- `maxResults` must be between 1-50

**Important**: Only these fields are supported. The system does not support `returnDate`, `adults`, `children`, `infants`, `includeCarriers`, `excludeCarriers`, `directFlights`, or `currencyCode`.

---

## Integration with Flight Search

**Saved Search Workflow**:
1. **Get Saved Search**: Use `GET /bookmarks/{id}` to retrieve saved search parameters
2. **Run Flight Search**: Use the returned individual fields with `/flight-search` endpoint
3. **Get Seat Maps**: Flight search results include integrated seat map data

**Workflow Example**:
1. **Create Items**: Save flight offers as bookmarks OR search parameters as saved searches
2. **List Items**: Retrieve user's saved bookmarks and searches (optionally filtered)
3. **Use Saved Searches**: Extract individual search fields and use with flight search API

---

## Real-Time Usage Counting

The unified API uses real-time usage counting instead of cumulative counters:

### How Real-Time Counting Works
- **Live Count**: Usage is calculated by counting active (non-expired) items in real-time
- **Deletions Count**: When you delete an item, your usage count immediately decreases
- **Expiration Counts**: When items expire, they no longer count toward your limit
- **Accurate Limits**: You always have an accurate view of how many slots you're using

### Usage Limits by Tier
- **FREE Tier**: Cannot create bookmarks or saved searches
- **PRO Tier**: 10 active items total
- **BUSINESS Tier**: Unlimited items
- **DEV Tier**: Unlimited items (development only)

### Usage Response Fields
Every list response includes:
- `total`: Number of items currently returned
- `tier`: Your account tier
- `remaining`: How many more items you can create (real-time active count)

### Example Real-Time Counting
```
PRO user (10 total limit):
- Creates 7 bookmarks and 2 saved searches = 9 active items
- Deletes 3 bookmarks = 6 active items, 4 remaining slots
- 2 bookmarks expire after flights = 4 active items, 6 remaining slots
- Can create 6 more items before hitting the 10-item limit
```

This is a significant improvement from the old cumulative system where deletions didn't reduce your usage count.

---

## Error Responses

**400 Bad Request** (Missing itemType):
```json
{
  "success": false,
  "message": "Item type is required"
}
```

**400 Bad Request** (Invalid itemType):
```json
{
  "success": false,
  "message": "Invalid item type. Must be BOOKMARK or SAVED_SEARCH"
}
```

**400 Bad Request** (Validation errors):
```json
{
  "success": false,
  "message": "Validation errors: Title is required; Flight offer data is required for bookmarks;"
}
```

**400 Bad Request** (Invalid type filter):
```json
{
  "success": false,
  "message": "Invalid item type. Valid types: BOOKMARK, SAVED_SEARCH"
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
  "message": "Bookmark not found"
}
```

**403 Forbidden** (Tier limits exceeded):
```json
{
  "success": false,
  "message": "Monthly bookmark limit reached for your account tier. Upgrade for higher limits."
}
```

**403 Forbidden** (FREE tier creation):
```json
{
  "success": false,
  "message": "Bookmark creation is not available for FREE tier. Upgrade to access bookmark functionality."
}
```

**405 Method Not Allowed**:
```json
{
  "success": false,
  "message": "Method POST not allowed for this endpoint"
}
```

**410 Gone** (Expired item):
```json
{
  "success": false,
  "message": "Bookmark has expired"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```

---

## Migration from Separate APIs

**Breaking Changes**: This unified API replaces the previous separate `/bookmarks` and `/saved-searches` endpoints:

### What Changed
- **Required Field**: `itemType` is now required for all creation requests
- **Unified Endpoint**: Both bookmarks and saved searches use `/bookmarks` endpoint
- **Type Filtering**: Use `?type=` parameter instead of separate endpoints
- **Field Structure**: Saved searches now store individual fields instead of JSON objects
- **Separated Flight Fields**: `flightNumber` split into `airlineCode` and `flightNumber`
- **Execute Endpoint**: Removed - users must manually use saved search parameters
- **Real-Time Counting**: Usage limits now reflect active items, not cumulative creation

### Migration Guide
1. **Update Creation Calls**: Add `"itemType": "BOOKMARK"` or `"itemType": "SAVED_SEARCH"` to all creation requests
2. **Update Flight Search Calls**: Separate `flightNumber` into `airlineCode` and `flightNumber` fields
3. **Update List Calls**: Use `GET /bookmarks?type=BOOKMARK` instead of `GET /bookmarks` for bookmarks only
4. **Update Saved Search Lists**: Use `GET /bookmarks?type=SAVED_SEARCH` instead of `GET /saved-searches`
5. **Remove Execute Calls**: Replace execute functionality with manual parameter extraction and flight search
6. **Update Response Parsing**: Saved searches now return individual fields instead of nested `searchRequest` object
7. **Update Error Handling**: Review new error messages and status codes

### Backward Compatibility
**None**: This is a clean breaking change with no backward compatibility. All clients must be updated to use the new unified API.