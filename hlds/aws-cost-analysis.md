# Seatmap Backend Service - AWS Cost Analysis

## Executive Summary

**Estimated Monthly Cost (Both Environments)**: **$84.64**

- Production: $65.11/month
- Development: $19.53/month (30% of prod)

**Cost Optimized Estimate**: **~$57/month** (with optimizations applied)

---

## Cost Assumptions

### Traffic Profile (Production)

- **Active Users**: 1,000/month
- **Searches per User**: 10/month
- **Seat Map Views**: 2 per search average
- **Cache Hit Rate**: 90% (after initial requests)
- **API Calls to External Services**: 500/day
- **Guest Sessions**: 300/month
- **New Registrations**: 100/month
- **Active Subscriptions**: 1,000

### Development Environment

- **Usage**: 30% of production traffic
- **Active during business hours only**
- **Cost multiplier**: 0.3x production

---

## Detailed Cost Breakdown (Production)

### 1. API Gateway

**Service**: REST API

**Pricing**: $3.50 per million requests

**Usage**:

- Flight searches: 10,000/month
- Seat map views: 20,000/month
- User operations: 5,000/month (login, profile, etc.)
- Subscription operations: 1,000/month
- **Total**: 36,000 requests/month

**Calculation**:

```
36,000 requests / 1,000,000 * $3.50 = $0.126

```

**Monthly Cost**: **$0.13**

---

### 2. Lambda Functions

**Runtime**: Java 17 (1GB memory average)

**Pricing**:

- Requests: $0.20 per 1M requests (first 1M free)
- Compute: $0.0000166667 per GB-second

**Usage**:

| Function | Invocations/Month | Avg Duration | Memory |
| --- | --- | --- | --- |
| Auth Service | 5,000 | 1.0s | 512MB |
| Flight Search | 10,000 | 2.0s | 1GB |
| Seat Map | 20,000 | 1.5s | 1GB |
| User Management | 3,000 | 0.8s | 512MB |
| Subscriptions | 1,000 | 1.2s | 512MB |
| Bookmarks | 2,000 | 0.5s | 512MB |
| API Monitor | 8,640 | 0.3s | 256MB |
| **Total** | **49,640** |  |  |

**Compute Cost Calculation**:

```
Auth: 5,000 * 0.5GB * 1.0s = 2,500 GB-seconds
Flight: 10,000 * 1GB * 2.0s = 20,000 GB-seconds
Seat Map: 20,000 * 1GB * 1.5s = 30,000 GB-seconds
User Mgmt: 3,000 * 0.5GB * 0.8s = 1,200 GB-seconds
Subscriptions: 1,000 * 0.5GB * 1.2s = 600 GB-seconds
Bookmarks: 2,000 * 0.5GB * 0.5s = 500 GB-seconds
API Monitor: 8,640 * 0.25GB * 0.3s = 648 GB-seconds

Total: 55,448 GB-seconds
Cost: 55,448 * $0.0000166667 = $0.92

```

**Request Cost**: First 1M requests free, so $0

**Monthly Cost**: **$0.92**

---

### 3. DynamoDB

**Capacity Mode**: On-Demand

**Pricing**:

- Storage: $0.25 per GB/month
- Read Requests: $0.25 per million
- Write Requests: $1.25 per million

### Users Table

- **Storage**: 1,000 users × 2KB = 2MB
- **Reads**: 15,000/month (logins, profiles)
- **Writes**: 100/month (new users, updates)

### Sessions Table

- **Storage**: 2,000 sessions × 1KB = 2MB (TTL cleanup)
- **Reads**: 40,000/month (auth checks)
- **Writes**: 35,000/month (session creation, updates)

### Bookmarks Table

- **Storage**: 1,500 bookmarks × 5KB = 7.5MB
- **Reads**: 10,000/month
- **Writes**: 1,500/month

### APICache Table

- **Storage**: 100MB (high turnover, short TTL)
- **Reads**: 36,000/month
- **Writes**: 3,600/month (10% cache misses)

### Subscriptions Table

- **Storage**: 1,000 records × 1KB = 1MB
- **Reads**: 5,000/month
- **Writes**: 200/month

**Total Storage**: 110.5MB ≈ 0.11GB

**Total Reads**: 106,000 RRUs
**Total Writes**: 40,400 WRUs

**Calculation**:

```
Storage: 0.11GB * $0.25 = $0.03
Reads: 106,000 / 1,000,000 * $0.25 = $0.03
Writes: 40,400 / 1,000,000 * $1.25 = $0.05

```

**Monthly Cost**: **$0.11**

---

### 4. S3

**Service**: Standard storage

**Pricing**:

