# Seatmap Backend Service - API Specification

## Base URL

- **Development**: `https://api-dev.seatmap.example.com`
- **Production**: `https://api.seatmap.example.com`

## Authentication

All requests require one of the following:

### Client Authentication (API Key)

```
Header: X-API-Key: <api-key>

```

- Required for all API requests
- Obtained from API Gateway
- Different keys for different client applications

### User Authentication (JWT)

```
Header: Authorization: Bearer <jwt-token>

```

- Required for user-specific operations
- Obtained from `/auth/login`, `/auth/google`, `/auth/apple`, or `/auth/guest`
- Expires after 24 hours

---

## Authentication Endpoints

### Register with Email/Password

```
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}

```

**Response 201 Created**:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "email",
  "message": "Verification email sent"
}

```

**Errors**:

- `400` - Invalid email format, weak password, missing fields
- `409` - Email already registered

---

### Login with Email/Password

```
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "SecurePass123!"
}

```

**Response 200 OK**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "email",
  "expiresIn": 86400
}

```

**Errors**:

- `401` - Invalid credentials
- `423` - Account locked (too many failed attempts)

---

### Google Sign-In

```
POST /auth/google
Content-Type: application/json

{
  "idToken": "google-id-token-from-frontend"
}

```

**Response 200 OK**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid",
  "email": "user@gmail.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "google",
  "profilePicture": "https://...",
  "isNewUser": false,
  "expiresIn": 86400
}

```

**Errors**:

- `401` - Invalid Google token
- `400` - Missing idToken

---

### Apple Sign In

```
POST /auth/apple
Content-Type: application/json

{
  "identityToken": "apple-identity-token",
  "authorizationCode": "apple-auth-code",
  "user": {
    "firstName": "John",
    "lastName": "Doe",
    "email": "user@privaterelay.appleid.com"
  }
}

```

**Response 200 OK**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "uuid",
  "email": "user@privaterelay.appleid.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "apple",
  "isNewUser": true,
  "expiresIn": 86400
}

```

**Errors**:

- `401` - Invalid Apple token
- `400` - Missing required fields

---

### Continue as Guest

```
POST /auth/guest
Content-Type: application/json

{}

```

**Response 200 OK**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "sessionId": "guest_uuid",
  "role": "guest",
  "guestLimits": {
    "flightsViewed": 0,
    "maxFlights": 2
  },
  "expiresIn": 86400,
  "message": "Guest session created. You can view up to 2 seat maps."
}

```

---

### Forgot Password

```
POST /auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}

```

**Response 200 OK**:

```json
{
  "message": "If the email exists, a password reset link has been sent."
}

```

**Note**: Always returns 200 to prevent email enumeration

---

### Reset Password

```
POST /auth/reset-password
Content-Type: application/json

{
  "token": "reset-token-from-email",
  "newPassword": "NewSecurePass123!"
}

```

**Response 200 OK**:

```json
{
  "message": "Password reset successful. You can now log in."
}

```

**Errors**:

- `400` - Invalid or expired token, weak password

---

### Refresh Token

```
POST /auth/refresh
Authorization: Bearer <jwt-token>

```

**Response 200 OK**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400
}

```

---

## Flight Search Endpoints

### Search Flights

```
POST /flights/search
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "origin": "JFK",
  "destination": "LAX",
  "departureDate": "2025-12-15",
  "returnDate": "2025-12-20",
  "travelClass": "economy",
  "airline": "AA",
  "flightNumber": "100"
}

```

**Request Parameters**:

- `origin` (required): 3-letter IATA airport code
- `destination` (required): 3-letter IATA airport code
- `departureDate` (required): ISO date (YYYY-MM-DD), must be future
- `returnDate` (optional): ISO date for round trip
- `travelClass` (optional): `economy` (default), `premium_economy`, `business`, `first`
- `airline` (optional): 2-letter IATA airline code
- `flightNumber` (optional): Numeric, requires `airline`

**Response 200 OK**:

```json
{
  "searchId": "uuid",
  "flights": [
    {
      "flightId": "AA100-JFK-LAX-20251215",
      "airline": {
        "code": "AA",
        "name": "American Airlines"
      },
      "flightNumber": "100",
      "origin": {
        "code": "JFK",
        "name": "John F. Kennedy International",
        "city": "New York"
      },
      "destination": {
        "code": "LAX",
        "name": "Los Angeles International",
        "city": "Los Angeles"
      },
      "departure": {
        "dateTime": "2025-12-15T08:00:00",
        "terminal": "8"
      },
      "arrival": {
        "dateTime": "2025-12-15T11:30:00",
        "terminal": "4"
      },
      "duration": "PT5H30M",
      "travelClass": "ECONOMY",
      "aircraft": "Boeing 737-800",
      "price": {
        "amount": 299.99,
        "currency": "USD"
      },
      "availableSeats": 42,
      "source": "amadeus",
      "stops": 0
    }
  ],
  "sources": ["amadeus", "sabre"],
  "cached": false,
  "totalResults": 15,
  "filters": {
    "travelClass": "economy",
    "airline": "AA"
  }
}

```

