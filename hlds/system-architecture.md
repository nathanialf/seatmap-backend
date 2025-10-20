# Seatmap Backend Service - System Architecture

## Overview

The Seatmap Backend Service is a serverless REST API built on AWS that aggregates flight seat availability data from Amadeus and Sabre APIs. The system is designed for airline employees using free flight benefits, with support for guest users, OAuth authentication, and advanced search capabilities.

**Core Technologies**: AWS Lambda (Java 17), API Gateway, DynamoDB, Stripe, CloudWatch

**Security Model**: Secrets managed via Jenkins credentials and injected as environment variables during deployment

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Internet                                   │
└────────────────┬────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     API Gateway (REST)                            │
│  - Rate Limiting                                                  │
│  - API Key Authentication (Apps/Clients)                          │
│  - Request/Response Logging                                       │
└────────────────┬────────────────────────────────────────────────┘
                 │
    ┌────────────┴────────────┐
    │                         │
    ▼                         ▼
┌─────────────────┐    ┌─────────────────┐
│   Auth Lambda   │    │  Business Logic │
│   (JWT/OAuth2)  │    │     Lambdas     │
│   - Email/Pass  │    │  - Flight Search│
│   - Google      │    │  - User Mgmt    │
│   - Apple       │    │  - Subscriptions│
│   - Guest       │    │  - Bookmarks    │
└────────┬────────┘    └────────┬────────┘
         │                      │
         │      ┌───────────────┼───────────────┐
         │      │               │               │
         ▼      ▼               ▼               ▼
    ┌────────────────┐  ┌──────────────┐  ┌──────────────┐
    │   DynamoDB     │  │   Amadeus    │  │    Sabre     │
    │   - Users      │  │     API      │  │     API      │
    │   - Sessions   │  │  (External)  │  │  (External)  │
    │   - Bookmarks  │  └──────────────┘  └──────────────┘
    │   - Cache      │
    └────────────────┘
         │
         ▼
    ┌────────────────┐
    │  Stripe API    │
    │  (Payments)    │
    └────────────────┘
         │
         ▼
    ┌────────────────────────────────────────────┐
    │          CloudWatch Logs & Metrics          │
    │  ┌──────────────┐  ┌──────────────────┐   │
    │  │   Alarms     │  │  Custom Metrics   │   │
    │  └──────┬───────┘  └──────────────────┘   │
    │         │                                    │
    └─────────┼────────────────────────────────────┘
              │
         ┌────┴────┐
         │   SNS   │
         │ (Alerts)│
         └─────────┘
              │
    ┌─────────┴─────────┐
    │                   │
    ▼                   ▼
 [Email]            [Slack/SMS]

