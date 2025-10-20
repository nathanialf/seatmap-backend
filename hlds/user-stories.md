# Seatmap Backend Service - User Stories

## Epic 1: Authentication & Account Management

### US-001: User Registration with Email/Password

**As an** airline employee

**I want to** create an account with my email and password

**So that** I can access the seatmap service

**Acceptance Criteria:**

- Email validation (proper format, not already registered)
- Password requirements enforced (min 8 chars, uppercase, lowercase, number, special char)
- Email verification sent after registration
- User account created in Users table
- User can access service after email verification

**Technical Notes:**

- Use bcrypt for password hashing (cost factor 12)
- Store user with authProvider="email"
- Generate JWT token after verification
- Rate limit registration endpoint

**Priority:** P0 | **Story Points:** 5

---

### US-002: Google Sign-In

**As a** user

**I want to** sign in with my Google account

**So that** I can quickly access the service without creating a new password

**Acceptance Criteria:**

- OAuth 2.0 flow redirects to Google for authentication
- New account auto-created on first sign-in
- Existing account linked if email matches
- JWT token returned after successful authentication

**Technical Notes:**

- Implement Google OAuth 2.0 authorization code flow
- Store Google user ID in oauthId field
- Verify Google ID token on backend

**Priority:** P0 | **Story Points:** 8

---

### US-003: Apple Sign In

**As a** user

**I want to** sign in with my Apple ID

**So that** I can use a secure, privacy-focused authentication method

**Acceptance Criteria:**

- Apple authentication flow completes successfully
- Handles anonymized Apple email addresses
- New account auto-created on first sign-in
- JWT token returned after successful authentication

**Technical Notes:**

- Implement Apple Sign In with REST API
- Verify Apple identity token using Apple's public keys
- Store Apple user ID in oauthId field

**Priority:** P0 | **Story Points:** 8

---

### US-004: Continue as Guest

**As a** visitor

**I want to** try the service without creating an account

**So that** I can quickly check seat availability for up to 2 flights

**Acceptance Criteria:**

- Guest session created with unique identifier
- Guest JWT token issued with 24-hour expiration
- Guest can search for flights without restrictions
- Guest can view seat maps for up to 2 flights
- Counter shows "X/2 flights viewed" after first view
- After 2nd view, modal prompts to register/sign in
- Guest session tracked in Sessions table with guestFlightsViewed counter

**Technical Notes:**

- Generate guest JWT with role="guest"
- Increment guestFlightsViewed on each seat map view
- Return 403 with upgrade message after 2 views
- Auto-expire guest sessions after 24 hours via TTL

**Priority:** P0 | **Story Points:** 5

---

### US-005: User Login

**As a** registered user

**I want to** log in with my email and password

**So that** I can access my bookmarks and subscription

**Acceptance Criteria:**

- Valid credentials authenticate successfully
- Invalid credentials show clear error message
- JWT token returned on successful login
- Session created in Sessions table with 24-hour TTL
- Account locked after 5 failed attempts (15-minute lockout)

**Technical Notes:**

- Verify password against bcrypt hash
- Generate JWT with 24-hour expiration
- Implement rate limiting on login endpoint

**Priority:** P0 | **Story Points:** 3

---

### US-006: Password Reset

**As a** user who forgot their password

**I want to** reset my password via email

**So that** I can regain access to my account

**Acceptance Criteria:**

- Password reset email sent with unique token
- Token expires after 1 hour
- User can set new password with valid token
- All active sessions invalidated after password reset

**Technical Notes:**

- Generate secure random token (32 bytes)
- Store token in DynamoDB with 1-hour TTL
- Hash new password with bcrypt

**Priority:** P1 | **Story Points:** 5

---

### US-007: Change Password

**As a** logged-in user

**I want to** change my password

**So that** I can maintain account security

**Acceptance Criteria:**

- Current password must be validated
- New password meets all requirements
- All active sessions invalidated except current one
- User notified via email of password change

**Technical Notes:**

- Verify current password before allowing change
- Hash new password with bcrypt
- Keep current session active

**Priority:** P1 | **Story Points:** 3

---

## Epic 2: Subscription Management

### US-008: Subscribe to Service

**As a** registered user

**I want to** subscribe for $5/month using my credit card

**So that** I can access the service without guest limitations

