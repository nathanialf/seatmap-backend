# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Product Overview

**Serverless REST API aggregating flight seat availability data from Amadeus and Sabre APIs. Designed for airline employees using free flight benefits, featuring OAuth authentication and advanced search capabilities.**

### Target Users
- **Airline Employees** using free flight benefits (standby/non-revenue travel)
- Need real-time seat availability intelligence before committing to flights
- Want to compare seat availability across multiple data sources

### Core Value Proposition
- **Multi-source aggregation**: Combines Amadeus and Sabre data for comprehensive coverage
- **Seat availability focus**: Real-time seat maps and availability data
- **Operational flight data**: Actual running flights, gates, and operational status
- **Advanced search**: Flight discovery optimized for employee travel patterns

### User Journey
1. **Flight Discovery**: Search for active flights by airport/route
2. **Availability Analysis**: View detailed seat maps with real-time availability
3. **Multi-source Comparison**: Compare seat availability across Amadeus and Sabre
4. **Informed Decision**: Choose flights with best standby/seat availability chances

## Development Commands

### Build and Test
```bash
# Run all tests with coverage report
./gradlew clean test jacocoTestReport

# Run tests with detailed output
./gradlew testDetailed

# Build Lambda JAR for deployment
./gradlew buildLambda
```

**Important**: Terraform expects the JAR file to be named `SEATMAP-Backend-1.0.0.jar` in `build/libs/`. 

**For Terraform validation/testing:**
- **Configuration-only changes**: Create dummy JAR with `mkdir -p build/libs && touch build/libs/SEATMAP-Backend-1.0.0.jar` to validate syntax
- **Full deployment testing**: Build actual JAR first with `./gradlew buildLambda`, then run `terraform plan`
- **Local testing**: You may need to rename the generated JAR to match the expected filename

### API Testing with curl
**SECURITY & CONSISTENCY REQUIREMENT**: All API endpoints, credentials, and request bodies must be stored in temporary files and read from those files. This prevents credential exposure and ensures consistency against hallucinations.

**Pattern for API testing:**
```bash
# Store endpoint URL in temp file
echo "https://your-api-gateway-url/endpoint" > /tmp/api_endpoint

# Store credentials in temp file
echo "your-jwt-token-here" > /tmp/jwt_token

# Store request body in temp file (refer to docs/ for API request formats)
echo '{"key": "value"}' > /tmp/request_body.json

# Execute curl command reading from temp files
curl -X POST $(cat /tmp/api_endpoint) \
     -H "Authorization: Bearer $(cat /tmp/jwt_token)" \
     -H "Content-Type: application/json" \
     -d @/tmp/request_body.json

# Always cleanup temporary files when done
rm -f /tmp/api_endpoint /tmp/jwt_token /tmp/request_body.json
```

**Note**: Some requests may require JSON strings within the request body. Refer to the API documentation for specific request formats and examples.

### Development Guidelines

**Implementation Planning Requirement:**
- **MANDATORY**: For every feature request or code change, you MUST first:
  1. **Ask clarifying questions** to fully understand requirements, edge cases, and user expectations
  2. **Analyze the existing codebase** thoroughly using search tools to understand current patterns and architecture
  3. **Create a detailed implementation plan** using the TodoWrite tool that includes:
     - Specific files that need to be modified or created
     - Order of implementation steps
     - Test coverage requirements
     - Documentation updates needed
     - Integration points with existing code
     - Any assumptions or design decisions
  4. **Present this plan to the user for approval** before beginning implementation

**Code Changes Requirements:**
- All code changes must include appropriate test coverage additions
- Update documentation in `docs/` directory if API changes are made
- Follow existing patterns and conventions in the codebase
- Ensure tests pass: `./gradlew test`

### Commit Requirements
**CRITICAL**: Before making any commits, ensure ALL of the following pass:

1. **Build Success**: `./gradlew clean build` must complete without errors
2. **Test Success**: `./gradlew test` must pass with no test failures
3. **Terraform Validation**: If infrastructure changes are made:
   - Run `terraform plan` in appropriate environment directory
   - Validate that only expected resources are being changed
   - Ensure plan completes without errors

**Commit Message Guidelines:**
- Write clear, descriptive commit messages
- Focus on what and why, not how
- **❌ NEVER include Claude authoring or AI generation references in commit messages**
  - ❌ Don't include: "Generated with Claude Code", "Co-Authored-By: Claude", "🤖" emojis, "AI-generated", etc.
  - ✅ Write natural commit messages as if authored by a human developer
- Use conventional commit format when appropriate

