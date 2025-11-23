# Authentication API

## Overview

Authentication endpoints manage user registration, login, and token generation. All authentication endpoints require an API key.

---

## Guest Token Generation

Create a temporary guest token for limited API access.

**Endpoint**: `POST /auth/guest`

**Headers**:
```
X-API-Key: your_api_key
Content-Type: application/json
```

**Request Body**: None required

**Response**:
```json
{
  "success": true,
  "token": "jwt_token_string",
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

**Example cURL**:
```bash
curl -X POST {BASE_URL}/auth/guest \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}"
```

---

## User Registration

Register a new user account. **Note**: This sends a verification email and does NOT return a JWT token immediately.

**Endpoint**: `POST /auth/register`

**Headers**:
```
X-API-Key: your_api_key
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Password Requirements**:
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter  
- At least 1 number
- At least 1 special character (`!@#$%^&*`)

**Response**:
```json
{
  "success": true,
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "newUser": true,
  "pending": true,
  "message": "Registration successful. Please check your email to verify your account."
}
```

**Important**: 
- No JWT token is provided until email verification is complete
- User must check email and click verification link
- Verification token expires in 1 hour
- **Guest Usage Transfer**: If you used guest seat map views from the same IP address within the last 30 days, those views will be automatically transferred to your new user account, counting toward your monthly limit

**Example cURL**:
```bash
curl -X POST {BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

---

## Email Verification

Verify email address using token from verification email. **This endpoint provides the JWT token.**

**Endpoint**: `GET /auth/verify?token={verification_token}`

**Headers**:
```
X-API-Key: your_api_key
```

**Query Parameters**:
- `token` (required): Verification token from email

**Response**:
```json
{
  "success": true,
  "token": "jwt_token_string",
  "userId": "user_generated_id",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "EMAIL",
  "profilePicture": null,
  "expiresIn": 86400,
  "message": "Email verified successfully! Welcome to Seatmap."
}
```

**Example cURL**:
```bash
curl -X GET "{BASE_URL}/auth/verify?token={VERIFICATION_TOKEN}" \
  -H "X-API-Key: {YOUR_API_KEY}"
```

---

## Resend Verification Email

Resend verification email if the original expires or is lost.

**Endpoint**: `POST /auth/resend-verification`

**Headers**:
```
X-API-Key: your_api_key
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com"
}
```

**Response**:
```json
{
  "success": true,
  "message": "Verification email sent. Please check your inbox."
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/auth/resend-verification \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "user@example.com"
  }'
```

---

## User Login

Login with existing verified user credentials.

**Endpoint**: `POST /auth/login`

**Headers**:
```
X-API-Key: your_api_key
Content-Type: application/json
```

**Request Body**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response**:
```json
{
  "success": true,
  "token": "jwt_token_string",
  "userId": "user_id",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "authProvider": "EMAIL",
  "profilePicture": null,
  "expiresIn": 86400,
  "message": "Login successful",
  "newUser": false
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d '{
    "email": "user@example.com",
    "password": "SecurePass123!"
  }'
```

---

## Guest Usage Transfer

When you register for a new account, any guest seat map views made from your IP address within the last 30 days are automatically transferred to your new user account. This ensures a seamless transition from guest usage to authenticated usage.

### How It Works

1. **Guest Usage Tracking**: Guest users can view up to 2 seat maps per IP address over 30 days
2. **Registration Transfer**: During account registration, the system checks for guest usage from your IP
3. **Automatic Transfer**: Guest seat map views are added to your current month's usage count
4. **Fair Usage**: You get full tier limits minus any guest usage, ensuring fair access

### Examples

**Example 1: Fresh Registration**
- Guest usage: 0 seat map views from your IP
- After registration: Start with 0 usage, full tier limits available

**Example 2: Partial Guest Usage**  
- Guest usage: 1 seat map view from your IP
- After registration: Start with 1 usage counted toward your monthly limit
- FREE tier: 3 remaining (4 total - 1 transferred)
- PRO tier: 49 remaining (50 total - 1 transferred)

**Example 3: Maximum Guest Usage**
- Guest usage: 2 seat map views from your IP  
- After registration: Start with 2 usage counted toward your monthly limit
- FREE tier: 2 remaining (4 total - 2 transferred)
- PRO tier: 48 remaining (50 total - 2 transferred)

### Transfer Conditions

Guest usage is transferred only when:
- ✅ Registration occurs from the same IP address as guest usage
- ✅ Guest access record exists and is not expired (within 30 days)
- ✅ Guest has made at least 1 seat map view
- ❌ No transfer if guest record is expired or doesn't exist
- ❌ No transfer if guest has 0 seat map views

### Error Handling

- Guest usage transfer failures do not prevent account registration
- If transfer fails, you start with a clean usage count
- Transfer is logged for audit purposes but runs in the background

---

## Registration Flow Summary

1. **Register**: `POST /auth/register` → Returns success with `pending: true`
2. **Check Email**: User receives verification email with token
3. **Verify**: `GET /auth/verify?token=xyz` → Returns JWT token
4. **Use API**: Now user can access full API with JWT token

## Token Usage

### Guest Tokens
- **Scope**: Limited flight search and seat map access
- **Duration**: 30 days
- **Limitations**: IP-based rate limiting, restricted features

### User Tokens  
- **Scope**: Full API access based on user tier (FREE, PRO, BUSINESS, DEV)
- **Duration**: 30 days
- **Features**: Tier-based bookmarks, enhanced rate limits, priority access

### Token Headers
All authenticated requests (except auth endpoints):
```
X-API-Key: your_api_key
Authorization: Bearer your_jwt_token
```

---

## User Account Tiers

After successful login or email verification, users are assigned to account tiers with different usage limits:

### Tier-Based Access
- **FREE Tier**: Basic access with limited monthly quotas
- **PRO Tier**: Enhanced access with increased monthly limits
- **BUSINESS Tier**: Premium access with unlimited usage (one-time purchase, cannot be downgraded)
- **DEV Tier**: Developer access with unlimited usage (restricted access, contact support)

### Usage Tracking
- Monthly limits reset on the 1st of each month
- Usage counters are tracked per user per month
- Exceeded limits return 403 Forbidden responses with upgrade suggestions
- Current tier limits and usage can be retrieved via the user profile endpoint

### Error Responses for Tier Limits
When users exceed their tier limits, they receive descriptive error messages suggesting appropriate upgrades.

---

## Shell Escaping Troubleshooting

When using cURL with passwords containing special characters, shell escaping can cause authentication failures.

### Common Issues
- **Exclamation marks** (`!`) get escaped by the shell
- **Dollar signs** (`$`) trigger variable expansion
- **Quotes** within JSON strings cause parsing errors

### Solution: Use Heredoc Syntax
To avoid shell escaping issues, use heredoc syntax for request bodies:

```bash
curl -X POST {BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d @- << 'EOF'
{
  "email": "user@example.com", 
  "password": "SecurePass123!"
}
EOF
```

### Alternative: Store in Temporary Files
```bash
# Store request in temp file to avoid escaping
echo '{"email": "user@example.com", "password": "SecurePass123!"}' > /tmp/login_request.json

curl -X POST {BASE_URL}/auth/login \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -d @/tmp/login_request.json

# Clean up
rm /tmp/login_request.json
```

---

## Security Notes

- **Email Verification Required**: Users cannot access API until email is verified
- **Token Expiration**: Verification tokens expire in 1 hour
- **Password Storage**: Securely hashed with industry-standard algorithms
- **JWT Security**: Tokens with secure signing and expiration