# Error Handling

## Overview

All API endpoints return consistent error responses with appropriate HTTP status codes and descriptive messages to help developers integrate effectively.

---

## Standard Error Response Format

All errors follow this consistent structure:

```json
{
  "success": false,
  "message": "Human-readable error description",
  "errors": ["Optional array of specific validation errors"]
}
```

**Fields**:
- `success`: Always `false` for error responses
- `message`: Primary error message describing what went wrong
- `errors`: Optional array of detailed error messages (mainly for validation errors)

---

## HTTP Status Codes

### 400 Bad Request
Invalid request format, missing required fields, or validation failures.

**Common Causes**:
- Missing required request body
- Invalid JSON format
- Failed field validation (email format, password requirements, etc.)
- Invalid date formats or airport codes

**Example Response**:
```json
{
  "success": false,
  "message": "Validation errors: Origin is required; Departure date must be in YYYY-MM-DD format;"
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "INVALID",
    "destination": ""
  }'
```

---

### 401 Unauthorized
Authentication problems with API keys or JWT tokens.

**Common Causes**:
- Missing `X-API-Key` header
- Invalid or expired API key
- Missing `Authorization` header
- Invalid or expired JWT token
- Malformed Bearer token format

**Example Responses**:
```json
{
  "success": false,
  "message": "Missing or invalid authorization header"
}
```

```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

**Example cURL**:
```bash
# Missing API key
curl -X GET {BASE_URL}/profile \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}"

# Invalid token
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer invalid_token"
```

---

### 403 Forbidden
Valid authentication but insufficient permissions.

**Common Causes**:
- Guest tokens accessing user-only features (bookmarks, profile)
- Rate limit exceeded for user tier
- Account suspended or restricted
- Feature not available for current account tier

**Example Responses**:
```json
{
  "success": false,
  "message": "Bookmark access requires user authentication"
}
```

```json
{
  "success": false,
  "message": "Rate limit exceeded for your account tier"
}
```

**Example cURL**:
```bash
# Guest token trying to access bookmarks
curl -X GET {BASE_URL}/bookmarks \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {GUEST_JWT_TOKEN}"
```

---

### 404 Not Found
Requested resource does not exist or user lacks access.

**Common Causes**:
- Bookmark ID not found or belongs to different user
- User profile not found
- Invalid endpoint URL
- Resource deleted or expired

**Example Response**:
```json
{
  "success": false,
  "message": "Bookmark not found or access denied"
}
```

**Example cURL**:
```bash
curl -X GET {BASE_URL}/seat-map/bookmark/invalid_bookmark_id \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}"
```

---

### 409 Conflict
Request conflicts with current system state.

**Common Causes**:
- Bookmark storage limits exceeded
- Duplicate resource creation attempts
- Account tier restrictions preventing action

**Example Response**:
```json
{
  "success": false,
  "message": "Bookmark limit reached for your account tier"
}
```

**Example cURL**:
```bash
# Trying to create bookmark when at limit
curl -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "title": "New Flight",
    "flightOfferData": "{...}"
  }'
```

---

### 429 Too Many Requests
Rate limiting in effect for the current user or IP.

**Common Causes**:
- Exceeded API call limits for user tier
- IP-based rate limiting for guest users
- Seat map access limits reached

**Example Response**:
```json
{
  "success": false,
  "message": "Rate limit exceeded. Please try again later."
}
```

**Rate Limit Headers** (may be included):
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1698768000
```

---

### 500 Internal Server Error
Unexpected server-side errors or external service failures.

**Common Causes**:
- External API failures (Amadeus, Sabre)
- Database connectivity issues
- Internal service errors
- Temporary system maintenance

**Example Response**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```

**Note**: For security reasons, detailed internal error information is not exposed to clients.

---

## Field Validation Errors

### Flight Search Validation
```json
{
  "success": false,
  "message": "Validation errors: Origin must be a 3-letter airport code; Departure date must be in YYYY-MM-DD format;"
}
```

### Authentication Validation
```json
{
  "success": false,
  "message": "Validation errors: Password must contain at least 1 uppercase letter; Password must contain at least 1 special character;"
}
```

### Bookmark Validation
```json
{
  "success": false,
  "message": "Validation errors: Title is required; Flight offer data is required;"
}
```

---

## Error Handling Best Practices

### Client-Side Handling
1. **Check HTTP Status**: Always check the HTTP status code first
2. **Parse Error Message**: Display the `message` field to users
3. **Handle Specific Codes**: Implement specific handling for common error codes
4. **Retry Logic**: Implement appropriate retry logic for 429 and 500 errors
5. **Graceful Degradation**: Provide fallback functionality where possible

### Retry Strategies
- **401 Errors**: Do not retry, fix authentication
- **429 Errors**: Implement exponential backoff
- **500 Errors**: Retry with backoff, but limit attempts
- **Network Errors**: Retry with reasonable timeouts

### Example Error Handling (JavaScript)
```javascript
async function handleApiResponse(response) {
  if (!response.ok) {
    const errorData = await response.json();
    
    switch (response.status) {
      case 401:
        // Redirect to login or refresh token
        redirectToLogin();
        break;
      case 429:
        // Implement retry with backoff
        await retryWithBackoff();
        break;
      case 500:
        // Show generic error message
        showError("Service temporarily unavailable");
        break;
      default:
        // Show specific error message
        showError(errorData.message);
    }
    
    throw new Error(errorData.message);
  }
  
  return response.json();
}
```

---

## Debugging Tips

### Common Integration Issues
1. **Missing Headers**: Ensure both `X-API-Key` and `Authorization` headers are present
2. **JSON Escaping**: When passing flight offer data, ensure proper JSON string escaping
3. **Date Formats**: Use YYYY-MM-DD format for all date fields
4. **Airport Codes**: Use 3-letter IATA codes (LAX, JFK, etc.)
5. **Token Format**: Authorization header must use "Bearer " prefix

### Testing Error Scenarios
```bash
# Test missing API key
curl -X GET {BASE_URL}/profile

# Test invalid token format
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: {JWT_TOKEN_WITHOUT_BEARER}"

# Test validation error
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{}'
```