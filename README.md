# Seatmap Backend Service

A serverless REST API built on AWS that aggregates flight seat availability data from Amadeus and Sabre APIs. Designed for airline employees using free flight benefits, with support for guest users, OAuth authentication, and advanced search capabilities.

## Architecture Overview

**Core Technologies**: AWS Lambda (Java 17), API Gateway, DynamoDB, Stripe, CloudWatch  
**Build System**: Gradle 8.4 with fat JAR packaging  
**Infrastructure**: Terraform with Jenkins CI/CD pipeline  
**Security**: JWT tokens, bcrypt password hashing, API key authentication

## Current Progress

### ✅ **Completed (Phase 1 MVP - 95%)**

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
  - Comprehensive test suite with JUnit 5 (90 tests)
  - Jakarta validation for request validation

#### **Authentication System**
- ✅ **User Management**: Complete user lifecycle
  - Email/password registration with bcrypt hashing
  - JWT token generation and validation
  - Session management with 24-hour expiration
- ✅ **Guest Access**: Limited guest functionality
  - 2 seat map views before registration required
  - Automatic session expiration
- ✅ **Data Layer**: DynamoDB integration
  - User repository with email/OAuth lookups
  - Session repository with TTL management
  - Comprehensive test coverage (42 tests)

#### **Amadeus Seat Map API**
- ✅ **API Integration**: Complete Amadeus seat map service
  - OAuth2 token management with auto-refresh
  - Seat map retrieval with flight validation
  - Error handling and network resilience
- ✅ **Lambda Handler**: HTTP request processing
  - JWT authentication with guest limits
  - Request validation (flight number, dates, airports)
  - CORS support for web frontend
- ✅ **API Gateway**: REST endpoint configuration
  - POST /seat-map endpoint with OPTIONS CORS
  - Proper integration with Lambda functions
- ✅ **Test Coverage**: 48 comprehensive tests
  - AmadeusService: API integration and token management
  - SeatMapHandler: Request processing and validation
  - SeatMapRequest/Response: Data models and validation

### 🔄 **In Progress**

#### **API Layer (Phase 2)**
- 🔄 **Additional Endpoints**: User profile, bookmarks management

### 📅 **Planned Features**
- Google OAuth 2.0 and Apple Sign In
- Stripe subscription management ($5/month)
- Flight bookmarks (50 max per user)
- Advanced monitoring and alerting
- Sabre API integration

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
│  │   - GSI: email-index, oauth-id-index                      │ │
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

### Test Coverage (90 tests total)
- **Authentication Services**: 42 comprehensive tests
  - Password Security: bcrypt validation, strength requirements
  - JWT Tokens: Generation, validation, expiration handling
  - User Management: Registration, login, session management
- **Amadeus Seat Map API**: 48 comprehensive tests
  - AmadeusService: OAuth2 integration, error handling, token management
  - SeatMapHandler: Lambda request processing, JWT validation, CORS
  - SeatMapRequest: Jakarta validation for flight data
  - SeatMapResponse: Response model handling and JSON preservation

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

### Planned Endpoints
- `POST /flights/search` - Search flights across Amadeus and Sabre
- `POST /bookmarks` - Save flight bookmark
- `POST /subscriptions/subscribe` - Create Stripe subscription

## Security Features

### Implemented
- ✅ **Password Security**: bcrypt with cost factor 12
- ✅ **JWT Tokens**: 24-hour expiration, secure generation
- ✅ **Guest Limits**: 2 seat map views before registration required
- ✅ **Session Management**: Automatic expiration with TTL

### Planned
- API key authentication for client applications
- Rate limiting per endpoint
- OAuth 2.0 integration (Google, Apple)
- Input validation on all endpoints

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

**Status**: Phase 1 MVP (95% complete) - Authentication system and Amadeus seat map API ready for production deployment