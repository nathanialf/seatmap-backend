# Seatmap Backend Service

A serverless REST API built on AWS that aggregates flight seat availability data from Amadeus and Sabre APIs. Designed for airline employees using free flight benefits, with support for guest users, OAuth authentication, and advanced search capabilities.

## Architecture Overview

**Core Technologies**: AWS Lambda (Java 17), API Gateway, DynamoDB, Stripe, CloudWatch  
**Build System**: Gradle 8.4 with fat JAR packaging  
**Infrastructure**: Terraform with Jenkins CI/CD pipeline  
**Security**: JWT tokens, bcrypt password hashing, API key authentication

## Current Progress

### ✅ **Completed (Phase 1 MVP - 100%)**

#### **Infrastructure & DevOps**
- ✅ **Terraform Infrastructure**: Complete AWS infrastructure as code
  - Environment-specific directories (`terraform/environments/dev`, `terraform/environments/prod`)
  - DynamoDB tables with proper indexes and TTL
  - Lambda functions with API Gateway integration
  - S3 backend for state management with locking
  - Hardcoded backend configuration per environment
- ✅ **Jenkins CI/CD Pipeline**: 4-action deployment pipeline
  - `bootstrap`: Creates S3 + DynamoDB for Terraform state
  - `plan`: Builds application + shows infrastructure changes
  - `apply`: Deploys application and infrastructure  
  - `destroy`: Removes all resources
  - Amadeus API credentials integration
- ✅ **Build System**: Gradle 8.4 with Java 17
  - Fat JAR packaging for Lambda deployment
  - Comprehensive test suite with JUnit 5 (277 tests, 70% coverage)
  - Jakarta validation for request validation

#### **Authentication System**
- ✅ **User Management**: Complete user lifecycle
  - Email/password registration with bcrypt hashing
  - Comprehensive email verification system with 1-hour token expiration
  - JWT token generation and validation (24-hour expiration)
  - Session management with automatic cleanup
  - Account tier system (FREE, PRO, BUSINESS) with usage limits
  - Business tier upgrade restriction (no downgrades allowed)
  - Real-world tested with personal email verification
- ✅ **Guest Access**: IP-based rate limiting
  - 2 seat map views per IP before registration required
  - Real-time IP extraction from X-Forwarded-For headers
  - Fixed guest access tracking for successful retrievals only
- ✅ **Data Layer**: DynamoDB integration
  - User repository with email/OAuth/verification token lookups
  - Guest access history with TTL management
  - Fixed Jackson serialization with @JsonIgnore annotations
  - Comprehensive test coverage (100+ tests)
- ✅ **Email Verification System**: Production-ready AWS SES integration
  - Automated verification email sending
  - Verification token index (GSI) for efficient token lookups
  - 1-hour token expiration with cleanup
  - GET endpoint support for email verification links
  - SES email identity verification in Terraform
  - Real-world tested end-to-end verification flow

#### **Flight Search & Seat Map APIs**
- ✅ **Multi-Source Integration**: Amadeus + Sabre unified search
  - OAuth2 token management with auto-refresh for Amadeus
  - SOAP API integration for Sabre flight schedules
  - Intelligent flight meshing with source prioritization
  - Concurrent API calls for optimal performance
- ✅ **Lambda Handlers**: HTTP request processing
  - JWT authentication with IP-based guest rate limiting
  - Request validation (flight number, dates, airports)
  - CORS support for web frontend
  - Source routing based on flight data origin
- ✅ **API Gateway**: REST endpoint configuration
  - POST /seat-map endpoint for seat availability
  - POST /flights/search endpoint for flight discovery
  - OPTIONS CORS with proper integration
- ✅ **Test Coverage**: 64 comprehensive tests
  - FlightOffersHandler: 19 tests (concurrent calls, meshing, error handling)
  - SeatMapHandler: 38 tests (authentication, routing, rate limiting)
  - AmadeusService: 7 tests (API integration, token management)

### ✅ **Completed (Phase 2 - 100%)**

#### **API Layer**
- ✅ **User Profile Management**: View and update profile endpoints
- ✅ **Bookmarks Management**: Complete CRUD API for flight bookmarks
  - Create, read, update, delete operations with user isolation
  - 50 bookmark limit per user with TTL expiration (30 days)
  - Source field enhancement for clean API architecture
  - Full integration with seat map API for seamless workflow
  - End-to-end testing validated with real API calls

