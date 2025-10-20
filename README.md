# Seatmap Backend Service

A serverless REST API built on AWS that aggregates flight seat availability data from Amadeus and Sabre APIs. Designed for airline employees using free flight benefits, with support for guest users, OAuth authentication, and advanced search capabilities.

## Architecture Overview

**Core Technologies**: AWS Lambda (Java 17), API Gateway, DynamoDB, Stripe, CloudWatch  
**Build System**: Gradle 8.4 with fat JAR packaging  
**Infrastructure**: Terraform with Jenkins CI/CD pipeline  
**Security**: JWT tokens, bcrypt password hashing, API key authentication

## Current Progress

### ✅ **Completed (Phase 1 MVP - 75%)**

#### **Infrastructure & DevOps**
- ✅ **Terraform Infrastructure**: Complete AWS infrastructure as code
  - DynamoDB tables with proper indexes and TTL
  - S3 backend for state management with locking
  - Environment separation (dev/prod)
- ✅ **Jenkins CI/CD Pipeline**: 4-action deployment pipeline
  - `bootstrap`: Creates S3 + DynamoDB for Terraform state
  - `plan`: Builds application + shows infrastructure changes
  - `apply`: Deploys application and infrastructure  
  - `destroy`: Removes all resources
- ✅ **Build System**: Gradle 8.4 with Java 17
  - Fat JAR packaging for Lambda deployment
  - Comprehensive test suite with JUnit 5
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

### 🔄 **In Progress**

#### **API Layer (Phase 2)**
- 🔄 **Lambda Handlers**: HTTP request processing
- 🔄 **API Gateway**: REST endpoints with rate limiting

#### **Flight Search Integration (Phase 3)**  
- 🔄 **Amadeus API**: Flight search and seat map retrieval
- 🔄 **External API Service**: Caching and error handling

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

```bash
cd terraform

# Initialize
terraform init \
  -backend-config="bucket=seatmap-backend-terraform-state-dev" \
  -backend-config="key=seatmap-backend/terraform.tfstate" \
  -backend-config="region=us-west-1" \
  -backend-config="dynamodb_table=seatmap-backend-terraform-locks-dev"

# Plan
terraform plan -var="environment=dev" -var="aws_region=us-west-1"

# Apply
terraform apply -var="environment=dev" -var="aws_region=us-west-1"
```

## Testing

### Test Coverage
- **Authentication Services**: 42 comprehensive tests
- **Password Security**: bcrypt validation, strength requirements
- **JWT Tokens**: Generation, validation, expiration handling
- **User Management**: Registration, login, session management

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

### Planned Endpoints
- `POST /flights/search` - Search flights across Amadeus and Sabre
- `GET /flights/{flightId}/seatmap` - Get seat map (with guest limits)
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

**Status**: Phase 1 MVP (75% complete) - Authentication system ready for production deployment