```

---

## Infrastructure Components

```
┌─────────────────────────────────────────────────────────────────┐
│                      AWS Account                                  │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                 VPC (Optional for enhanced security)        │ │
│  │                                                             │ │
│  │  ┌──────────────────────────────────────────────────────┐ │ │
│  │  │           Lambda Functions (Java 17)                  │ │ │
│  │  │                                                        │ │ │
│  │  │  Auth Service                                         │ │ │
│  │  │   - Email/Password login                              │ │ │
│  │  │   - Google OAuth 2.0                                  │ │ │
│  │  │   - Apple Sign In                                     │ │ │
│  │  │   - Guest session management                          │ │ │
│  │  │                                                        │ │ │
│  │  │  Flight Service                                       │ │ │
│  │  │   - Search with travel class filter                   │ │ │
│  │  │   - Airline and flight number filters                 │ │ │
│  │  │   - Amadeus + Sabre aggregation                       │ │ │
│  │  │   - Seat map retrieval with guest tracking            │ │ │
│  │  │                                                        │ │ │
│  │  │  User Management Service                              │ │ │
│  │  │  Subscription Service (Stripe)                        │ │ │
│  │  │  Bookmark Service                                     │ │ │
│  │  │  External API Monitor                                 │ │ │
│  │  └──────────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              DynamoDB Tables                                │ │
│  │                                                             │ │
│  │  Users Table                                               │ │
│  │   - PK: userId                                             │ │
│  │   - GSI: email-index                                       │ │
│  │   - GSI: oauth-id-index (for Google/Apple users)          │ │
│  │   - Attributes: email, passwordHash, authProvider,        │ │
│  │                 oauthId, profilePicture                    │ │
│  │                                                             │ │
│  │  Sessions Table                                            │ │
│  │   - PK: sessionId                                          │ │
│  │   - SK: userId (or guest_<uuid>)                           │ │
│  │   - TTL: 24 hours                                          │ │
│  │   - Attributes: userType, guestFlightsViewed              │ │
│  │                                                             │ │
│  │  Bookmarks Table                                           │ │
│  │   - PK: userId                                             │ │
│  │   - SK: flightId#departureDate                             │ │
│  │   - TTL: departureDate + 1 day                             │ │
│  │   - Max 50 bookmarks per user                              │ │
│  │                                                             │ │
│  │  APICache Table                                            │ │
│  │   - PK: cacheKey (includes travelClass, airline, flight#)  │ │
│  │   - TTL: 15 minutes                                        │ │
│  │                                                             │ │
│  │  Subscriptions Table                                       │ │
│  │   - PK: userId                                             │ │
│  │   - Stripe integration data                                │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │                   S3 Buckets                                │ │
│  │  ┌──────────────────┐    ┌──────────────────┐            │ │
│  │  │  Terraform State │    │  Lambda Code     │            │ │
│  │  │  (Versioned)     │    │  Artifacts       │            │ │
│  │  └──────────────────┘    └──────────────────┘            │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │         Jenkins Credentials (Secrets Injection)             │ │
│  │  - Amadeus API Credentials                                 │ │
│  │  - Sabre API Credentials                                   │ │
│  │  - Stripe API Keys                                         │ │
│  │  - JWT Secret Keys                                         │ │
│  │  - Google OAuth Credentials                                │ │
│  │  - Apple Sign In Credentials                               │ │
│  │  - Injected during deployment, NOT stored in Git/Terraform │ │
│  └────────────────────────────────────────────────────────────┘ │
│                                                                   │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │     CloudWatch Logs, Metrics, Alarms, Dashboards          │ │
│  └────────────────┬───────────────────────────────────────────┘ │
│                   │                                               │
└───────────────────┼───────────────────────────────────────────────┘
                    │
                    ▼
              ┌──────────┐
              │   SNS    │
              │  Topics  │
              └──────────┘

```

---

## AWS Services Breakdown

### 1. API Gateway (REST API)

**Purpose**: Entry point for all HTTP requests

**Configuration**:

- REST API (not HTTP API for better feature set)
- API key authentication for client applications
- Usage plans with rate limiting
- Request/response transformation
- CloudWatch logging enabled
- CORS configuration for web clients

**Endpoints**:

- `/auth/*` - Authentication and authorization
- `/flights/*` - Flight search and seat maps
- `/users/*` - User profile management
- `/subscriptions/*` - Subscription management
- `/bookmarks/*` - Flight bookmarks

---

### 2. Lambda Functions (Java 17)

**Runtime Configuration**:

- Java 17 (Amazon Corretto)
- Memory: 512MB-1GB per function
- Timeout: 30 seconds (API), 5 minutes (background)
- Reserved concurrency per environment
- Environment variables injected from Jenkins

**Functions**:

### Auth Service

- Email/password registration and login
- Google OAuth 2.0 integration
- Apple Sign In integration
- Guest session creation
- JWT token generation
- Password reset flow

### Flight Search Service

- Parallel queries to Amadeus and Sabre
- Travel class filtering (economy, premium economy, business, first)
- Optional airline filtering
- Optional flight number exact match
- Result merging and deduplication
- Response caching

### Seat Map Service

- Retrieves seat maps from source API
- Tracks guest user view counts
- Enforces 2-view limit for guests
- Parses and normalizes seat data

### User Management Service

- Profile CRUD operations
- Password changes
- Account deletion

### Subscription Service

- Stripe integration
- Subscription creation/cancellation
- Payment method updates
- Webhook handling for payment events

### Bookmark Service

- Create/read/delete bookmarks
- 50 bookmark limit enforcement
- Auto-expiration after flight date

### External API Monitor

- Scheduled health checks (every 5 min)
- Amadeus availability check
- Sabre availability check
- CloudWatch metric publishing

---

### 3. DynamoDB Tables

### Users Table

```
PK: userId (UUID)
GSI-1: email-index (for email lookup)
GSI-2: oauth-id-index (for Google/Apple lookup)

Attributes:
- email (String)
- passwordHash (String, nullable for OAuth)
- firstName (String)
- lastName (String)
- authProvider (email|google|apple)
- oauthId (String, nullable)
- profilePicture (String, URL)
- createdAt (Timestamp)
- updatedAt (Timestamp)
- status (active|suspended)

```

### Sessions Table

```
PK: sessionId (UUID)
SK: userId (UUID or guest_<UUID>)
TTL: expiresAt (24 hours)

Attributes:
- jwtToken (String)
- userType (user|guest)
- guestFlightsViewed (Number, 0-2 for guests)
- createdAt (Timestamp)
- ipAddress (String)
- userAgent (String)

```

### Bookmarks Table

```
PK: userId (UUID)
SK: bookmarkId (flightId#departureDate)
TTL: departureDate + 1 day

Attributes:
- flightData (Map)
  - flightNumber
  - airline
  - origin
  - destination
  - departureTime
  - arrivalTime
  - price
  - travelClass
  - source
- createdAt (Timestamp)

```

### APICache Table

```
PK: cacheKey (hash of origin, destination, date, travelClass, airline, flightNumber)
TTL: 15 minutes

Attributes:
- responseData (JSON)
- source (String)
- createdAt (Timestamp)

```

### Subscriptions Table

```
PK: userId (UUID)

Attributes:
- stripeCustomerId (String)
- subscriptionId (String)
- status (active|cancelled|past_due)
- currentPeriodStart (Timestamp)
- currentPeriodEnd (Timestamp)
- cancelAtPeriodEnd (Boolean)
- createdAt (Timestamp)
- updatedAt (Timestamp)

```

---

### 4. S3 Buckets

### Terraform State Bucket

- **Purpose**: Store Terraform state files
- **Configuration**:
    - Versioning enabled
    - Encryption: SSE-S3
    - Bucket policy: Jenkins access only
    - Naming: `seatmap-terraform-state-{env}`

### Lambda Artifacts Bucket

- **Purpose**: Store compiled Lambda JAR files
- **Configuration**:
    - Lifecycle policy: 30 days retention
    - Versioning enabled
    - Naming: `seatmap-lambda-artifacts-{env}`

---

### 5. CloudWatch

### Logs

- Lambda function logs (all functions)
- API Gateway access logs
- Retention: 7 days (dev), 30 days (prod)

### Metrics

**Custom Application Metrics**:

- `FlightSearchDuration`
- `ExternalAPILatency` (per provider)
- `CacheHitRate`
- `UserRegistrations`
- `GuestSessionsCreated`
- `GuestSeatMapViewsBlocked`
- `SubscriptionPaymentFailures`
- `BookmarkCreations`

**AWS Service Metrics**:

- Lambda: invocations, errors, duration, throttles
- API Gateway: 4xx, 5xx, latency
- DynamoDB: read/write capacity, throttles

### Alarms

**Critical** (immediate action):

- Lambda Error Rate > 5%
- API Gateway 5xx > 10/min
- Amadeus API Down (3 failures)
- Sabre API Down (3 failures)
- Payment Processing Failures
- DynamoDB Throttling

**Warning** (investigation):

- Lambda Duration > 25s (p99)
- API Gateway Latency > 3s (p95)
- Cache Miss Rate > 80%
- External API Latency > 5s (p95)

### Dashboards

- Real-time request volume
- Error rates by endpoint
- External API health status
- Lambda performance metrics
- Cost tracking
- Active users (registered + guest)
- Subscription metrics

---

### 6. SNS (Simple Notification Service)

**Topics**:

- `seatmap-alerts-dev`
- `seatmap-alerts-prod`

**Subscriptions**:

- Email (initial)
- Slack webhook (future)
- SMS (future)

**Event Types**:

- Critical alarms
- Deployment notifications
- External API failures
- Payment failures

---

## Authentication & Authorization

### Authentication Methods

### 1. Email/Password

- bcrypt password hashing (cost factor 12)
- Email verification required
- Password requirements: min 8 chars, mixed case, number, special char
- Account lockout: 5 failed attempts, 15 min lockout

### 2. Google OAuth 2.0

- Google Sign-In button on frontend
- OAuth 2.0 authorization code flow
- Scope: `email`, `profile`
- Auto-create account on first login
- Link to existing account if email matches

### 3. Apple Sign In

- Apple Sign In button on frontend
- REST API integration
- Scope: `email`, `name`
- Handle anonymized emails
- Auto-create account on first login

### 4. Guest Access

- No registration required
- "Continue as Guest" button
- Limited to 2 seat map views
- 24-hour session expiration
- No bookmarks or subscriptions
- Prompt to register after limit reached

### JWT Token Structure

**Registered User**:

```json
{
  "sub": "userId",
  "email": "user@example.com",
  "role": "user",
  "provider": "email|google|apple",
  "iat": 1234567890,
  "exp": 1234654290
}

```

**Guest User**:

```json
{
  "sub": "guest_<uuid>",
  "role": "guest",
  "provider": "guest",
  "guestLimits": {
    "flightsViewed": 0,
    "maxFlights": 2
  },
  "iat": 1234567890,
  "exp": 1234567890
}

```

### Authorization Rules

| Feature | Guest | Registered | Subscriber |
| --- | --- | --- | --- |
| Flight Search | ✅ | ✅ | ✅ |
| Seat Map View | ✅ (2 max) | ✅ | ✅ |
| Bookmarks | ❌ | ✅ | ✅ |
| Advanced Filters | ✅ | ✅ | ✅ |
| Subscription | ❌ | ✅ | ✅ |

---

## External Service Integration

### Amadeus API

**Base URL**: `https://api.amadeus.com/v2`

**Authentication**: OAuth2 Client Credentials

- Client ID and Secret stored in Jenkins
- Token cached and refreshed automatically

**Rate Limits**: 10 transactions/second (self-service tier)

**Key Endpoints**:

- `POST /shopping/flight-offers` - Search flights
- `GET /shopping/seatmaps` - Get seat maps
- `GET /reference-data/locations` - Airport search

**Request Parameters**:

- `originLocationCode`
- `destinationLocationCode`
- `departureDate`
- `travelClass` (ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST)
- `currencyCode`
- `adults` (fixed at 1)
- `includedAirlineCodes` (optional)
- `flightNumber` (optional)

---

### Sabre API

**Base URL**: `https://api.sabre.com/v1`

**Authentication**: Token-based (SOAP/REST)

- Credentials stored in Jenkins
- Token refreshed automatically

**Key Endpoints**:

- Seat Map Display
- Flight Search

**Note**: Rate limits and detailed integration to be investigated during implementation

---

### Stripe API

**Purpose**: Subscription billing ($5/month)

**Integration Points**:

- Customer creation
- Subscription management
- Payment method updates
- Webhook event handling

**Webhook Events**:

- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`
- `invoice.payment_failed`
- `invoice.payment_succeeded`

**Payment Failure Flow**:

1. `invoice.payment_failed` webhook received
2. Update subscription status to `past_due`
3. Send notification email
4. Immediately cancel subscription
5. Revoke access

---

### Google OAuth 2.0

**Endpoints**:

- Authorization: `https://accounts.google.com/o/oauth2/v2/auth`
- Token: `https://oauth2.googleapis.com/token`
- User Info: `https://www.googleapis.com/oauth2/v2/userinfo`

**Credentials**: Stored in Jenkins

- Client ID
- Client Secret

**Scopes**: `email`, `profile`

---

### Apple Sign In

**Endpoints**:

- Authorization: `https://appleid.apple.com/auth/authorize`
- Token: `https://appleid.apple.com/auth/token`

**Credentials**: Stored in Jenkins

- Client ID
- Team ID
- Key ID
- Private Key (.p8 file)

**Scopes**: `email`, `name`

---

## Security Architecture

### Security Layers

### 1. Network Security

- API Gateway with AWS WAF (future)
- Lambda in VPC (if needed)
- Security groups and NACLs

### 2. Authentication & Authorization

- Multi-factor authentication (future)
- API keys for client applications
- JWT tokens for user sessions
- OAuth 2.0 for Google/Apple
- Guest session limiting

### 3. Data Security

- Encryption at rest: DynamoDB, S3
- Encryption in transit: TLS 1.2+
- Secrets in Jenkins (NOT Git/Terraform)
- Password hashing: bcrypt
- Lambda env vars encrypted by AWS

### 4. API Security

- Rate limiting per API key
- Input validation on all endpoints
- CORS configuration
- Request signing for external APIs

### 5. Secret Management

- All secrets in Jenkins credential store
- Injected as Lambda environment variables
- Never committed to Git
- Never managed by Terraform
- Automatic injection during deployment

---

## Deployment Architecture

### CI/CD Pipeline

```
GitHub → Jenkins → Terraform → Lambda Deployment → Testing → Production

```

**Pipeline Stages**:

1. Checkout code
2. Load Jenkins credentials
3. Run unit tests
4. Build Lambda JARs
5. Terraform init/plan/apply
6. Deploy Lambdas with secret injection
7. Integration tests
8. E2E tests
9. User approval (for prod)
10. Deployment complete

### Environment Strategy

**Development**:

- Lower resource limits
- Verbose logging
- Cost-optimized
- Separate Jenkins credentials

**Production**:

- Production-grade resources
- Structured logging only
- High availability
- Separate Jenkins credentials

---

## Monitoring Strategy

### Health Checks

- External API monitor runs every 5 minutes
- Checks Amadeus and Sabre availability
- Publishes custom CloudWatch metrics
- Alerts on 3 consecutive failures

### Dashboards

- Real-time system health
- API performance metrics
- External dependency status
- Cost tracking
- User activity metrics

### Alerting

- Email notifications (initial)
- SNS topic per environment
- Future: Slack/SMS integration

---

## Scalability Considerations

### Horizontal Scaling

- Lambda auto-scales with traffic
- DynamoDB on-demand capacity
- API Gateway handles bursts

### Performance Optimization

- 15-minute cache for flight searches
- 5-minute cache for seat maps
- Parallel API calls (Amadeus + Sabre)
- Connection pooling for external APIs

### Cost Optimization

- Short Lambda timeouts
- Efficient DynamoDB access patterns
- Cache hit rate optimization
- Reserved concurrency where needed