### 📅 **Planned Features (Phase 3)**
- **Sabre Implementation**: Fix integration issues due to external API factors
- **Google OAuth 2.0 and Apple Sign In**: Social authentication integration
- **Stripe subscription management ($5/month)**: Payment processing and recurring billing
- **Advanced monitoring and alerting**: CloudWatch alarms, custom metrics, SNS notifications

## Infrastructure Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     AWS Account (us-west-1)                      │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │           Lambda Functions (Java 17)                        │ │
│  │                                                             │ │
│  │  Auth Service                                               │ │
│  │   - Email/Password + JWT                                    │ │
│  │   - Guest session management                               │ │
│  │                                                             │ │
│  │  Flight Service (Planned)                                  │ │
│  │   - Amadeus + Sabre aggregation                           │ │
│  │   - Seat map retrieval                                     │ │
│  │                                                             │ │
│  │  User Management Service (Planned)                         │ │
│  │  Subscription Service (Planned)                            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              DynamoDB Tables                                │ │
│  │                                                             │ │
│  │  Users Table                                                │ │
│  │   - PK: userId                                             │ │
│  │   - GSI: email-index, oauth-id-index, verification-token-index │ │
│  │                                                             │ │
│  │  Sessions Table                                             │ │
│  │   - PK: sessionId, SK: userId                             │ │
│  │   - TTL: 24 hours                                          │ │
│  │                                                             │ │
│  │  Bookmarks, APICache, Subscriptions (Ready)               │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   S3 Buckets                                │ │
│  │  Terraform State + Lambda Artifacts                        │ │
│  └────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
```

## Development Setup

### Prerequisites
- Java 17
- AWS CLI configured
- Terraform
- Jenkins with AWS credentials

### Local Development

```bash
# Clone repository
git clone <repository-url>
cd seatmap-backend

# Run tests
./gradlew clean test

# Build Lambda JAR
./gradlew buildLambda

# View test results
open build/reports/tests/test/index.html
```

### Environment Variables (for testing)
```bash
export JWT_SECRET="your-jwt-secret-key-at-least-32-characters-long"
export AMADEUS_API_KEY="your-amadeus-api-key"
export AMADEUS_API_SECRET="your-amadeus-api-secret"
export AMADEUS_ENDPOINT="test.api.amadeus.com"
```

## Deployment

### Jenkins Pipeline

The deployment is managed through Jenkins with these actions:

#### **Bootstrap Environment**
```bash
# Jenkins parameters: ACTION=bootstrap, ENVIRONMENT=dev
# Creates S3 bucket and DynamoDB table for Terraform state
```

#### **Plan Deployment**
```bash
# Jenkins parameters: ACTION=plan, ENVIRONMENT=dev
# 1. Builds and tests application
# 2. Shows infrastructure changes
# 3. Validates deployment readiness
```

#### **Deploy to Environment**
```bash
# Jenkins parameters: ACTION=apply, ENVIRONMENT=dev
# 1. Builds and tests application  
# 2. Plans infrastructure changes
# 3. Deploys Lambda functions and infrastructure
```

#### **Destroy Environment**
```bash
# Jenkins parameters: ACTION=destroy, ENVIRONMENT=dev
# Removes all resources (use with caution!)
```

### Manual Terraform (Alternative)

**With the new environment-specific structure, manual Terraform is much simpler:**

```bash
# For dev environment
cd terraform/environments/dev

# Initialize (backend config is hardcoded)
terraform init

# Plan (only need API secrets)
terraform plan \
  -var="amadeus_api_key=YOUR_AMADEUS_KEY" \
  -var="amadeus_api_secret=YOUR_AMADEUS_SECRET" \
  -var="jwt_secret=YOUR_JWT_SECRET"

# Apply
terraform apply \
  -var="amadeus_api_key=YOUR_AMADEUS_KEY" \
  -var="amadeus_api_secret=YOUR_AMADEUS_SECRET" \
  -var="jwt_secret=YOUR_JWT_SECRET"