**Errors**:

- `400` - Invalid airport codes, invalid date, flightNumber without airline
- `404` - No flights found

---

### Get Seat Map

```
GET /flights/{flightId}/seatmap
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "flightId": "AA100-JFK-LAX-20251215",
  "airline": "AA",
  "flightNumber": "100",
  "aircraft": "Boeing 737-800",
  "seatmap": {
    "cabins": [
      {
        "cabinType": "ECONOMY",
        "rows": [
          {
            "rowNumber": 10,
            "seats": [
              {
                "seatNumber": "10A",
                "available": true,
                "characteristics": ["WINDOW"],
                "price": {
                  "amount": 0,
                  "currency": "USD"
                }
              },
              {
                "seatNumber": "10B",
                "available": true,
                "characteristics": ["MIDDLE"],
                "price": {
                  "amount": 0,
                  "currency": "USD"
                }
              },
              {
                "seatNumber": "10C",
                "available": false,
                "characteristics": ["AISLE"],
                "price": null
              }
            ]
          }
        ]
      }
    ]
  },
  "source": "amadeus",
  "guestLimitsRemaining": 1
}

```

**For Guest Users**:

- Response includes `guestLimitsRemaining` field
- After 2 views, returns `403` with upgrade message

**Errors**:

- `404` - Flight not found
- `403` - Guest limit reached (see error response below)
- `401` - Invalid or expired token

**Guest Limit Error (403)**:

```json
{
  "error": {
    "code": "GUEST_LIMIT_REACHED",
    "message": "You've reached your guest limit of 2 seat map views. Please sign up to continue.",
    "details": {
      "viewsUsed": 2,
      "maxViews": 2,
      "upgradeOptions": [
        "register",
        "google",
        "apple"
      ]
    }
  }
}

```

---

## User Management Endpoints

### Get User Profile

```
GET /users/profile
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "email",
  "profilePicture": null,
  "createdAt": "2025-10-01T12:00:00Z",
  "subscription": {
    "status": "active",
    "currentPeriodEnd": "2025-11-01T12:00:00Z"
  }
}

```

---

### Update User Profile

```
PUT /users/profile
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith"
}

```

**Response 200 OK**:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Smith",
  "updatedAt": "2025-10-14T15:30:00Z"
}

```

---

### Change Password

```
PUT /users/password
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "currentPassword": "OldPass123!",
  "newPassword": "NewPass123!"
}

```

**Response 200 OK**:

```json
{
  "message": "Password updated successfully. All other sessions have been invalidated."
}

```

**Errors**:

- `401` - Current password incorrect
- `400` - New password doesn't meet requirements

---

### Delete Account

```
DELETE /users/account
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "password": "CurrentPass123!",
  "confirmDelete": true
}

```

**Response 200 OK**:

```json
{
  "message": "Account deleted successfully. All data has been removed."
}

```

**Errors**:

- `401` - Password incorrect
- `400` - confirmDelete not true

---

## Subscription Endpoints

### Subscribe

```
POST /subscriptions/subscribe
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "paymentMethodId": "pm_1234567890"
}

```

**Request Parameters**:

- `paymentMethodId` (required): Stripe payment method ID from frontend

**Response 200 OK**:

```json
{
  "subscriptionId": "sub_1234567890",
  "status": "active",
  "currentPeriodStart": "2025-10-14T15:30:00Z",
  "currentPeriodEnd": "2025-11-14T15:30:00Z",
  "amount": 5.00,
  "currency": "USD"
}

```

**Errors**:

- `400` - Invalid payment method
- `409` - User already has active subscription
- `402` - Payment failed

---

### Cancel Subscription

```
POST /subscriptions/cancel
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "subscriptionId": "sub_1234567890",
  "status": "active",
  "cancelAtPeriodEnd": true,
  "currentPeriodEnd": "2025-11-14T15:30:00Z",
  "message": "Subscription will be cancelled at the end of the current billing period."
}

```

---

### Get Subscription Status

```
GET /subscriptions/status
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "subscriptionId": "sub_1234567890",
  "status": "active",
  "currentPeriodStart": "2025-10-14T15:30:00Z",
  "currentPeriodEnd": "2025-11-14T15:30:00Z",
  "cancelAtPeriodEnd": false,
  "amount": 5.00,
  "currency": "USD"
}

```

**Response 404 Not Found** (No subscription):

```json
{
  "message": "No active subscription found"
}

```

---

### Update Payment Method

```
PUT /subscriptions/payment-method
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "paymentMethodId": "pm_0987654321"
}

```

**Response 200 OK**:

```json
{
  "message": "Payment method updated successfully",
  "paymentMethod": {
    "id": "pm_0987654321",
    "type": "card",
    "last4": "4242",
    "brand": "visa"
  }
}

```

---

## Bookmark Endpoints

### Create Bookmark

```
POST /bookmarks
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>
Content-Type: application/json

