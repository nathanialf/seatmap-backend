# Incident Response Playbook - Seatmap Backend

## Purpose
This playbook provides step-by-step guidance for investigating, documenting, and resolving incidents in the Seatmap Backend system using the Correction of Error (COE) methodology.

## Incident Classification

### Severity Levels

| Severity | Description | Response Time | Examples |
|----------|-------------|---------------|----------|
| **Critical** | Complete service outage or data loss | Immediate (< 15 min) | API completely down, database corruption |
| **High** | Major functionality broken, significant user impact | < 1 hour | Authentication failures, booking system down |
| **Medium** | Important feature degraded, moderate user impact | < 4 hours | Seat map display issues, search performance |
| **Low** | Minor issues, minimal user impact | < 24 hours | UI cosmetic issues, non-critical feature bugs |

### Impact Categories

- **User Experience**: Direct impact on airline employee users
- **System Reliability**: Infrastructure, performance, availability issues  
- **Data Integrity**: Data corruption, inconsistency, or loss
- **Security**: Authentication, authorization, or data protection issues
- **Operational**: Monitoring, alerting, or deployment issues

## Incident Response Process

### 1. Initial Response (First 15 minutes)

#### ✅ Immediate Actions Checklist
- [ ] **Assess Severity**: Determine initial severity level
- [ ] **Create COE Document**: Use `coe-template.md` 
- [ ] **Gather Initial Information**:
  - Error messages and stack traces
  - Affected components and APIs
  - User reports and reproduction steps
  - CloudWatch logs and metrics
- [ ] **Establish Communication**:
  - Create incident channel (if high/critical)
  - Notify relevant stakeholders
  - Set up status updates cadence

#### 🔍 Quick Investigation Steps
```bash
# Check system health
aws logs describe-log-groups --profile seatmap-dev --region us-west-1

# Check recent deployments
git log --oneline --since="24 hours ago"

# Review CloudWatch metrics
aws cloudwatch get-metric-statistics --profile seatmap-dev --region us-west-1 \
  --namespace AWS/Lambda --metric-name Errors \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 --statistics Sum
```

### 2. Investigation Phase

#### 📊 Data Collection
```bash
# Application logs
aws logs get-log-events --log-group-name "/aws/lambda/seatmap-flight-offers-dev" \
  --profile seatmap-dev --region us-west-1 \
  --start-time $(date -d '1 hour ago' +%s)000

# System metrics
aws logs get-log-events --log-group-name "/aws/lambda/seatmap-auth-dev" \
  --profile seatmap-dev --region us-west-1 \
  --start-time $(date -d '1 hour ago' +%s)000

# Database operations
aws dynamodb describe-table --table-name seatmap-users-dev \
  --profile seatmap-dev --region us-west-1
```

#### 🔍 Code Investigation
```bash
# Local testing
./gradlew test --tests="*[ComponentName]*Test"

# Build verification
./gradlew clean build

# Search for related issues
grep -r "ERROR_PATTERN" src/ --include="*.java"
rg "EXCEPTION_TYPE" src/ -A 5 -B 5
```

#### 📝 Documentation Updates
- Update COE document with findings
- Track investigation progress with checkboxes
- Document hypotheses and testing results
- Include relevant code snippets and logs (sanitized)

### 3. Resolution Phase

#### 🔧 Fix Development
1. **Reproduce Locally**: Create test case demonstrating the issue
2. **Develop Fix**: Implement solution with appropriate testing
3. **Code Review**: Document changes in COE
4. **Testing**: Comprehensive regression testing

```bash
# Testing protocol
./gradlew clean test                    # Full test suite
./gradlew test --tests="*Fix*Test"      # Specific fix tests
./gradlew build                         # Build verification
```

#### 🚀 Deployment Process
1. **Staging Deployment**: Test fix in non-production environment
2. **Validation**: Verify fix resolves the issue
3. **Production Deployment**: Deploy through Jenkins pipeline
4. **Post-Deployment Monitoring**: Watch for related issues

### 4. Post-Incident Activities

#### 📋 Documentation Completion
- [ ] **Update COE Document**: Mark as RESOLVED
- [ ] **Document Final Root Cause**: Clear technical explanation
- [ ] **List All Changes**: Code, configuration, infrastructure
- [ ] **Lessons Learned**: What could be improved
- [ ] **Prevention Measures**: How to avoid recurrence

