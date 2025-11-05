# Saved Searches API

## Overview

Save and reuse flight search criteria for quick access to fresh flight results. **Requires user authentication (not guest tokens).** Saved searches share the same tier-based limits as bookmarks.

---

## Create Saved Search

Save flight search criteria for future reuse.

**Endpoint**: `POST /saved-searches`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
Content-Type: application/json
```

**Request Body**:
```json
{
  "title": "SFO to LAX Morning Flights",
  "searchRequest": {
    "origin": "SFO",
    "destination": "LAX",
    "departureDate": "2024-12-15",
    "travelClass": "ECONOMY",
    "flightNumber": null,
    "maxResults": 10
  }
}
```

**Parameters**:
- `title` (required): Descriptive name for the saved search (max 100 characters)
- `searchRequest` (required): Flight search criteria object
  - `origin` (required): 3-letter origin airport code
  - `destination` (required): 3-letter destination airport code  
  - `departureDate` (required): Date in YYYY-MM-DD format
  - `travelClass` (optional): ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST
  - `flightNumber` (optional): Specific flight number filter
  - `maxResults` (optional): Maximum results (default: 10)

**Response**:
```json
{
  "bookmarkId": "ss_abc123xyz",
  "userId": "user_456def",
  "title": "SFO to LAX Morning Flights",
  "itemType": "SAVED_SEARCH",
  "searchRequest": {
    "origin": "SFO",
    "destination": "LAX",
    "departureDate": "2024-12-15",
    "travelClass": "ECONOMY",
    "flightNumber": null,
    "maxResults": 10
  },
  "createdAt": "2024-10-30T12:00:00Z",
  "updatedAt": "2024-10-30T12:00:00Z",
  "lastAccessedAt": "2024-10-30T12:00:00Z",
  "isBookmark": false,
  "isSavedSearch": true
}
```

**Example cURL**:
```bash
# Store endpoint URL in temp file
echo "https://your-api-gateway-url/saved-searches" > /tmp/api_endpoint

# Store credentials in temp file  
echo "your-jwt-token-here" > /tmp/jwt_token

# Store request body in temp file
echo '{
  "title": "SFO to LAX Morning Flights",
  "searchRequest": {
    "origin": "SFO", 
    "destination": "LAX",
    "departureDate": "2024-12-15",
    "travelClass": "ECONOMY"
  }
}' > /tmp/request_body.json

# Execute curl command
curl -X POST $(cat /tmp/api_endpoint) \
     -H "Authorization: Bearer $(cat /tmp/jwt_token)" \
     -H "X-API-Key: your-api-key" \
     -H "Content-Type: application/json" \
     -d @/tmp/request_body.json