### AWS Profile Configuration
**CRITICAL**: Always use the appropriate AWS profiles for environment-specific operations:

- **Development**: Use `seatmap-dev` profile for all dev environment operations
- **Production**: Use `seatmap-prod` profile for all prod environment operations

**Examples:**
```bash
# AWS CLI commands (MUST include --region for all operations)
aws s3 ls --profile seatmap-dev --region us-west-1
aws s3 ls --profile seatmap-prod --region us-west-1

# CloudWatch logs access
aws logs describe-log-groups --profile seatmap-dev --region us-west-1
aws logs get-log-events --log-group-name "/aws/lambda/seatmap-flight-offers-dev" --profile seatmap-dev --region us-west-1

# Terraform commands (use environment variables - ALWAYS use this exact format)
export AWS_PROFILE=seatmap-dev
terraform plan -var="amadeus_api_key=dummy" -var="amadeus_api_secret=dummy" -var="jwt_secret=dummy" -var="sabre_user_id=dummy" -var="sabre_password=dummy"

export AWS_PROFILE=seatmap-prod
terraform plan -var="amadeus_api_key=dummy" -var="amadeus_api_secret=dummy" -var="jwt_secret=dummy" -var="sabre_user_id=dummy" -var="sabre_password=dummy"
```

**IMPORTANT**: All AWS CLI commands MUST include `--region us-west-1` parameter. The AWS CLI will fail without explicit region specification.

### Deployment - JENKINS ONLY
**CRITICAL**: All infrastructure changes must be done through Jenkins pipeline. Never apply Terraform directly via CLI.

Jenkins pipeline parameters:
- **ENVIRONMENT**: `dev` or `prod`
- **ACTION**: `bootstrap`, `plan`, `apply`, or `destroy`

Required Terraform variables (managed by Jenkins credentials):
- `amadeus_api_key` (sensitive)
- `amadeus_api_secret` (sensitive) 
- `jwt_secret` (sensitive)
- `sabre_user_id` (sensitive)
- `sabre_password` (sensitive)

## Architecture Overview

### Core Structure
- **AWS Lambda Functions**: Java 17 serverless functions with API Gateway integration
- **DynamoDB**: NoSQL database with GSI indexes for efficient queries
- **External APIs**: Amadeus (REST/OAuth2) and Sabre (SOAP) flight data aggregation
- **Infrastructure**: Terraform with environment-specific configurations (dev/prod)

### Key Components

#### Lambda Handlers
- `FlightSearchHandler`: Integrated flight search with embedded seat map data from multiple APIs
- `SeatmapViewHandler`: Usage tracking for seat map views with IP-based guest limits
- `AuthHandler`: User authentication, registration, email verification, profile management
- `BookmarkHandler`: Flight bookmark CRUD operations
- `TierHandler`: Account tier management and pricing information

#### Authentication Flow
1. **User Registration**: Email/password with bcrypt hashing (cost factor 12)
2. **Email Verification**: AWS SES integration with 1-hour token expiration
3. **JWT Tokens**: 24-hour expiration with secure validation
4. **Guest Access**: IP-based rate limiting (2 seat map views per IP)

#### External API Integration
- **Amadeus Service**: OAuth2 token management with auto-refresh, REST API calls
- **Sabre Service**: SOAP integration for flight schedules
- **Flight Meshing**: Intelligent combination of results from multiple sources

### Repository Patterns
All repositories extend `DynamoDbRepository<T>` base class:
- `UserRepository`: Email, OAuth, and verification token GSI lookups
- `SessionRepository`: JWT session management with TTL
- `BookmarkRepository`: User-isolated bookmark storage with 30-day TTL
- `GuestAccessRepository`: IP-based rate limiting with TTL cleanup

### Service Layer Architecture
- **AuthService**: User lifecycle management, session handling
- **JwtService**: Token generation, validation, claims extraction
- **EmailService**: AWS SES integration for verification and welcome emails
- **UserUsageLimitsService**: Account tier-based usage enforcement
- **PasswordService**: bcrypt security with strength validation

## Testing Strategy

### Test Coverage (433 tests, 73% coverage)
- **Unit Tests**: Service layer business logic with Mockito
- **Integration Tests**: Repository serialization and AWS service integration
- **Security Tests**: JWT validation, password security, IP extraction
- **API Handler Tests**: Request validation, authentication flows, error handling

### Environment Variables for Testing
```bash
export JWT_SECRET="test-secret-key-that-is-at-least-32-characters-long-for-testing"
export AMADEUS_API_KEY="test-api-key"
export AMADEUS_API_SECRET="test-api-secret"
export AMADEUS_ENDPOINT="test.api.amadeus.com"
```

