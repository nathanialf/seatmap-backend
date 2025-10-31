# User Profile API

## Overview

Manage user profile information and account settings. **Requires user authentication (not guest tokens).**

---

## Get User Profile

Retrieve the current user's profile information.

**Endpoint**: `GET /profile`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Response**:
```json
{
  "success": true,
  "data": {
    "userId": "user_456def",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "authProvider": "EMAIL",
    "emailVerified": true,
    "status": "ACTIVE",
    "accountTier": "PRO",
    "createdAt": "2025-10-15T09:30:00Z",
    "updatedAt": "2025-10-30T08:15:00Z"
  }
}
```

**Profile Fields**:
- `userId`: Unique user identifier
- `email`: User's email address (used for login)
- `firstName`: User's first name
- `lastName`: User's last name  
- `fullName`: Computed full name (firstName + lastName)
- `authProvider`: Authentication method (`EMAIL`, `GOOGLE`, `APPLE`)
- `emailVerified`: Email verification status
- `status`: Account status (`ACTIVE`, `SUSPENDED`)
- `accountTier`: User's access tier (`FREE`, `PRO`, `BUSINESS`)
- `createdAt`: Account creation timestamp
- `updatedAt`: Last profile update timestamp

**Note**: Current month usage statistics and remaining quotas can be retrieved via separate usage endpoints (check individual API documentation for usage tracking details).

**Example cURL**:
```bash
curl -X GET {BASE_URL}/profile \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Update User Profile

Update the current user's profile information.

**Endpoint**: `PUT /profile`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
Content-Type: application/json
```

**Request Body**:
```json
{
  "firstName": "John",
  "lastName": "Smith"
}
```

**Updatable Fields**:
- `firstName` (optional): User's first name
- `lastName` (optional): User's last name

**Response**:
```json
{
  "success": true,
  "data": {
    "userId": "user_456def",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Smith",
    "fullName": "John Smith",
    "authProvider": "EMAIL",
    "emailVerified": true,
    "status": "ACTIVE",
    "accountTier": "PRO",
    "createdAt": "2025-10-15T09:30:00Z",
    "updatedAt": "2025-10-30T08:20:00Z"
  }
}
```

**Example cURL**:
```bash
curl -X PUT {BASE_URL}/profile \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}" \
  -d '{
    "firstName": "John",
    "lastName": "Smith"
  }'
```

---

## Account Tiers

User accounts have different access levels that affect API usage:

### FREE Tier
- **Flight Search**: Full access
- **Seat Maps**: Monthly quota limits
- **Bookmarks**: Not available
- **Rate Limits**: Basic monthly quotas

### PRO Tier
- **Flight Search**: Full access
- **Seat Maps**: Enhanced monthly limits  
- **Bookmarks**: Monthly quota available
- **Rate Limits**: Increased monthly quotas
- **Priority Support**: Enhanced support access

### BUSINESS Tier
- **Flight Search**: Full access
- **Seat Maps**: Unlimited access
- **Bookmarks**: Unlimited
- **Rate Limits**: No monthly quotas
- **Priority Support**: Dedicated support
- **Advanced Features**: Beta access to new features
- **Permanent Upgrade**: Cannot be downgraded (one-time purchase)

---

## Profile Management

### Email Changes
Email addresses cannot be changed through the profile API. Contact support for email address modifications.

### Password Changes
Password changes are handled through separate authentication endpoints (not part of profile API).

### Account Status
- **ACTIVE**: Normal account with full access
- **SUSPENDED**: Account temporarily disabled

### Account Deletion
Account deletion requests must be processed through support channels.

### Data Export
Users can request data export through support for compliance with data protection regulations.

---

## Authentication Provider Information

### Email Authentication
- **Provider**: `EMAIL`
- **Verification**: Email verification required
- **Login**: Email and password

### Social Authentication
- **Providers**: `GOOGLE`, `APPLE`
- **Profile Sync**: Basic profile information synchronized
- **Email**: Primary email from provider used

---

## Error Responses

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Invalid profile data provided"
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
  "message": "Profile access requires user authentication"
}
```

**404 Not Found**:
```json
{
  "success": false,
  "message": "User profile not found"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Error processing profile request"
}
```

---

## Security Notes

- **Token Validation**: All profile operations validate JWT tokens
- **User Isolation**: Users can only access their own profile data
- **Sensitive Data**: Passwords and internal IDs are never exposed
- **Audit Trail**: Profile changes are logged for security monitoring
- **Email Verification**: Critical operations may require verified email