**Acceptance Criteria:**

- User enters payment information via Stripe
- Subscription created in Stripe with $5/month recurring
- User status updated to "active subscriber"
- Subscription record created in Subscriptions table
- Access granted immediately after payment

**Technical Notes:**

- Use Stripe Checkout or Payment Element
- Store Stripe customer ID and subscription ID
- Handle webhook for subscription confirmation
- Set up recurring billing on 1st of each month

**Priority:** P0 | **Story Points:** 8

---

### US-009: Cancel Subscription

**As a** subscriber

**I want to** cancel my subscription at any time

**So that** I won't be charged in future periods

**Acceptance Criteria:**

- Subscription cancelled immediately in Stripe
- Access continues until end of current billing period
- User cannot access premium features after period ends
- Cancellation confirmation email sent

**Technical Notes:**

- Call Stripe API to cancel subscription
- Set cancelAtPeriodEnd=true in Stripe
- Update subscription status in DynamoDB

**Priority:** P0 | **Story Points:** 5

---

### US-010: Update Payment Method

**As a** subscriber

**I want to** update my credit card information

**So that** my subscription continues without interruption

**Acceptance Criteria:**

- User can add new payment method via Stripe
- Default payment method updated
- No disruption to active subscription
- Next invoice uses new payment method

**Technical Notes:**

- Use Stripe Payment Element for card update
- Verify new payment method before removing old one

**Priority:** P1 | **Story Points:** 5

---

### US-011: Handle Payment Failure

**As a** system

**I want to** immediately cancel subscriptions when payment fails

**So that** service access is tied to successful payment

**Acceptance Criteria:**

- Payment failure detected via Stripe webhook (`invoice.payment_failed`)
- Subscription cancelled immediately
- User access revoked immediately
- Email sent to user with resubscription instructions

**Technical Notes:**

- Listen to `invoice.payment_failed` webhook
- Cancel subscription in Stripe
- Update subscription status to "cancelled"

**Priority:** P0 | **Story Points:** 5

---

## Epic 3: Flight Search & Discovery

### US-012: Search Flights with Basic Filters

**As a** user (registered or guest)

**I want to** search for flights by origin, destination, and date

**So that** I can find flights that match my travel plans

**Acceptance Criteria:**

- Search form accepts 3-letter IATA airport codes
- Date must be in the future
- Results aggregated from both Amadeus and Sabre APIs
- Duplicate flights intelligently deduplicated
- Results sorted by departure time (earliest first)
- Each flight shows: airline, flight number, times, duration, price
- Error message if no results found

**Technical Notes:**

- Query both APIs in parallel
- Deduplicate based on airline + flight number + departure time
- Cache results for 15 minutes in APICache table
- Handle API failures gracefully

**Priority:** P0 | **Story Points:** 13

---

### US-013: Filter by Travel Class

**As a** user

**I want to** filter flights by travel class

**So that** I can find flights in my preferred cabin

**Acceptance Criteria:**

- Travel class dropdown with 4 options (Economy, Premium Economy, Business, First)
- Default selection: Economy
- Results filtered to show only selected travel class
- Travel class clearly indicated on each result

**Technical Notes:**

- Pass travelClass parameter to external APIs
- Include travelClass in cache key

**Priority:** P0 | **Story Points:** 3

---

### US-014: Filter by Airline

**As a** user

**I want to** optionally filter by a specific airline

**So that** I can find flights on my preferred carrier

**Acceptance Criteria:**

- Airline dropdown with major carriers
- Default: "All Airlines"
- When airline selected, only flights from that airline shown
- Can clear filter to see all airlines again

**Technical Notes:**

- Pass airline IATA code to external APIs
- Include airline in cache key

**Priority:** P1 | **Story Points:** 3

---

### US-015: Search by Exact Flight Number

**As a** user

**I want to** search for a specific flight by flight number

**So that** I can quickly find the exact flight I'm interested in

**Acceptance Criteria:**

- Flight number field only enabled when airline is selected
- Results show exact match if flight exists
- Error message if flight number not found

**Technical Notes:**

- Pass flightNumber parameter to APIs
- Handle case where flight doesn't exist on searched date

**Priority:** P1 | **Story Points:** 3

---

### US-016: View Seat Map