## Important Patterns

### Error Handling
- `SeatmapException`: Base exception for domain errors
- `SeatmapApiException`: API-specific errors with HTTP status codes
- Consistent error responses with success/message/data structure

### Request/Response Flow
1. API Gateway → Lambda Handler
2. Request validation with Jakarta Validation
3. JWT authentication (if required) 
4. Business logic execution
5. Standardized JSON response format

### DynamoDB Patterns
- **GSI Usage**: Email lookups, verification tokens, OAuth IDs
- **TTL Management**: Automatic cleanup for sessions, guest access, bookmarks
- **Composite Keys**: userId + resourceId for user isolation
- **Serialization**: Jackson with @JsonIgnore for DynamoDB internal fields

### Multi-API Aggregation
Flight searches use concurrent API calls with intelligent meshing:
- Amadeus: Primary source with OAuth2 token management
- Sabre: Secondary source with SOAP integration
- Result merging with source prioritization and deduplication

## Security Considerations

### Implemented Security
- **bcrypt Password Hashing**: Cost factor 12
- **JWT Security**: 24-hour expiration, signature validation
- **Email Verification**: Mandatory with secure token generation
- **IP Rate Limiting**: Guest access restrictions
- **Input Validation**: Jakarta validation on all request models
- **User Isolation**: Repository-level access control

### Environment Secrets
Never commit these to repository:
- `JWT_SECRET`: Used for JWT token signing
- `AMADEUS_API_KEY/SECRET`: Amadeus API credentials
- `SABRE_USER_ID/PASSWORD`: Sabre API credentials
- AWS credentials are managed through IAM roles in Lambda

## File Organization

```
src/main/java/com/seatmap/
├── api/handler/          # Lambda function handlers for API endpoints
├── api/service/          # External API integration services
├── auth/handler/         # Authentication Lambda handlers
├── auth/service/         # Authentication business logic
├── auth/repository/      # Data access layer for auth entities
├── common/model/         # Shared domain models
├── common/repository/    # Base repository patterns
├── email/service/        # Email integration services
└── common/exception/     # Custom exception classes

terraform/
├── environments/dev/     # Development environment config
├── environments/prod/    # Production environment config
├── bootstrap/           # Terraform state setup
└── *.tf                # Main infrastructure definitions

docs/                    # API documentation and request examples
```

## External API Documentation

### Amadeus API
- **Documentation**: https://developers.amadeus.com/
- **General API Docs**: https://developers.amadeus.com/self-service
- **SeatMap Display API**: https://developers.amadeus.com/self-service/category/flights/api-doc/seatmap-display/api-reference
- **Flight Offers Search API**: https://developers.amadeus.com/self-service/category/flights/api-doc/flight-offers-search/api-reference
- **Authentication Guide**: https://developers.amadeus.com/self-service/apis-docs/guides/authorization-262
- **Purpose**: Primary flight data source with OAuth2 integration
- **Implementation**: See `AmadeusService.java` for our integration patterns

### Sabre API
- **Documentation**: https://developer.sabre.com/
- **Endpoints Guide**: https://developer.sabre.com/guides/travel-agency/developer-guides/endpoints
- **Flight Discovery API**: ACS_AirportFlightListRQ - Returns active flights departing specified airports
- **Seat Map API v8.0.0**: https://developer.sabre.com/soap-api/seat-map/8.0.0 - Enhanced seat availability and pricing
- **API Resources**: https://developer.sabre.com/soap-api/seat-map/8.0.0/resources.html
- **Authentication**: SOAP SessionCreateRQ with BinarySecurityToken
- **SOAP Endpoints**: 
  - Production: `https://webservices.platform.sabre.com`
  - Certification: `https://webservices.cert.platform.sabre.com`
  - Test: `https://webservices.int.platform.sabre.com`
- **Local Documentation**: `docs-private/sabre/` - Contains detailed API guides, PDFs, and implementation examples
- **Purpose**: Operational flight data and detailed seat availability for airline employee use cases
- **Implementation**: See `SabreService.java` for SOAP integration patterns

**Key APIs for Airline Employee Use Case:**
- **ACS_AirportFlightListRQ**: Discovers active/operational flights by airport (perfect for employee travel planning)
- **Seat Map v8.0.0**: Detailed seat availability, pricing, and aircraft configuration data
- **Resources Available**: All API documentation, user guides, and technical specifications stored in `docs-private/sabre/`