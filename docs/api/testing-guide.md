# Testing Guide

## Overview

Complete step-by-step guide for testing all API endpoints using cURL commands. Follow this guide to verify your API integration and understand the complete workflow.

---

## Prerequisites

Before testing, ensure you have:
- **API Key**: Contact administrator for your API credentials
- **Base URL**: Your environment's API endpoint
- **cURL**: Command-line tool for making HTTP requests

---

## Quick Start Testing

### Step 1: Get Guest Token
Start with a guest token for basic API access:

```bash
curl -X POST {BASE_URL}/auth/guest \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}"
```

**Expected Response**:
```json
{
  "success": true,
  "token": "guest_jwt_token_here",
  "userId": "guest_generated_id",
  "authProvider": "GUEST",
  "expiresIn": 86400,
  "guestLimits": {
    "maxFlights": 2,
    "flightsViewed": 0
  },
  "message": "Guest session created. You have 2 seat map views remaining."
}
```

### Step 2: Search Flights
Use the guest token to search for flights:

```bash
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {GUEST_TOKEN}" \
  -d '{
    "origin": "LAX",
    "destination": "JFK",
    "departureDate": "2025-12-15",
    "travelClass": "ECONOMY",
    "maxResults": 3
  }'
```

### Step 3: Get Seat Map
Use a flight offer from Step 2 to get seat map:

```bash
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {GUEST_TOKEN}" \
  -d '{
    "flightOfferData": "{COMPLETE_FLIGHT_OFFER_JSON_FROM_STEP_2}"
  }'
```

---

## Complete Authentication Flow

### Test User Registration

```bash
curl -X POST {BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "newUser": true,
  "pending": true,
  "message": "Registration successful. Please check your email to verify your account."
}
```

### Test Email Verification
After receiving verification email:

```bash
curl -X GET "{BASE_URL}/auth/verify?token={VERIFICATION_TOKEN_FROM_EMAIL}" \
  -H "X-API-Key: {YOUR_API_KEY}"
```

**Expected Response**:
```json
{
  "success": true,
  "token": "user_jwt_token_here",
  "userId": "user_generated_id",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "authProvider": "EMAIL",
  "expiresIn": 86400,
  "message": "Email verified successfully! Welcome to Seatmap."
}
```

### Test User Login

```bash
curl -X POST {BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "test@example.com",
    "password": "SecurePass123!"
  }'
```

---

## Flight Search Testing

### Basic Flight Search

```bash
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "LAX",
    "destination": "JFK",
    "departureDate": "2025-12-15"
  }'
```

### Advanced Flight Search with Filters

```bash
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "SFO",
    "destination": "ORD",
    "departureDate": "2025-12-20",
    "travelClass": "BUSINESS",
    "flightNumber": "UA456",
    "maxResults": 5
  }'
```

### Test Different Routes

```bash
# Domestic US flight
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "DEN",
    "destination": "MIA",
    "departureDate": "2025-12-25"
  }'

# International flight
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "JFK",
    "destination": "LHR",
    "departureDate": "2025-12-30"
  }'
```

---

## Seat Map Testing

### Direct Seat Map Request
Save a complete flight offer from flight search, then:

```bash
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"itineraries\":[{\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"}}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\"}}"
  }'
```

### Test Both Data Sources
Try flights from different providers to test routing:

```bash
# Test Amadeus seat map
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "flightOfferData": "{AMADEUS_FLIGHT_OFFER_WITH_dataSource_AMADEUS}"
  }'

# Test Sabre seat map  
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "flightOfferData": "{SABRE_FLIGHT_OFFER_WITH_dataSource_SABRE}"
  }'
```

---

## Bookmark Testing

**Note**: Bookmark endpoints require user authentication (not guest tokens).

### Create Bookmark

```bash
curl -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}" \
  -d '{
    "title": "LAX to JFK - Holiday Flight",
    "flightOfferData": "{COMPLETE_FLIGHT_OFFER_JSON_STRING}"
  }'
```

### List User Bookmarks

```bash
curl -X GET {BASE_URL}/bookmarks \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}"
```

### Get Seat Map by Bookmark
Using bookmark ID from create/list response:

```bash
curl -X GET {BASE_URL}/seat-map/bookmark/{BOOKMARK_ID} \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}"
```

### Delete Bookmark

```bash
curl -X DELETE {BASE_URL}/bookmarks/{BOOKMARK_ID} \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}"
```

---

## User Profile Testing

### Get Profile

```bash
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}"
```

### Update Profile

```bash
curl -X PUT {BASE_URL}/profile \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {USER_JWT_TOKEN}" \
  -d '{
    "firstName": "Updated",
    "lastName": "Name"
  }'
```

