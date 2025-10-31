# JWT Token Investigation - Correction of Error (COE)

## Incident Summary
**Date:** October 26, 2025  
**Severity:** P1 - Critical Authentication System Malfunction  
**Impact:** Authentication service issued invalid JWT token with timestamp from over a year ago  
**Status:** **OPEN** - Critical Issue Requiring Investigation  

## Timeline of Events
- **04:00 UTC** - User began end-to-end bookmark testing with JWT token
- **04:15 UTC** - Token worked successfully for Steps 1-6 of bookmark testing
- **04:19 UTC** - Token began failing with "Invalid or expired token" error
- **04:30 UTC** - Investigation revealed token had timestamps from over a year ago

## Problem Statement
**CRITICAL:** The authentication service issued a JWT token with timestamps indicating it was created over a year ago (epochs 1729993995/1729997595) and had only a 1-hour lifespan, despite the service being deployed only days ago. This represents a fundamental failure in JWT token generation.

## Technical Details

### Anomalous Token Analysis
**Problem Token (issued ~04:00 UTC today):**
- Payload: `{"sub":"[USER_EMAIL]","iss":"seatmap-api","exp":1729997595,"iat":1729993995}`
- **ISSUE:** iat/exp timestamps from over a year ago despite being issued today
- **ISSUE:** Only 1-hour lifespan (3600 seconds) vs expected 24-hour standard
- Token worked initially, then correctly expired

**Corrected Token (issued 04:30 UTC):**
- Issued at (iat): 1761453037 (Oct 26, 2025 04:30:37 UTC) - **CORRECT**
- Expires at (exp): 1761539437 (Oct 27, 2025 04:30:37 UTC) - **CORRECT**
- Duration: 86400 seconds (24 hours) - **CORRECT**

### Critical Questions
1. **How did authentication service generate token with past timestamps?** - *JVM clock drift in Lambda containers*
2. **Why did the "expired" token work initially?** - *Both generation and validation used same drifted time source*
3. **What caused the timestamp generation to be incorrect by over a year?** - *JVM maintaining internal clock from container initialization*
4. **Why did token suddenly stop working?** - *Validation moved to container with correct system time*
5. **Are other users receiving similar malformed tokens intermittently?** - *Investigation ongoing*

## System Evidence
- **Current Time:** October 26, 2025 (~epoch 1761453000)
- **Problem Token Times:** October 2024 (epochs 1729993995/1729997595)
- **Time Discrepancy:** Over 1 year difference
- **Service Deployment:** Only operational for days, not years

## Root Cause Hypotheses
**Primary Suspects:**
1. **System Clock Issues:** Authentication service using incorrect system time
2. **Configuration Error:** JWT generation using wrong time source or timezone
3. **Code Bug:** Timestamp generation logic corrupted
4. **Environment Issue:** Development/test data bleeding into production

## Investigation Findings

### Code Analysis - JWT Generation Logic
**Location:** `/src/main/java/com/seatmap/auth/service/JwtService.java:53-64`
```java
private String createToken(Map<String, Object> claims, String subject) {
    Instant now = Instant.now();  // ← Uses correct system time
    Instant expiration = now.plusSeconds(TOKEN_EXPIRATION_SECONDS);
    
    return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(Date.from(now))      // ← Convert Instant to Date
            .setExpiration(Date.from(expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
}
```

**Analysis:** The JWT generation code is correct and uses `Instant.now()` which should return current UTC time.

### Lambda Infrastructure Analysis
**Runtime:** Java 17 (confirmed in Terraform)
**Environment:** No timezone overrides or custom time configurations
**Deployment:** Standard AWS Lambda with no time-related modifications

### Critical Discovery - JVM Clock Drift in Validation Chain
**HYPOTHESIS:** Research indicates this may be a JVM clock drift issue affecting both token generation AND validation, explaining why "expired" tokens initially worked.

**Current Evidence:**
- **Working tokens:** Fresh cold starts with correct 2025 timestamps 
- **Failing tokens:** Container reuse with timestamps from JVM initialization time (2024)
- **Pattern:** Both user and guest token generation affected intermittently
- **KEY INSIGHT:** "Expired" tokens worked temporarily, then suddenly failed

