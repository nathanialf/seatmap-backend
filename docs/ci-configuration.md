# CI/CD Configuration

## Overview

This document details the Jenkins variables, AWS profiles, and configuration requirements for the SeatMap Backend CI/CD pipeline.

## Jenkins Pipeline Variables

The Jenkins pipeline requires these environment variables to be configured in the Jenkins environment:

### Required Secret Variables

| Variable Name | Description | Usage | Example Value |
|---------------|-------------|-------|---------------|
| `AMADEUS_API_KEY` | Amadeus API access key | Flight data and seat map APIs | `your_amadeus_key` |
| `AMADEUS_API_SECRET` | Amadeus API secret key | Flight data and seat map APIs | `your_amadeus_secret` |
| `SABRE_USER_ID` | Sabre API user identifier | Backup flight data provider | `your_sabre_user` |
| `SABRE_PASSWORD` | Sabre API password | Backup flight data provider | `your_sabre_password` |
| `JWT_SECRET` | JSON Web Token signing key | User authentication | `your_jwt_secret_min_256_bits` |

### Configuration Notes

**Secret Management**:
- All variables contain sensitive credentials
- Must be configured as Jenkins secret variables
- Values should be rotated regularly
- Never expose in logs or console output

**Variable Validation**:
- Jenkins pipeline validates all required variables are present
- Terraform plan command uses dummy values for validation
- Terraform apply uses real credential values

## AWS Profile Configuration

### Required AWS Profile

**Profile Name**: `seatmap-dev`
- **Region**: `us-west-1`
- **Purpose**: Development environment deployment
- **Access**: DynamoDB, Lambda, API Gateway, IAM, SES, S3

### AWS Credentials Setup

The CI/CD pipeline expects AWS credentials to be configured via:

1. **Jenkins AWS Plugin** (recommended)
2. **IAM Instance Profile** (for EC2-hosted Jenkins)
3. **AWS CLI Profile** (for local Jenkins)

### Required AWS Permissions

The AWS credentials must have the following service permissions:

#### DynamoDB
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:CreateTable",
        "dynamodb:DeleteTable",
        "dynamodb:DescribeTable",
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Query",
        "dynamodb:Scan",
        "dynamodb:UpdateTimeToLive",
        "dynamodb:DescribeTimeToLive"
      ],
      "Resource": [
        "arn:aws:dynamodb:us-west-1:*:table/seatmap-*-dev",
        "arn:aws:dynamodb:us-west-1:*:table/seatmap-*-dev/index/*"
      ]
    }
  ]
}
```

#### Lambda
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "lambda:CreateFunction",
        "lambda:UpdateFunctionCode",
        "lambda:UpdateFunctionConfiguration",
        "lambda:DeleteFunction",
        "lambda:GetFunction",
        "lambda:ListFunctions",
        "lambda:AddPermission",
        "lambda:RemovePermission",
        "lambda:InvokeFunction"
      ],
      "Resource": "arn:aws:lambda:us-west-1:*:function:seatmap-*-dev"
    }
  ]
}
```

#### API Gateway
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "apigateway:*"
      ],
      "Resource": "*"
    }
  ]
}
```

#### IAM (for Lambda roles)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "iam:CreateRole",
        "iam:DeleteRole",
        "iam:GetRole",
        "iam:PassRole",
        "iam:AttachRolePolicy",
        "iam:DetachRolePolicy",
        "iam:PutRolePolicy",
        "iam:DeleteRolePolicy",
        "iam:GetRolePolicy"
      ],
      "Resource": "arn:aws:iam::*:role/seatmap-*-dev"
    }
  ]
}
```

#### SES (for email verification)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail",
        "ses:VerifyEmailIdentity",
        "ses:GetIdentityVerificationAttributes"
      ],
      "Resource": "*"
    }
  ]
}
```

#### S3 (for Terraform state)
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::seatmap-backend-terraform-state-dev/*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": "arn:aws:s3:::seatmap-backend-terraform-state-dev"
    }
  ]
}
```

## Jenkins Pipeline Structure

### Build Stage
1. **Checkout**: Clone repository
2. **Build**: Execute `./gradlew build`
3. **Test**: Execute `./gradlew test`
4. **Package**: Generate `SEATMAP-Backend-1.0.0.jar`

### Validation Stage
1. **Terraform Init**: Initialize with S3 backend
2. **Terraform Plan**: Validate configuration with dummy variables
3. **Lint**: Code quality checks
4. **Security Scan**: Credential and vulnerability scanning

### Deployment Stage
1. **Terraform Apply**: Deploy with real credential variables
2. **Function Update**: Update Lambda functions with new code
3. **Account Tier Setup**: Configure tier definitions
4. **Smoke Tests**: Basic API health checks

## Terraform Backend Configuration

### State Management
- **Backend**: S3
- **Bucket**: `seatmap-backend-terraform-state-dev`
- **Key**: `seatmap-backend/terraform.tfstate`
- **Region**: `us-west-1`
- **DynamoDB Lock Table**: `seatmap-backend-terraform-locks-dev`

### Required S3 Bucket Policy
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "AWS": "arn:aws:iam::ACCOUNT-ID:role/jenkins-role"
      },
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::seatmap-backend-terraform-state-dev/*"
    }
  ]
}
```

## Security Best Practices

### Credential Management
- **Rotation**: Rotate API keys quarterly
- **Scope**: Use least-privilege IAM policies
- **Monitoring**: Enable AWS CloudTrail for audit logs
- **Encryption**: Enable S3 bucket encryption for state files

### CI/CD Security
- **Secrets**: Never log credential values
- **Isolation**: Use separate AWS accounts for dev/prod
- **Validation**: Validate all inputs and outputs
- **Rollback**: Maintain rollback procedures

## Environment-Specific Configuration

### Development Environment
- **Profile**: `seatmap-dev`
- **Region**: `us-west-1`
- **Resource Prefix**: `seatmap-*-dev`
- **Monitoring**: Basic CloudWatch logs

### Production Environment (Future)
- **Profile**: `seatmap-prod`
- **Region**: `us-west-1`
- **Resource Prefix**: `seatmap-*-prod`
- **Monitoring**: Enhanced monitoring and alerting

## Troubleshooting

### Common CI/CD Issues

**Terraform State Lock**:
```bash
# If Terraform state is locked
terraform force-unlock LOCK_ID
```

**AWS Credential Issues**:
- Verify AWS profile configuration
- Check IAM permission policies
- Validate AWS CLI access

**DynamoDB Access Denied**:
- Verify table names match environment
- Check IAM DynamoDB permissions
- Ensure correct region configuration

**Lambda Deployment Failures**:
- Verify JAR file exists and is correct size
- Check Lambda function memory/timeout settings
- Validate IAM Lambda execution role

### Debug Commands

```bash
# Verify AWS access
aws sts get-caller-identity --profile seatmap-dev

# Check Terraform state
terraform show

# Validate Terraform configuration
terraform validate

# Check Lambda function
aws lambda get-function --function-name seatmap-auth-dev --profile seatmap-dev
```

## Contact Information

For CI/CD pipeline issues or credential access:
- **DevOps Team**: [contact information]
- **AWS Account Admin**: [contact information]
- **API Key Management**: [contact information]

---

**Note**: This document contains sensitive configuration information. Access should be restricted to authorized personnel only.