# Cleanup
rm -f /tmp/api_endpoint /tmp/jwt_token /tmp/request_body.json
```

---

## List Saved Searches

Retrieve all saved searches for the authenticated user.

**Endpoint**: `GET /saved-searches`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Response**:
```json
{
  "savedSearches": [
    {
      "bookmarkId": "ss_abc123xyz",
      "userId": "user_456def", 
      "title": "SFO to LAX Morning Flights",
      "itemType": "SAVED_SEARCH",
      "searchRequest": {
        "origin": "SFO",
        "destination": "LAX",
        "departureDate": "2024-12-15",
        "travelClass": "ECONOMY"
      },
      "createdAt": "2024-10-30T12:00:00Z",
      "updatedAt": "2024-10-30T12:00:00Z",
      "lastAccessedAt": "2024-10-30T15:30:00Z",
      "isBookmark": false,
      "isSavedSearch": true
    }
  ],
  "total": 1,
  "tier": "PRO",
  "remainingThisMonth": 8
}
```

**Example cURL**:
```bash
curl -X GET "https://your-api-gateway-url/saved-searches" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "X-API-Key: your-api-key"
```

---

## Get Saved Search

Retrieve a specific saved search by ID.

**Endpoint**: `GET /saved-searches/{searchId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `searchId` (required): The saved search ID

**Response**:
```json
{
  "bookmarkId": "ss_abc123xyz",
  "userId": "user_456def",
  "title": "SFO to LAX Morning Flights", 
  "itemType": "SAVED_SEARCH",
  "searchRequest": {
    "origin": "SFO",
    "destination": "LAX",
    "departureDate": "2024-12-15",
    "travelClass": "ECONOMY"
  },
  "createdAt": "2024-10-30T12:00:00Z",
  "updatedAt": "2024-10-30T12:00:00Z",
  "lastAccessedAt": "2024-10-30T16:00:00Z",
  "isBookmark": false,
  "isSavedSearch": true
}
```

**Example cURL**:
```bash
curl -X GET "https://your-api-gateway-url/saved-searches/ss_abc123xyz" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "X-API-Key: your-api-key"
```

---

## Execute Saved Search

Execute a saved search to get fresh flight results using the stored search criteria.

**Endpoint**: `POST /saved-searches/{searchId}/execute`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `searchId` (required): The saved search ID to execute

**Response**:
```json
{
  "searchRequest": {
    "origin": "SFO",
    "destination": "LAX", 
    "departureDate": "2024-12-15",
    "travelClass": "ECONOMY",
    "flightNumber": null,
    "maxResults": 10
  },
  "title": "SFO to LAX Morning Flights",
  "searchId": "ss_abc123xyz",
  "message": "Search request ready for execution"
}
```

**Usage**:
1. Call this endpoint to get the search criteria
2. Use the returned `searchRequest` object to call the `POST /flight-search` endpoint
3. The saved search's `lastAccessedAt` timestamp is automatically updated

**Example cURL**:
```bash
# Execute saved search to get criteria
curl -X POST "https://your-api-gateway-url/saved-searches/ss_abc123xyz/execute" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "X-API-Key: your-api-key"

# Then use the returned searchRequest with the flight search API
curl -X POST "https://your-api-gateway-url/flight-search" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "X-API-Key: your-api-key" \
     -H "Content-Type: application/json" \
     -d '{
       "origin": "SFO",
       "destination": "LAX",
       "departureDate": "2024-12-15",
       "travelClass": "ECONOMY"
     }'
```

---

## Delete Saved Search

Remove a saved search permanently.

**Endpoint**: `DELETE /saved-searches/{searchId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `searchId` (required): The saved search ID to delete

**Response**:
```json
{
  "message": "Saved search deleted successfully"
}
```

**Example cURL**:
```bash
curl -X DELETE "https://your-api-gateway-url/saved-searches/ss_abc123xyz" \
     -H "Authorization: Bearer your-jwt-token" \
     -H "X-API-Key: your-api-key"
```

---

## Tier-Based Limits

Saved searches count toward the same monthly limits as bookmarks:

### Usage Quotas
- **FREE Tier**: Saved search creation not available  
- **PRO Tier**: 50 saved searches + bookmarks per month
- **BUSINESS Tier**: Unlimited saved searches + bookmarks
- **DEV Tier**: Unlimited for development/testing

### Automatic Management
- **Expiration**: Saved searches auto-expire at the end of the departure date specified in the search criteria (23:59:59 UTC)
- **Cleanup**: System automatically removes expired entries based on flight departure dates
- **Monthly Reset**: Usage limits reset on the 1st of each month
- **Shared Limits**: Creating either a bookmark or saved search counts toward the same quota

---

## Error Responses

### 400 Bad Request
**Validation Errors**:
```json
{
  "success": false,
  "message": "Validation errors: Title is required; Origin must be a 3-letter airport code; "
}
```

**Invalid JSON**:
```json
{
  "success": false,
  "message": "Invalid request format"
}
```

### 401 Unauthorized
**Missing/Invalid Token**:
```json
{
  "success": false,
  "message": "Invalid or missing authentication token"
}
```

### 403 Forbidden
**Monthly Limit Reached**:
```json
{
  "success": false,
  "message": "Monthly bookmark limit reached for your account tier. Upgrade for higher limits."
}
```

**FREE Tier Restriction**:
```json
{
  "success": false,
  "message": "Saved search creation is not available for FREE tier. Upgrade to PRO (50 searches/month) or BUSINESS (unlimited) for saved search access."
}
```

### 404 Not Found
**Saved Search Not Found**:
```json
{
  "success": false,
  "message": "Saved search not found"
}
```

---

## Frontend Integration

### Unified Dashboard
Both bookmarks and saved searches can be displayed in a single dashboard:

```javascript
// Get all saved items
const [bookmarks, savedSearches] = await Promise.all([
  fetch('/bookmarks', { headers: authHeaders }),
  fetch('/saved-searches', { headers: authHeaders })
]);

// Combine and sort by creation date
const allItems = [
  ...bookmarks.data.bookmarks.map(item => ({ ...item, type: 'bookmark' })),
  ...savedSearches.data.savedSearches.map(item => ({ ...item, type: 'saved_search' }))
].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
```

### Execute Workflow
```javascript
// Execute a saved search
async function executeSavedSearch(searchId) {
  // Get search criteria
  const response = await fetch(`/saved-searches/${searchId}/execute`, {
    method: 'POST',
    headers: authHeaders
  });
  
  const { searchRequest } = await response.json();
  
  // Execute fresh search
  const searchResults = await fetch('/flight-search', {
    method: 'POST',
    headers: { ...authHeaders, 'Content-Type': 'application/json' },
    body: JSON.stringify(searchRequest)
  });
  
  return searchResults.json();
}
```

### Shared Limit Display
```javascript
// Both APIs return the same limit information
const { remainingThisMonth, tier } = bookmarksResponse.data;
// or
const { remainingThisMonth, tier } = savedSearchesResponse.data;

console.log(`${remainingThisMonth} items remaining this month for ${tier} tier`);
```

---

## Best Practices

### 1. Search Criteria Validation
- Always validate airport codes (3-letter IATA codes)
- Ensure departure dates are in the future
- Use standard travel class values

### 2. Error Handling
- Check tier limits before allowing creation
- Handle validation errors gracefully
- Provide clear upgrade paths for FREE tier users

### 3. User Experience
- Update `lastAccessedAt` to track usage patterns
- Show creation dates for organization
- Provide search execution feedback

### 4. Performance
- Cache search results when possible
- Use the execute endpoint to get fresh data
- Consider pagination for users with many saved searches

### 5. Security
- Always use authenticated requests
- Validate user ownership of saved searches
- Sanitize search criteria inputs