---

## Error Testing

### Test Missing API Key

```bash
curl -X GET {BASE_URL}/profile \
  -H "Authorization: Bearer {USER_JWT_TOKEN}"
```

**Expected**: 401 Unauthorized

### Test Invalid Token

```bash
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer invalid_token"
```

**Expected**: 401 Unauthorized

### Test Guest Token on User Endpoint

```bash
curl -X GET {BASE_URL}/bookmarks \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {GUEST_TOKEN}"
```

**Expected**: 403 Forbidden

### Test Validation Errors

```bash
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "INVALID",
    "destination": "",
    "departureDate": "invalid-date"
  }'
```

**Expected**: 400 Bad Request with validation details

---

## Complete Integration Test Workflow

Follow this sequence to test the complete user journey:

### 1. Guest Experience
```bash
# Get guest token
GUEST_TOKEN=$(curl -s -X POST {BASE_URL}/auth/guest \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" | jq -r '.token')

# Search flights as guest
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $GUEST_TOKEN" \
  -d '{
    "origin": "LAX",
    "destination": "JFK", 
    "departureDate": "2025-12-15"
  }'

# Get seat map as guest (save flight offer data first)
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $GUEST_TOKEN" \
  -d '{
    "flightOfferData": "{FLIGHT_OFFER_FROM_SEARCH}"
  }'
```

### 2. User Registration & Verification
```bash
# Register new user
curl -X POST {BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "integration-test@example.com",
    "password": "TestPass123!",
    "firstName": "Integration",
    "lastName": "Test"
  }'

# Verify email (get token from email)
USER_TOKEN=$(curl -s -X GET "{BASE_URL}/auth/verify?token={VERIFICATION_TOKEN}" \
  -H "X-API-Key: {YOUR_API_KEY}" | jq -r '.token')
```

### 3. Full User Experience
```bash
# Search flights as user
curl -X POST {BASE_URL}/flight-offers \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "origin": "SFO",
    "destination": "ORD",
    "departureDate": "2025-12-20"
  }'

# Create bookmark
BOOKMARK_ID=$(curl -s -X POST {BASE_URL}/bookmarks \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -d '{
    "title": "Test Bookmark",
    "flightOfferData": "{FLIGHT_OFFER_JSON}"
  }' | jq -r '.data.bookmarkId')

# Get seat map by bookmark
curl -X GET {BASE_URL}/seat-map/bookmark/$BOOKMARK_ID \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $USER_TOKEN"

# Check profile
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer $USER_TOKEN"
```

---

## Performance Testing

### Rate Limit Testing
Test rate limits by making multiple rapid requests:

```bash
# Test guest rate limits
for i in {1..10}; do
  curl -X POST {BASE_URL}/seat-map \
    -H "Content-Type: application/json" \
    -H "X-API-Key: {YOUR_API_KEY}" \
    -H "Authorization: Bearer {GUEST_TOKEN}" \
    -d '{
      "flightOfferData": "{FLIGHT_OFFER_JSON}"
    }'
  sleep 1
done
```

### Concurrent Request Testing
```bash
# Test concurrent flight searches
for i in {1..5}; do
  curl -X POST {BASE_URL}/flight-offers \
    -H "Content-Type: application/json" \
    -H "X-API-Key: {YOUR_API_KEY}" \
    -H "Authorization: Bearer {USER_TOKEN}" \
    -d "{
      \"origin\": \"LAX\",
      \"destination\": \"JFK\",
      \"departureDate\": \"2025-12-$(printf %02d $((15 + i)))\"
    }" &
done
wait
```

---

## Troubleshooting Common Issues

### Token Expiration
If getting 401 errors, tokens may have expired. Get new tokens:

```bash
# For user tokens, login again
USER_TOKEN=$(curl -s -X POST {BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "your@email.com",
    "password": "YourPassword123!"
  }' | jq -r '.token')
```

### JSON Escaping Issues
When passing flight offer data, ensure proper escaping:

```bash
# Correct: Escape quotes in flight offer data
"flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\"}"

# Incorrect: Unescaped quotes will break JSON
"flightOfferData": "{"type":"flight-offer","dataSource":"AMADEUS"}"
```

### Environment Variables
Set up environment variables for easier testing:

```bash
export API_KEY="your_api_key"
export BASE_URL="your_base_url"
export USER_TOKEN="your_user_token"

# Then use in commands
curl -X GET $BASE_URL/profile \
  -H "X-API-Key: $API_KEY" \
  -H "Authorization: Bearer $USER_TOKEN"
```