#### 🔄 Follow-up Actions
- [ ] **Monitoring Improvements**: Add alerts/metrics if needed
- [ ] **Documentation Updates**: Update runbooks, README files
- [ ] **Process Improvements**: Update this playbook if needed
- [ ] **Team Knowledge Sharing**: Present findings to team

## Common Investigation Patterns

### API Failures
```bash
# Check API Gateway logs
aws logs filter-log-events --log-group-name "API-Gateway-Execution-Logs_[API_ID]/dev" \
  --profile seatmap-dev --region us-west-1 \
  --start-time $(date -d '1 hour ago' +%s)000

# Test API endpoints
echo "https://your-api-gateway-url/endpoint" > /tmp/api_endpoint
echo "your-jwt-token" > /tmp/jwt_token
curl -X GET $(cat /tmp/api_endpoint) \
  -H "Authorization: Bearer $(cat /tmp/jwt_token)" \
  -H "Content-Type: application/json"
```

### Database Issues
```bash
# Check DynamoDB metrics
aws cloudwatch get-metric-statistics --namespace AWS/DynamoDB \
  --metric-name ThrottledRequests --profile seatmap-dev --region us-west-1 \
  --dimensions Name=TableName,Value=seatmap-users-dev \
  --start-time $(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%S) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 300 --statistics Sum
```

### Authentication Problems
```bash
# Check JWT token validity (use expired tokens only)
echo "EXPIRED_JWT_TOKEN" > /tmp/test_token
# Analyze token structure (do not include in COE if still valid)
```

### External API Issues
```bash
# Test external API connectivity
curl -v https://api.amadeus.com/v2/shopping/flight-offers \
  -H "Authorization: Bearer EXPIRED_TOKEN" \
  --connect-timeout 10 --max-time 30
```

## COE Documentation Best Practices

### ✅ Include
- Sanitized error messages and stack traces
- Code snippets showing problematic areas
- Timeline of investigation and resolution
- Root cause analysis with evidence
- Complete list of changes made
- Lessons learned and prevention measures

### ❌ Exclude
- Live API keys, secrets, or credentials
- Active JWT tokens or session data
- User emails or personal information
- Internal system details that could compromise security
- Unconfirmed hypotheses presented as facts

### 📝 Writing Guidelines
- Use clear, technical language
- Include timestamps in UTC
- Reference specific file paths and line numbers
- Use checkboxes to track progress
- Update status as investigation progresses
- Include code examples with syntax highlighting

## Escalation Procedures

### High/Critical Incidents
1. **Immediate Notification**: Alert team leads and on-call engineers
2. **Incident Commander**: Designate single point of coordination
3. **Status Updates**: Every 30 minutes until resolved
4. **Communication**: Keep stakeholders informed of progress

### External Dependencies
- **AWS Support**: For infrastructure issues
- **Amadeus Support**: For Amadeus API issues  
- **Sabre Support**: For Sabre API issues
- **Third-party Services**: Contact vendor support as needed

## Tools and Resources

### Monitoring & Logging
- **CloudWatch**: Application logs and metrics
- **AWS X-Ray**: Distributed tracing (if enabled)
- **API Gateway Logs**: Request/response logging

### Development Tools
```bash
# Essential commands
./gradlew test                # Run all tests
./gradlew build              # Build project
./gradlew testDetailed       # Detailed test output
rg "PATTERN" src/            # Search codebase
grep -r "PATTERN" src/       # Alternative search
```

### AWS CLI Commands
```bash
# Always include profile and region
aws [command] --profile seatmap-dev --region us-west-1

# Common patterns
aws logs describe-log-groups --profile seatmap-dev --region us-west-1
aws cloudwatch list-metrics --profile seatmap-dev --region us-west-1
aws dynamodb list-tables --profile seatmap-dev --region us-west-1
```

## Template Checklist

When creating a new COE document:
- [ ] Copy `coe-template.md` to new file with descriptive name
- [ ] Fill in incident summary information
- [ ] Document timeline as investigation progresses
- [ ] Include sanitized technical details
- [ ] Track progress with checkboxes
- [ ] Update status field as work progresses
- [ ] Complete post-incident sections when resolved

---

**Playbook Version**: 1.0  
**Last Updated**: November 5, 2025  
**Next Review**: December 5, 2025