**As a** user (registered or guest)

**I want to** view the seat map for a specific flight

**So that** I can see which seats are available

**Acceptance Criteria:**

- Seat map displayed as visual grid
- Available seats marked green, unavailable seats red
- Seat class and features indicated
- **Guest users:** Counter incremented after each view, modal appears after 2nd view
- **Registered users:** Unlimited seat map views

**Technical Notes:**

- Query seat map API from source provider
- Cache seat maps for 5 minutes
- Check user role from JWT (guest vs user)
- Increment guestFlightsViewed for guests
- Return 403 after 2 views with upgrade prompt

**Priority:** P0 | **Story Points:** 13

---

## Epic 4: Bookmark Management

### US-017: Bookmark a Flight

**As a** registered user

**I want to** bookmark flights I'm interested in

**So that** I can easily review them later

**Acceptance Criteria:**

- Only registered users can bookmark (NOT guests)
- User can bookmark up to 50 flights
- Visual indicator shows already-bookmarked flights
- Error shown if trying to exceed 50 bookmarks

**Technical Notes:**

- Store in Bookmarks table with userId as PK
- Check bookmark count before allowing new one
- Set TTL to departureDate + 1 day for auto-deletion

**Priority:** P1 | **Story Points:** 5

---

### US-018: View Bookmarked Flights

**As a** registered user

**I want to** see all my bookmarked flights

**So that** I can review and compare my options

**Acceptance Criteria:**

- All active bookmarks displayed
- Sorted by departure date (soonest first)
- Shows days until departure
- Can click to view seat map
- Shows count: "X of 50 bookmarks"

**Technical Notes:**

- Query Bookmarks table by userId
- Calculate daysUntilDeparture dynamically

**Priority:** P1 | **Story Points:** 3

---

### US-019: Remove Bookmark

**As a** registered user

**I want to** remove a bookmark

**So that** I can manage my list of flights

**Acceptance Criteria:**

- Remove button on each bookmark
- Confirmation dialog shown
- Bookmark deleted immediately
- Bookmark count updated

**Technical Notes:**

- Delete from Bookmarks table
- Return updated bookmark count

**Priority:** P1 | **Story Points:** 2

---

## Epic 5: User Experience

### US-027: Convert Guest to Registered User

**As a** guest user who has reached the limit

**I want to** easily create an account

**So that** I can continue using the service

**Acceptance Criteria:**

- Modal appears after 2nd seat map view
- Three options displayed: Register with Email, Google, Apple
- One-click transition from guest to registered
- After registration, can immediately view more seat maps

**Technical Notes:**

- Show modal on 403 response from seat map endpoint
- After successful registration, redirect back to seat map
- Delete guest session after conversion

**Priority:** P0 | **Story Points:** 3

---

### US-028: Clear Guest Limitations Messaging

**As a** guest user

**I want to** understand my limitations clearly

**So that** I'm not surprised when I hit the limit

**Acceptance Criteria:**

- Banner on homepage: "Guest mode: View up to 2 seat maps"
- Counter shown after first view: "1/2 seat maps viewed"
- Counter updates after second view: "2/2 seat maps viewed"
- Benefits of registration clearly listed

**Technical Notes:**

- Return guestLimitsRemaining in API responses
- Update counter in real-time after each view

**Priority:** P1 | **Story Points:** 2

---

## Epic 6: System Reliability & Monitoring

### US-020: Monitor External API Health

**As a** system operator

**I want to** monitor Amadeus and Sabre API availability

**So that** I'm alerted when external services are down

**Acceptance Criteria:**

- Health checks run every 5 minutes
- Success/failure logged in CloudWatch
- Alert triggered after 3 consecutive failures
- Dashboard shows current API status

**Technical Notes:**

- Scheduled Lambda function runs every 5 minutes
- Publish custom CloudWatch metric: `ExternalAPIAvailability`
- Create CloudWatch alarm for consecutive failures

**Priority:** P0 | **Story Points:** 5

---

### US-021: Handle External API Failures Gracefully

**As a** system

**I want to** gracefully handle when external APIs are unavailable

**So that** users still receive partial results when possible

**Acceptance Criteria:**

- If Amadeus fails, show only Sabre results
- If Sabre fails, show only Amadeus results
- If both fail, show clear error message
- Cached results returned if available during outage
- Retry logic with exponential backoff