# For prod environment
cd terraform/environments/prod
# Same commands, different environment
```

## Testing

### Test Coverage (277 tests total - 70% instruction coverage)
- **API Handlers**: 57 comprehensive tests (93% coverage)
  - SeatMapHandler: 38 tests - JWT validation, guest rate limiting, Sabre/Amadeus routing
  - FlightOffersHandler: 19 tests - concurrent API calls, error handling, flight meshing
- **Authentication Services**: 100 comprehensive tests (94% coverage)
  - Password Security: bcrypt validation, strength requirements  
  - JWT Tokens: Generation, validation, expiration, security edge cases
  - User Management: Registration, login, session management
  - Guest Access: IP-based rate limiting (2 seat map views max)
- **API Services**: 7 tests (35% coverage)
  - AmadeusService: OAuth2 integration, error handling, token management
- **Data Layer**: 42 comprehensive tests (40-78% coverage)
  - DynamoDB repositories with serialization testing
  - User and guest access models with JSON validation
  - Rate limiting integration tests

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "PasswordServiceTest"

# Run with detailed output
./gradlew testDetailed
```

## API Documentation

### Authentication Endpoints (Implemented)

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "StrongPass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Login User
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com", 
  "password": "StrongPass123!"
}
```

#### Create Guest Session
```http
POST /auth/guest
```

#### Verify Email
```http
GET /auth/verify?token=<verification-token>
```

**Response (Success):**
```json
{
  "success": true,
  "message": "Email verified successfully",
  "data": {
    "token": "jwt-token-here",
    "expiresAt": 1735142400
  }
}
```

#### Resend Verification Email
```http
POST /auth/resend-verification
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Get User Profile
```http
GET /auth/profile
Authorization: Bearer <jwt-token>
```

**Response (Success):**
```json
{
  "userId": "45852541-1ca9-48c4-aab2-8669d772a2d2",
  "email": "nathanial@defnf.com",
  "firstName": "Nathanial",
  "lastName": "Fine",
  "authProvider": "EMAIL",
  "oauthId": null,
  "createdAt": 1761416568.884190800,
  "updatedAt": 1761416577.363132500,
  "status": "ACTIVE",
  "emailVerified": true,
  "fullName": "Nathanial Fine"
}
```

#### Update User Profile
```http
PUT /auth/profile
Authorization: Bearer <jwt-token>
Content-Type: application/json
```

**Supports partial updates - send any combination of these fields:**

**Update first name only:**
```json
{
  "firstName": "Nathan"
}
```

**Update last name only:**
```json
{
  "lastName": "Smith"
}
```

**Update both fields:**
```json
{
  "firstName": "Nathan",
  "lastName": "Smith"
}
```

**Response (Success):**
```json
{
  "userId": "45852541-1ca9-48c4-aab2-8669d772a2d2",
  "email": "nathanial@defnf.com",
  "firstName": "Nathan",
  "lastName": "Smith",
  "authProvider": "EMAIL",
  "oauthId": null,
  "createdAt": 1761416568.884190800,
  "updatedAt": 1761416577.363132500,
  "status": "ACTIVE",
  "emailVerified": true,
  "fullName": "Nathan Smith"
}
```

**Field Validation:**
- `firstName`: Max 50 characters
- `lastName`: Max 50 characters  
- Guest tokens are rejected - requires user JWT token

#### Get Seat Map
```http
POST /seat-map
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "flightNumber": "AA123",
  "departureDate": "2024-12-01",
  "origin": "LAX",
  "destination": "JFK"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Seat map retrieved successfully",
  "data": {
    "data": [
      {
        "type": "seat-map",
        "flightNumber": "AA123",
        "aircraft": { "code": "32A" },
        "decks": [
          {
            "deckType": "MAIN",
            "seats": [
              {
                "number": "1A",
                "characteristicsCodes": ["A", "W"],
                "travelerPricing": []
              }
            ]
          }
        ]
      }
    ]
  },
  "flightNumber": "AA123",
  "departureDate": "2024-12-01",
  "origin": "LAX",
  "destination": "JFK"
}
```

#### Search Flights (Implemented)
```http
POST /flight-offers
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-10-27",
  "travelClass": "PREMIUM_ECONOMY",
  "flightNumber": "UA",
  "maxResults": 5
}
```