- Storage: $0.023 per GB/month
- PUT requests: $0.005 per 1,000 requests
- GET requests: $0.0004 per 1,000 requests

### Terraform State Bucket

- **Storage**: 100MB = 0.1GB
- **Requests**: 100 PUT + 500 GET/month

### Lambda Artifacts Bucket

- **Storage**: 500MB = 0.5GB
- **Requests**: 50 PUT + 200 GET/month

**Calculation**:

```
Storage: 0.6GB * $0.023 = $0.014
PUT: 150 / 1,000 * $0.005 = $0.00075
GET: 700 / 1,000 * $0.0004 = $0.00028

```

**Monthly Cost**: **$0.02**

---

### 5. CloudWatch

**Components**: Logs, Metrics, Alarms, Dashboards

### Logs

- **Ingestion**: 10GB/month
- **Storage**: 5GB average (7-day retention)
- **Pricing**: $0.50/GB ingestion, $0.03/GB storage

**Calculation**:

```
Ingestion: 10GB * $0.50 = $5.00
Storage: 5GB * $0.03 = $0.15

```

**Logs Subtotal**: **$5.15**

### Custom Metrics

- **Count**: 20 metrics
- **Pricing**: $0.30 per metric/month

**Calculation**: `20 * $0.30 = $6.00`

**Metrics Subtotal**: **$6.00**

### Alarms

- **Count**: 10 alarms
- **Pricing**: $0.10 per alarm/month

**Calculation**: `10 * $0.10 = $1.00`

**Alarms Subtotal**: **$1.00**

### Dashboards

- **Count**: 2 dashboards
- **Pricing**: $3.00 per dashboard/month

**Calculation**: `2 * $3.00 = $6.00`

**Dashboards Subtotal**: **$6.00**

**CloudWatch Total**: **$18.15**

---

### 6. SNS

**Service**: Simple Notification Service

**Pricing**:

- Email notifications: First 1,000 free, then $2.00 per 100,000

**Usage**:

- Alert emails: ~100/month

**Monthly Cost**: **$0.00** (within free tier)

---

### 7. External API Costs

### Amadeus API

**Tier**: Self-Service

**Pricing** (estimated):

- Free tier: 2,000 calls/month
- Paid: $0.0015 per call after free tier

**Usage**:

- Total API calls needed: 10,000/month
- With 90% cache hit rate: 1,000 fresh calls/month
- All within free tier

**Monthly Cost**: **$0.00**

### Sabre API

**Pricing**: To be confirmed (assumed similar to Amadeus)

**Usage**: 1,000 calls/month (with caching)

**Estimated Monthly Cost**: **$0.00** (assumed free tier)

**Total External API Cost**: **$0.00** (conservative estimate)

**Note**: If usage exceeds free tiers:

- Amadeus: Additional 8,000 calls = $12.00/month
- Sabre: Similar pricing = $12.00/month
- **Potential additional cost**: $24/month at scale

---

### 8. Data Transfer

**Service**: Outbound data transfer

**Pricing**:

- First 1GB free
- $0.09 per GB thereafter

**Usage**:

- Outbound data: ~10GB/month
- Billable: 9GB

**Calculation**: `9GB * $0.09 = $0.81`

**Monthly Cost**: **$0.81**

---

## Production Environment Summary

| Service | Monthly Cost |
| --- | --- |
| API Gateway | $0.13 |
| Lambda | $0.92 |
| DynamoDB | $0.11 |
| S3 | $0.02 |
| CloudWatch | $18.15 |
| SNS | $0.00 |
| External APIs | $0.00 |
| Data Transfer | $0.81 |
| **TOTAL** | **$20.14** |

**With External API Costs at Scale**: **$44.14/month**

---

## Development Environment

**Multiplier**: 30% of production

**Monthly Cost**: $20.14 * 0.30 = **$6.04**

**With External API Costs**: $44.14 * 0.30 = **$13.24**

---

## Grand Total (Both Environments)

| Scenario | Production | Development | **Total** |
| --- | --- | --- | --- |
| Within Free Tiers | $20.14 | $6.04 | **$26.18** |
| With API Costs | $44.14 | $13.24 | **$57.38** |

**Conservative Estimate (recommended)**: **$57/month**

---

## Cost Optimization Strategies

### 1. CloudWatch Optimization

**Current**: $18.15/month

**Actions**:

- Reduce log retention to 3 days (dev) / 7 days (prod)
- Consolidate metrics (reduce from 20 to 15)
- Use single combined dashboard
- Sample logs instead of capturing all

**Savings**: ~$9/month

**Optimized**: $9.15/month

---

### 2. Lambda Optimization

**Current**: $0.92/month

**Actions**:

- Reduce memory allocations where possible
- Optimize cold start times with SnapStart
- Reduce execution time through code optimization
- Use provisioned concurrency only if needed

**Savings**: ~$0.30/month

**Optimized**: $0.62/month

---

### 3. DynamoDB Optimization

**Current**: $0.11/month

**Actions**:

- Review and optimize query patterns
- Consider provisioned capacity if usage is predictable
- Enable DynamoDB auto-scaling

**Savings**: ~$0.03/month

**Optimized**: $0.08/month

---

### 4. External API Caching

**Current**: 90% hit rate

**Actions**:

- Increase cache TTL to 30 minutes (from 15)
- Implement smarter cache invalidation
- Pre-cache popular routes

**Target**: 95% cache hit rate

**Potential Savings**: $5-10/month on API costs

---

## Optimized Cost Summary

| Component | Current | Optimized | Savings |
| --- | --- | --- | --- |
| CloudWatch | $18.15 | $9.15 | $9.00 |
| Lambda | $0.92 | $0.62 | $0.30 |
| DynamoDB | $0.11 | $0.08 | $0.03 |
| API Caching | - | - | $7.50 |
| **Total Savings** |  |  | **$16.83** |

**Optimized Monthly Cost**: ~$40-50/month (both environments)

---

## Scaling Cost Projections

### Scenario Analysis

| Users | Searches/Month | API Calls | Monthly Cost | Notes |
| --- | --- | --- | --- | --- |
| 100 | 1,000 | 100 | $18 | Free API tiers |
| 1,000 | 10,000 | 1,000 | $26-57 | Baseline (current) |
| 5,000 | 50,000 | 5,000 | $85-140 | Cache critical |
| 10,000 | 100,000 | 10,000 | $160-260 | Consider reserved capacity |
| 50,000 | 500,000 | 50,000 | $650-1,100 | Need cost review |

### Cost Growth Factors

**Linear Scaling**:

- Lambda invocations
- DynamoDB reads/writes
- Data transfer

**Logarithmic Scaling** (good):

- Cache effectiveness improves
- Fixed costs amortized

**Step Function Scaling** (watch for):

- External API tier changes
- Reserved capacity thresholds

---

## Cost Monitoring & Alerts

### Budget Setup

**Recommended Budgets**:

- Development: $15/month
- Production: $60/month
- Alert at 80% threshold
- Alert at 100% threshold

### Cost Anomaly Detection

**Enable AWS Cost Anomaly Detection**:

- Monitor for 20% increase week-over-week
- Alert on unusual service usage
- Review monthly cost trends

### Tagging Strategy

**Required Tags**:

- `Environment`: dev | prod
- `Service`: seatmap-backend
- `CostCenter`: engineering
- `ManagedBy`: terraform

---

## Reserved Capacity Considerations

### When to Consider Reserved Capacity

**DynamoDB**:

- If read/write patterns become predictable
- Savings: up to 75% vs on-demand
- Requires: 1-year commitment

**Lambda**:

- Provisioned concurrency if cold starts are issue
- Cost: $0.0000041667 per GB-second
- Only for high-traffic functions

**Current Recommendation**: Stay on-demand until usage stabilizes

---

## Stripe Fees (Not AWS Costs)

**Subscription Revenue**: $5,000/month (1,000 users × $5)

**Stripe Fees**:

- 2.9% + $0.30 per transaction
- Monthly: $5,000 × 0.029 + (1,000 × $0.30) = $445

**Net Revenue**: $4,555/month

**Note**: Stripe fees are deducted from revenue, not AWS infrastructure costs

---

## Cost Comparison: Alternatives

### Serverless vs. Containerized

**Current (Serverless)**:

- Cost: ~$57/month
- Scaling: Automatic
- Maintenance: Low

**Alternative (ECS Fargate)**:

- Cost: ~$150/month (2 tasks, 0.5 vCPU, 1GB)
- Scaling: Manual/Auto-scaling
- Maintenance: Medium

**Recommendation**: Stay serverless for cost efficiency

---

## Annual Cost Projection

| Scenario | Monthly | Annual |
| --- | --- | --- |
| Current Baseline | $57 | $684 |
| Optimized | $45 | $540 |
| At 5K Users | $115 | $1,380 |
| At 10K Users | $210 | $2,520 |

---

## ROI Analysis

### Break-Even Analysis

**Monthly Costs**: $57

**Subscription Price**: $5/user/month

**Stripe Fees**: $0.45/user (9%)

**Net Revenue per User**: $4.55/month

**Break-Even Users**: 57 / 4.55 = **13 subscribers**

**Profitability**:

- 50 users: $170/month profit
- 100 users: $398/month profit
- 500 users: $2,218/month profit
- 1,000 users: $4,493/month profit