**Technical Notes:**

- Implement circuit breaker pattern
- Try cache first if API call fails
- Return partial results with source indicator

**Priority:** P0 | **Story Points:** 8

---

### US-022: Alert on High Error Rates

**As a** system operator

**I want to** be alerted when error rates exceed thresholds

**So that** I can investigate and resolve issues quickly

**Acceptance Criteria:**

- Email alert when Lambda error rate > 5% (5-min window)
- Email alert when API Gateway 5xx > 10 requests/min
- Alert includes error type, endpoint, and timestamp
- Dashboard shows error rates in real-time

**Technical Notes:**

- CloudWatch alarm on Lambda errors metric
- CloudWatch alarm on API Gateway 5xx metric
- SNS topic sends email notifications

**Priority:** P0 | **Story Points:** 3

---

### US-023: Cache Flight Search Results

**As a** system

**I want to** cache external API responses

**So that** I reduce costs and improve performance

**Acceptance Criteria:**

- Flight search results cached for 15 minutes
- Seat maps cached for 5 minutes
- Cache key includes all search parameters
- Cache hit returns results in < 100ms
- TTL automatically expires old entries

**Technical Notes:**

- Store in APICache DynamoDB table
- Hash request parameters for cache key
- Target: > 20% cache hit rate

**Priority:** P0 | **Story Points:** 5

---

## Epic 7: Security & Data Protection

### US-024: Secure Password Storage

**As a** security-conscious system

**I want** user passwords to be securely hashed

**So that** user data is protected even if database is compromised

**Acceptance Criteria:**

- Passwords never stored in plain text
- bcrypt used for hashing with salt
- Hash strength appropriate (cost factor 12)
- Password hash never returned in API responses

**Technical Notes:**

- Use bcrypt library with cost factor 12
- Generate unique salt per password

**Priority:** P0 | **Story Points:** 2

---

### US-025: Encrypt Sensitive Data

**As a** system administrator

**I want** sensitive user information encrypted

**So that** user privacy is protected

**Acceptance Criteria:**

- Data encrypted in transit (TLS 1.2+)
- Data encrypted at rest (DynamoDB encryption)
- API credentials stored securely

**Technical Notes:**

- Enable DynamoDB encryption at rest
- Enforce TLS 1.2+ on API Gateway

**Priority:** P0 | **Story Points:** 2

---

### US-026: Rate Limit API Requests

**As a** system

**I want** to rate limit incoming requests

**So that** the service isn't overwhelmed or abused

**Acceptance Criteria:**

- Rate limits enforced at API Gateway level
- 429 status code returned when limit exceeded
- Clear error message explaining rate limit
- Headers indicate remaining requests

**Technical Notes:**

- Configure API Gateway usage plans
- Authentication: 10/min
- Flight search: 30/min
- Seat maps: 20/min

**Priority:** P0 | **Story Points:** 3

---

## Epic 8: Admin & Operations

### US-029: View System Dashboard

**As a** system operator

**I want to** view a real-time dashboard of system health

**So that** I can monitor the service at a glance

**Acceptance Criteria:**

- Dashboard shows: request volume, error rates, API status, Lambda metrics, user count, cache hit rate, subscription metrics
- Dashboard auto-refreshes every 30 seconds
- Accessible only to authorized operators

**Technical Notes:**

- CloudWatch Dashboard with custom widgets
- Custom metrics from application code

**Priority:** P1 | **Story Points:** 5

---

### US-030: Automated Deployment

**As a** developer

**I want** deployments to be fully automated via Jenkins

**So that** I can deploy safely and consistently

**Acceptance Criteria:**

- Push to develop branch triggers Jenkins build
- Jenkins runs all tests (unit, integration, e2e)
- User approval required for production deploy
- Lambda functions updated with zero downtime
- Rollback available if deployment fails

**Technical Notes:**

- Jenkinsfile in repository root
- Use Lambda aliases for blue-green deployment
- Integration with GitHub webhooks

**Priority:** P0 | **Story Points:** 13

---

## Summary

**Total Stories:** 30

**Total Story Points:** ~170

### P0 - Must Have (MVP): 18 stories, ~120 points

### P1 - Should Have: 12 stories, ~50 points