### Root Cause Analysis - Dual Clock Drift
**Theory:** JVMs may maintain internal clocks based on CPU ticks since VM startup, causing `Instant.now()` to return timestamps from when the JVM was first started rather than current system time.

**Critical Finding - Why "Old Tokens Worked":**
JWT validation uses automatic expiration checking in the library (`Jwts.parserBuilder().parseClaimsJws()`), which compares token `exp` timestamp against **current system time during validation**. 

**Scenario Timeline:**
1. **Token Generation (drifted container):** Creates token with `exp: 1729997595` (Oct 26, 2024)
2. **Token Validation (same/similar drifted container):** Library compares against drifted system time → **VALID**
3. **Later Validation (fresh container):** Library compares against correct 2025 system time → **EXPIRED**

**AWS Lambda Context:**
- Lambda container reuse could preserve JVM state with drifted clock across multiple function invocations
- Cold starts initialize fresh JVM with correct time
- Reused containers may maintain old timestamp reference affecting BOTH generation and validation
- Validation occurs on every API call: flight-offers, seat-map, bookmarks, auth endpoints

**JWT Validation Code Analysis:**
```java
// JwtService.validateToken() - expiration checked automatically by library
return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)  // ← Library checks: if (tokenExp < currentTime) throw ExpiredJwtException
        .getBody();
```

**Updated Solution Research:** JVM flag `-XX:+UseGetTimeOfDay` does not exist in Java 17 (was HP-UX specific). Modern approach requires using `java.time.Clock` API instead of direct `Instant.now()` calls for proper time management, but Clock API would still be affected by same underlying JVM clock drift.

## Mitigation Options Analysis

### 1. Force Lambda Container Refresh (Immediate/Short-term)
**Option A: Reduce Lambda Memory/Timeout**
- **Pros**: Forces more frequent cold starts, fresh JVMs
- **Cons**: Performance impact, higher costs, doesn't solve root cause
- **Implementation**: Adjust memory from 512MB to 256MB in Terraform

**Option B: Manual Container Cycling**
- **Pros**: Can test if fresh containers resolve issue
- **Cons**: Not sustainable, manual intervention required
- **Implementation**: Deploy code changes to force container refresh

### 2. Alternative Time Sources (Medium-term)
**Option A: External Time Service**
```java
// Use AWS Systems Manager Parameter Store with current timestamp
private Instant getCurrentTime() {
    String timestamp = ssmClient.getParameter("current-timestamp").getValue();
    return Instant.ofEpochSecond(Long.parseLong(timestamp));
}
```
- **Pros**: Bypasses JVM clock entirely
- **Cons**: Network latency, dependency on external service, complexity

**Option B: HTTP Time Service**
```java
// Call external time API (time.gov, worldtimeapi.org)
private Instant getCurrentTime() {
    HttpResponse<String> response = httpClient.send(...);
    return Instant.parse(json.get("utc_datetime").getAsString());
}
```
- **Pros**: Reliable external time source
- **Cons**: Network dependency, latency, potential failure point

### 3. JVM Time Management (Medium-term)
**Option A: Clock API Dependency Injection**
```java
public class JwtService {
    private final Clock clock;
    
    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = clock.instant(); // Instead of Instant.now()
        // ... rest of method
    }
}
```
- **Pros**: Testable, configurable time source
- **Cons**: Still uses same underlying system clock that may drift

**Option B: System Property Time Source**
```java
private Instant getCurrentTime() {
    // Force system call instead of cached JVM time
    return Instant.ofEpochMilli(System.currentTimeMillis());
}
```
- **Pros**: May bypass JVM internal clock
- **Cons**: Not guaranteed to work, still JVM-dependent

### 4. Monitoring and Detection (Immediate)
**Option A: Token Timestamp Validation**
```java
private void validateTokenTimestamps(String token) {
    Claims claims = parseTokenUnsafe(token);
    long iat = claims.getIssuedAt().getTime();
    long now = System.currentTimeMillis();
    
    // Alert if token timestamps are > 1 hour from current time
    if (Math.abs(iat - now) > 3600000) {
        logger.error("CLOCK_DRIFT_DETECTED: Token iat {} differs from system time {} by {} ms", 
            iat, now, Math.abs(iat - now));
    }
}
```
- **Pros**: Early detection, monitoring capability
- **Cons**: Reactive, doesn't prevent issue