**Field Descriptions:**
- `origin` (required): 3-letter airport code
- `destination` (required): 3-letter airport code  
- `departureDate` (required): Date in YYYY-MM-DD format
- `travelClass` (optional): Minimum cabin quality - ECONOMY, PREMIUM_ECONOMY, BUSINESS, or FIRST. If not specified, searches all travel classes
- `flightNumber` (optional): Airline code for filtering (e.g., "UA", "AA")
- `maxResults` (optional): Number of results (default: 10)
```

### Bookmarks API

#### Create Bookmark
```http
POST /bookmarks
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "title": "NYC to LAX - Dec 15",
  "source": "AMADEUS",
  "flightOfferData": "{\"id\":\"amadeus_123\",\"source\":\"GDS\",\"type\":\"flight-offer\",\"itineraries\":[{\"duration\":\"PT6H5M\",\"segments\":[{\"departure\":{\"iataCode\":\"JFK\",\"at\":\"2024-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"LAX\",\"at\":\"2024-12-15T11:05:00\"},\"carrierCode\":\"AA\",\"number\":\"123\"}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\"}}"
}
```

**Response (Success):**
```json
{
  "userId": "user-123",
  "bookmarkId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "NYC to LAX - Dec 15",
  "source": "AMADEUS",
  "flightOfferData": "{\"id\":\"amadeus_123\",...}",
  "createdAt": 1703520000.000,
  "updatedAt": 1703520000.000,
  "expiresAt": 1706112000.000,
  "isExpired": false
}
```

#### List User Bookmarks
```http
GET /bookmarks
Authorization: Bearer <jwt-token>
```

**Response (Success):**
```json
{
  "bookmarks": [
    {
      "userId": "user-123",
      "bookmarkId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "NYC to LAX - Dec 15",
      "source": "AMADEUS",
      "flightOfferData": "{\"id\":\"amadeus_123\",...}",
      "createdAt": 1703520000.000,
      "updatedAt": 1703520000.000,
      "expiresAt": 1706112000.000,
      "isExpired": false
    }
  ],
  "total": 1,
  "maxAllowed": 50
}
```

#### Get Specific Bookmark
```http
GET /bookmarks/{bookmarkId}
Authorization: Bearer <jwt-token>
```

**Response (Success):**
```json
{
  "userId": "user-123",
  "bookmarkId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "NYC to LAX - Dec 15",
  "source": "AMADEUS",
  "flightOfferData": "{\"id\":\"amadeus_123\",...}",
  "createdAt": 1703520000.000,
  "updatedAt": 1703520000.000,
  "expiresAt": 1706112000.000,
  "isExpired": false
}
```

#### Delete Bookmark
```http
DELETE /bookmarks/{bookmarkId}
Authorization: Bearer <jwt-token>
```

**Response (Success):**
```json
{
  "message": "Bookmark deleted successfully"
}
```

**Key Features:**
- **50 bookmark limit** per user with validation
- **TTL expiration** (30 days default) with automatic cleanup
- **Source tracking** (AMADEUS/SABRE) for proper seat map routing
- **Complete flight data storage** for seamless seat map integration
- **User isolation** - users can only access their own bookmarks

### Planned Endpoints
- `POST /subscriptions/subscribe` - Create Stripe subscription

## Security Features

### Implemented
- ✅ **Password Security**: bcrypt with cost factor 12
- ✅ **JWT Tokens**: 24-hour expiration, secure generation, signature validation
- ✅ **Email Verification**: Mandatory verification with 1-hour token expiration
  - Production-ready AWS SES integration
  - Secure verification token generation and validation
  - GET endpoint support for verification links
  - Automatic token cleanup after expiration
- ✅ **IP-Based Rate Limiting**: 2 seat map views per IP before registration required
- ✅ **Session Management**: Automatic expiration with TTL
- ✅ **Input Validation**: Jakarta validation on all request models
- ✅ **CORS**: Proper cross-origin support for web clients

### Planned
- API key authentication for client applications  
- OAuth 2.0 integration (Google, Apple)
- Advanced rate limiting per endpoint and user

## Monitoring & Operations

### Current
- ✅ **Build Validation**: Tests run on every deployment
- ✅ **Infrastructure as Code**: Complete Terraform management
- ✅ **Environment Separation**: dev/prod isolation

### Planned
- CloudWatch alarms for Lambda errors and API Gateway 5xx
- Custom metrics for cache hit rates and external API latency
- SNS notifications for critical alerts

## Contributing

1. **Development**: Follow test-driven development with comprehensive coverage
2. **Deployment**: Use Jenkins pipeline for all environment changes
3. **Testing**: Ensure all tests pass before creating pull requests
4. **Security**: Never commit credentials or secrets to repository

## Support

- **Infrastructure Issues**: Check Jenkins pipeline logs
- **Application Issues**: Review CloudWatch logs (when deployed)
- **Build Issues**: Run `./gradlew clean test` locally

---

**Status**: Phase 1 MVP (100% complete) - Authentication system with email verification and Amadeus seat map API ready for production deployment