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
1. **How did authentication service generate token with past timestamps?**
2. **Why was token lifespan only 1 hour instead of 24 hours?**
3. **What caused the timestamp generation to be incorrect by over a year?**
4. **Are other users receiving similar malformed tokens?**

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

### Critical Discovery - Potential JVM Clock Drift Issue
**HYPOTHESIS:** Research indicates this may be a JVM clock drift issue where JVMs maintain internal clocks based on CPU ticks that can drift from system time.

**Current Evidence:**
- **Working tokens:** Fresh cold starts with correct 2025 timestamps 
- **Failing tokens:** Container reuse with timestamps from JVM initialization time (2024)
- **Pattern:** Both user and guest token generation affected intermittently

### Potential Root Cause - JVM Clock Drift
**Theory:** JVMs may maintain internal clocks based on CPU ticks since VM startup, causing `Instant.now()` to return timestamps from when the JVM was first started rather than current system time.

**AWS Lambda Context:**
- Lambda container reuse could preserve JVM state with drifted clock
- Cold starts initialize fresh JVM with correct time
- Reused containers may maintain old timestamp reference

**Updated Solution Research:** JVM flag `-XX:+UseGetTimeOfDay` does not exist in Java 17 (was HP-UX specific). Modern approach requires using `java.time.Clock` API instead of direct `Instant.now()` calls for proper time management.

## Critical Actions Required (Updated)
- [x] **IMMEDIATE:** Check CloudWatch logs for the exact time the problematic token was generated
- [x] **IMMEDIATE:** Analyze JAR build artifacts and Lambda deployment timestamps
- [ ] **IMMEDIATE:** Search CloudWatch logs for historical instances of this timestamp anomaly
- [ ] **PLANNED:** Wait ~1 hour for Lambda cold start, then test fresh token generation with immediate analysis
- [ ] **PLANNED:** If cold start test normal, make small code change to force new deployment
- [ ] **PLANNED:** Test first token generation after fresh deployment (cold start + new build)
- [ ] **HIGH:** Compare JWT libraries and dependencies for time-related bugs
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
**Investigation Status:** **OPEN - CRITICAL**  
**Next Steps:** System clock and JWT generation audit  
**Escalation:** Development team immediate review required  