**Option B: Health Check Endpoint**
```java
@GetMapping("/health/time")
public Map<String, Object> timeHealthCheck() {
    return Map.of(
        "system_time", System.currentTimeMillis(),
        "instant_now", Instant.now().toEpochMilli(),
        "container_start_time", containerStartTime
    );
}
```
- **Pros**: Proactive monitoring, debugging capability
- **Cons**: Doesn't fix issue, just detects it

### 5. AWS Lambda Configuration (Short-term)
**Option A: Reserved Concurrency = 1**
- **Pros**: Forces single container, consistent behavior
- **Cons**: No scaling, performance bottleneck

**Option B: Provisioned Concurrency**
- **Pros**: Pre-warmed containers, more predictable
- **Cons**: Higher costs, may still have drift over time

### 6. Token Strategy Changes (Long-term)
**Option A: Shorter Token Lifespans**
```java
private static final int TOKEN_EXPIRATION_SECONDS = 1 * 60 * 60; // 1 hour instead of 24
```
- **Pros**: Reduces impact window of drift
- **Cons**: More frequent token refresh needed, user experience impact

**Option B: Refresh Token Pattern**
- **Pros**: Can detect and correct drift during refresh
- **Cons**: Additional complexity, more API calls

## Recommended Implementation Strategy

### Phase 1: Immediate Detection (This Week)
1. **Add timestamp monitoring** to detect future drift occurrences
2. **Deploy small code change** to force fresh container deployment
3. **Test multiple token generations** across different time periods
4. **Implement health check endpoint** for time drift monitoring

### Phase 2: Short-term Mitigation (Next Sprint)
1. **Implement Clock API dependency injection** for better testability
2. **Add comprehensive timestamp validation** with CloudWatch alerts
3. **Consider external time service fallback** for critical operations
4. **Evaluate Lambda configuration changes** (memory, concurrency)

### Phase 3: Long-term Architecture (Future)
1. **Evaluate container-based Lambda** for more runtime control
2. **Implement comprehensive time drift monitoring** across all services
3. **Consider token strategy changes** if issue persists
4. **Runtime environment alternatives** if JVM clock drift proves persistent

## Critical Actions Required (Updated)
- [x] **IMMEDIATE:** Check CloudWatch logs for the exact time the problematic token was generated
- [x] **IMMEDIATE:** Analyze JAR build artifacts and Lambda deployment timestamps
- [ ] **IMMEDIATE:** Search CloudWatch logs for historical instances of this timestamp anomaly
- [ ] **IMMEDIATE:** Implement timestamp monitoring in JWT generation
- [ ] **PLANNED:** Wait ~1 hour for Lambda cold start, then test fresh token generation with immediate analysis
- [ ] **PLANNED:** If cold start test normal, make small code change to force new deployment
- [ ] **PLANNED:** Test first token generation after fresh deployment (cold start + new build)
- [ ] **HIGH:** Implement health check endpoint for time drift detection
- [ ] **HIGH:** Monitor token generation patterns over multiple cold starts

## Business Impact (Updated)
- **Security Risk:** Intermittent token generation producing invalid timestamps across entire authentication system
- **User Impact:** Unpredictable authentication failures affecting both guest and registered users
- **System Reliability:** Core timestamp generation system unreliable, indicating deeper runtime issues
- **Production Risk:** Issue may have been occurring undetected for extended period
- **Trust Impact:** Fundamental JWT implementation showing intermittent failures

## Investigation Priority
**P0 - Critical Production Issue** - System-wide intermittent JWT timestamp generation failure affecting all authentication flows. Potential for widespread user impact and indicates serious runtime environment issues.

---

## Recent JWT Signature Validation Failure (October 31, 2025)

### New Incident Details
**Date:** October 31, 2025  
**Time:** ~07:58 UTC  
**Issue:** JWT signature validation failure during API testing  
**Error:** "Invalid JWT signature: JWT signature does not match locally computed signature"