{
  "flightId": "AA100-JFK-LAX-20251215",
  "flightData": {
    "airline": "AA",
    "flightNumber": "100",
    "origin": "JFK",
    "destination": "LAX",
    "departureTime": "2025-12-15T08:00:00",
    "arrivalTime": "2025-12-15T11:30:00",
    "price": 299.99,
    "travelClass": "ECONOMY",
    "source": "amadeus"
  },
  "departureDate": "2025-12-15"
}

```

**Response 201 Created**:

```json
{
  "bookmarkId": "uuid",
  "flightId": "AA100-JFK-LAX-20251215",
  "createdAt": "2025-10-14T15:30:00Z",
  "expiresAt": "2025-12-16T00:00:00Z"
}

```

**Errors**:

- `400` - Invalid flight data, missing required fields
- `403` - Bookmark limit reached (50 max)
- `409` - Flight already bookmarked

---

### Get Bookmarks

```
GET /bookmarks
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "bookmarks": [
    {
      "bookmarkId": "uuid",
      "flightId": "AA100-JFK-LAX-20251215",
      "flightData": {
        "airline": "AA",
        "flightNumber": "100",
        "origin": "JFK",
        "destination": "LAX",
        "departureTime": "2025-12-15T08:00:00",
        "arrivalTime": "2025-12-15T11:30:00",
        "price": 299.99,
        "travelClass": "ECONOMY",
        "source": "amadeus"
      },
      "departureDate": "2025-12-15",
      "createdAt": "2025-10-14T15:30:00Z",
      "daysUntilDeparture": 62
    }
  ],
  "total": 1,
  "limit": 50
}

```

---

### Delete Bookmark

```
DELETE /bookmarks/{bookmarkId}
Authorization: Bearer <jwt-token>
X-API-Key: <api-key>

```

**Response 200 OK**:

```json
{
  "message": "Bookmark deleted successfully"
}

```

**Errors**:

- `404` - Bookmark not found
- `403` - Bookmark belongs to another user

---

## Error Response Format

All errors follow this structure:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": {
      "field": "fieldName",
      "reason": "specific reason"
    },
    "timestamp": "2025-10-14T15:30:00Z",
    "requestId": "uuid"
  }
}

```

### Common Error Codes

| HTTP Status | Code | Description |
| --- | --- | --- |
| 400 | INVALID_REQUEST | Request validation failed |
| 401 | UNAUTHORIZED | Invalid or missing authentication |
| 403 | FORBIDDEN | Insufficient permissions |
| 403 | GUEST_LIMIT_REACHED | Guest has viewed 2 seat maps |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | Resource already exists |
| 423 | ACCOUNT_LOCKED | Too many failed login attempts |
| 429 | RATE_LIMIT_EXCEEDED | Too many requests |
| 500 | INTERNAL_ERROR | Server error |
| 502 | EXTERNAL_API_ERROR | External service unavailable |
| 503 | SERVICE_UNAVAILABLE | Service temporarily down |

---

## Rate Limiting

Rate limits are enforced per API key:

| Endpoint Category | Rate Limit |
| --- | --- |
| Authentication | 10 requests/minute |
| Flight Search | 30 requests/minute |
| Seat Maps | 20 requests/minute |
| User Management | 20 requests/minute |
| Subscriptions | 10 requests/minute |
| Bookmarks | 30 requests/minute |

**Rate Limit Headers**:

```
X-RateLimit-Limit: 30
X-RateLimit-Remaining: 25
X-RateLimit-Reset: 1697294400

```

**Rate Limit Exceeded Response (429)**:

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Rate limit exceeded. Try again in 45 seconds.",
    "details": {
      "limit": 30,
      "remaining": 0,
      "resetAt": "2025-10-14T15:31:00Z"
    }
  }
}

```

---

## Pagination

For endpoints that return lists (future enhancement):

**Request**:

```
GET /bookmarks?page=2&limit=20

```

**Response**:

```json
{
  "data": [...],
  "pagination": {
    "page": 2,
    "limit": 20,
    "total": 45,
    "pages": 3
  }
}

```

---

## Versioning

API versioning via URL path (future):

- Current: No version (implicit v1)
- Future: `/v2/flights/search`

---

## CORS Configuration

Allowed origins (configurable per environment):

- Development: `http://localhost:3000`, `http://localhost:8080`
- Production: `https://app.seatmap.example.com`

Allowed methods: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`

Allowed headers: `Authorization`, `Content-Type`, `X-API-Key`

---

## Webhooks (Stripe)

### Subscription Webhook

```
POST /webhooks/stripe
Content-Type: application/json
Stripe-Signature: <signature>

{
  "id": "evt_...",
  "type": "invoice.payment_failed",
  "data": {
    "object": {
      "customer": "cus_...",
      "subscription": "sub_...",
      ...
    }
  }
}

```

**Handled Events**:

- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_succeeded`
- `invoice.payment_failed`

**Response 200 OK**:

```json
{
  "received": true
}

```