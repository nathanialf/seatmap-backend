# Development Environment Setup

## Overview

This guide provides comprehensive instructions for setting up a development environment for the SeatMap Backend API, including local development, testing, and deployment processes.

## Prerequisites

### Required Tools
- **Java 17** - Required for building and running the application
- **Gradle 8.4+** - Build automation tool
- **AWS CLI** - For AWS resource management and deployment
- **Terraform 1.0+** - Infrastructure as Code tool
- **Git** - Version control
- **Docker** (optional) - For containerized testing

### AWS Account Requirements
- AWS account with appropriate permissions
- AWS CLI configured with profiles
- Access to DynamoDB, Lambda, API Gateway, and SES services

## AWS Configuration

### AWS Profiles
The project uses AWS profiles for environment management:

```bash
# Configure the development profile
aws configure --profile seatmap-dev
```

**Required AWS Profile**: `seatmap-dev`
- **Region**: `us-west-1`
- **Access Key ID**: Contact administrator
- **Secret Access Key**: Contact administrator

### AWS Permissions
The AWS user/role must have permissions for:
- **DynamoDB**: Full access to create, read, update tables
- **Lambda**: Create and manage Lambda functions
- **API Gateway**: Create and manage REST APIs
- **IAM**: Create roles and policies for Lambda functions
- **SES**: Send emails for user verification
- **S3**: Access to Terraform state bucket

## Project Structure

```
seatmap-backend/
├── src/
│   ├── main/java/com/seatmap/
│   │   ├── api/           # Flight and seat map APIs
│   │   ├── auth/          # Authentication and user management
│   │   └── common/        # Shared models and utilities
│   └── test/java/         # Unit and integration tests
├── terraform/
│   └── environments/
│       └── dev/           # Development environment infrastructure
├── docs/                  # API documentation
├── build.gradle          # Build configuration
└── README.md
```

## Development Setup

### 1. Clone and Build

```bash
# Clone the repository
git clone [repository-url]
cd seatmap-backend

# Build the project
./gradlew build

# Run tests
./gradlew test
```

### 2. Environment Variables

The application requires these environment variables for local development:

```bash
export ENVIRONMENT=dev
export AWS_PROFILE=seatmap-dev
export AWS_REGION=us-west-1

# API Keys (contact administrator for values)
export AMADEUS_API_KEY=your_amadeus_key
export AMADEUS_API_SECRET=your_amadeus_secret
export SABRE_USER_ID=your_sabre_user
export SABRE_PASSWORD=your_sabre_password
export JWT_SECRET=your_jwt_secret
```

### 3. Database Setup

The application uses DynamoDB tables. Ensure your AWS profile has access to:

- `seatmap-users-dev`
- `seatmap-sessions-dev`
- `seatmap-subscriptions-dev`
- `seatmap-guest-access-dev`
- `seatmap-bookmarks-dev`
- `seatmap-account-tiers-dev`
- `seatmap-user-usage-dev`

### 4. Account Tier Configuration

After infrastructure deployment, populate the account tiers table:

```bash
# Set up account tiers (run once)
./scripts/setup-tiers.sh
```

This script configures the three account tiers:
- **FREE**: Basic access with limited quotas
- **PRO**: Enhanced access with increased limits
- **BUSINESS**: Unlimited access (one-time purchase)

## Infrastructure Management

### Terraform Setup

```bash
# Navigate to terraform directory
cd terraform/environments/dev

# Initialize Terraform (first time)
export AWS_PROFILE=seatmap-dev
terraform init

# Plan infrastructure changes
terraform plan \
  -var="amadeus_api_key=dummy" \
  -var="amadeus_api_secret=dummy" \
  -var="sabre_user_id=dummy" \
  -var="sabre_password=dummy" \
  -var="jwt_secret=dummy"

# Apply infrastructure (with real values)
terraform apply
```

### Jenkins Variables

When deploying via Jenkins, these variables are injected:

- `AMADEUS_API_KEY` - Amadeus API credentials
- `AMADEUS_API_SECRET` - Amadeus API credentials  
- `SABRE_USER_ID` - Sabre API credentials
- `SABRE_PASSWORD` - Sabre API credentials
- `JWT_SECRET` - JSON Web Token signing secret

**Note**: These are sensitive values managed by the CI/CD pipeline.

## Testing

### Unit Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "UserUsageLimitsServiceTest"

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Integration Testing

The project includes comprehensive integration tests for:
- Authentication flows
- Tier-based access controls
- Bookmark management
- Seat map retrieval
- Guest access limits

### Manual API Testing

Refer to the [Testing Guide](./api/testing-guide.md) for complete API testing examples with cURL commands.

## Deployment Process

### Local Development Deployment

1. **Build the JAR**:
   ```bash
   ./gradlew build
   ```

2. **Prepare Infrastructure**:
   ```bash
   cd terraform/environments/dev
   export AWS_PROFILE=seatmap-dev
   terraform plan [with variables]
   ```

3. **Deploy Infrastructure**:
   ```bash
   terraform apply [with real credentials]
   ```

4. **Configure Account Tiers**:
   ```bash
   ./scripts/setup-tiers.sh
   ```

### Jenkins Deployment

The Jenkins pipeline automatically:
1. Builds the JAR file (`SEATMAP-Backend-1.0.0.jar`)
2. Runs all tests
3. Validates Terraform configurations
4. Deploys infrastructure with injected secrets
5. Configures account tiers

## Common Development Tasks

### Adding New Account Tiers

1. Update the tier setup script
2. Modify the `AccountTier` enum if needed
3. Update documentation
4. Test tier validation logic

### Updating Usage Limits

1. Modify the tier setup script values
2. Update tests with new limits
3. Update API documentation
4. Deploy via standard process

### Database Schema Changes

1. Update Terraform DynamoDB table definitions
2. Plan and apply infrastructure changes
3. Update repository classes if needed
4. Test with new schema

## Troubleshooting

### Common Issues

**Terraform Access Denied**:
- Verify AWS profile configuration
- Check AWS credentials expiration
- Ensure IAM permissions are correct

**DynamoDB Connection Errors**:
- Verify tables exist in the correct region
- Check AWS profile and region settings
- Ensure proper IAM permissions

**Test Failures**:
- Ensure all dependencies are available
- Check for proper mock setup
- Verify test isolation

**JAR File Issues**:
- Ensure correct JAR name (`SEATMAP-Backend-1.0.0.jar`)
- Check Gradle build configuration
- Verify file exists at expected path

### Debug Mode

Enable debug logging for development:

```bash
# Add to environment variables
export LOGGING_LEVEL_ROOT=DEBUG
export LOGGING_LEVEL_COM_SEATMAP=DEBUG
```

## Security Considerations

### Local Development
- Never commit sensitive credentials
- Use environment variables for secrets
- Rotate development API keys regularly
- Use development-specific AWS accounts when possible

### Production Deployment
- All secrets managed via Jenkins/CI pipeline
- AWS credentials with minimal required permissions
- Regular credential rotation
- Monitoring and alerting for unauthorized access

## Additional Resources

- [API Documentation](./README.md)
- [Testing Guide](./api/testing-guide.md)
- [Error Handling](./api/error-handling.md)
- AWS Documentation for DynamoDB, Lambda, API Gateway
- Terraform AWS Provider Documentation

---

For questions or issues with the development setup, contact the development team or refer to the troubleshooting section above.