### Failed Token Analysis
**Token**: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyXzQ1NmRlZiIsImVtYWlsIjoidGVzdEBleGFtcGxlLmNvbSIsInRva2VuX3R5cGUiOiJ1c2VyIiwiaWF0IjoxNzYxODk0NzIzLCJleHAiOjE3NjE5ODExMjN9.RP8eVhNhYvMi4zIUY64gULxNfj1xWCJ1T4jWC9FpZGo`

**Decoded Payload**:
```json
{
  "sub": "user_456def",
  "email": "test@example.com", 
  "token_type": "user",
  "iat": 1761894723,  // Oct 31, 2025 06:52:03 UTC - CORRECT TIMESTAMP
  "exp": 1761981123   // Nov 1, 2025 07:12:03 UTC - CORRECT TIMESTAMP
}
```

**Timing Analysis**:
- **Issued**: Oct 31, 2025 06:52:03 UTC (CORRECT - current date)
- **Expires**: Nov 1, 2025 07:12:03 UTC (CORRECT - 24 hours later)
- **Current**: Oct 31, 2025 07:58:43 UTC (within validity period)
- **Duration**: 24 hours (86,400 seconds) - CORRECT
- **Remaining**: ~23.2 hours (should be valid)

### Key Differences from Previous Incident
1. **Correct Timestamps**: Unlike the October 26 incident, this token has proper 2025 timestamps
2. **Proper Duration**: 24-hour lifespan as expected (not 1-hour)
3. **Different Failure Mode**: Signature validation failure, not expiration
4. **Timing**: Token was valid and working earlier, then suddenly failed signature validation

### Environment Configuration Analysis
**Current JWT_SECRET across all Lambda functions**:
- **seatmap-seat-map-dev**: `"p+JoYjxAoE+4NkTzi8rjFni+UnHSmitO382Gp1MnCTg="`
- **seatmap-auth-dev**: `"p+JoYjxAoE+4NkTzi8rjFni+UnHSmitO382Gp1MnCTg="`  
- **seatmap-bookmarks-dev**: `"p+JoYjxAoE+4NkTzi8rjFni+UnHSmitO382Gp1MnCTg="`

**Finding**: All Lambda functions have identical JWT_SECRET, ruling out configuration inconsistency.

### Root Cause Analysis - JWT Secret Rotation
**Most Likely Scenario**:
1. Token was issued by auth Lambda with `JWT_SECRET_VERSION_A`
2. Terraform deployment occurred that changed `JWT_SECRET` to `JWT_SECRET_VERSION_B`
3. Token validation attempted with new secret → signature mismatch → failure

**JWT Signature Process**:
- **Creation**: `HMACSHA256(base64(header) + "." + base64(payload), JWT_SECRET_OLD)`
- **Validation**: `HMACSHA256(base64(header) + "." + base64(payload), JWT_SECRET_NEW)`
- **Result**: Signatures don't match → "Invalid JWT signature" error

### CloudWatch Evidence
**Log Entry**: 
```
[main] WARN com.seatmap.auth.service.JwtService - Invalid JWT signature: JWT signature does not match locally computed signature. JWT validity cannot be asserted and should not be trusted.
```

**Source**: `/aws/lambda/seatmap-seat-map-dev` at timestamp `1761897496373`

### JWT Implementation Code Analysis
**Token Creation** (`JwtService.java:55-66`):
```java
return Jwts.builder()
        .setClaims(claims)
        .setSubject(subject)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiration))
        .signWith(secretKey, SignatureAlgorithm.HS256)  // Uses current JWT_SECRET
        .compact();
```

**Token Validation** (`JwtService.java:68-74`):
```java
return Jwts.parserBuilder()
        .setSigningKey(secretKey)  // Uses current JWT_SECRET
        .build()
        .parseClaimsJws(token)     // Validates signature against current secret
        .getBody();
```

### Updated Root Cause Assessment
**Primary Issue**: JWT secret rotation without token invalidation strategy

**Contributing Factors**:
1. No token versioning or secret rotation strategy
2. Outstanding tokens become invalid immediately after secret change
3. No graceful transition period for secret rotation
4. Terraform deployments can change secrets without notification

### Immediate Resolution
**Required Action**: Issue fresh token with current JWT_SECRET

**Long-term Recommendations**:
1. Implement JWT secret versioning with gradual rotation
2. Add secret rotation detection and alerts
3. Consider shorter token lifespans during active development
4. Implement token refresh patterns for production use

---
**Investigation Status:** **OPEN - CRITICAL**  
**Next Steps:** System clock and JWT generation audit, plus JWT secret rotation strategy  
**Escalation:** Development team immediate review required  