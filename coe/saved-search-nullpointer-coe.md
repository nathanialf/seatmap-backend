# Correction of Error (COE): Saved Search Creation NullPointerException

## Incident Summary
- **Date**: 2025-11-05
- **Severity**: High
- **Impact**: Saved search feature completely broken - all creation attempts fail
- **Status**: INVESTIGATING
- **Duration**: Ongoing since testing began
- **Components Affected**: BookmarkHandler, DynamoDbRepository, Saved Search API

## Timeline of Events
- **2025-11-05 23:30:45**: First saved search creation attempt made during comprehensive bookmark testing
- **2025-11-05 23:30:45**: NullPointerException thrown in DynamoDbRepository.toAttributeValue()
- **2025-11-05 23:30:45**: Usage count still incremented despite failure (showing data corruption potential)
- **2025-11-05 23:30:45**: Investigation initiated through CloudWatch logs analysis

## Problem Statement

### Brief Description
Saved search creation fails with NullPointerException when trying to convert a null value to DynamoDB AttributeValue. The failure occurs after usage limits have been incremented, creating a potential data inconsistency issue.

### Expected vs Actual Behavior
**Expected**:
```json
{
  "bookmarkId": "ss_abc123xyz",
  "userId": "[USER_ID]",
  "title": "Test Saved Search #1 - LAX to SFO Daily",
  "itemType": "SAVED_SEARCH",
  "searchRequest": {...},
  "success": true
}
```

**Actual**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```

## Technical Details

### Exact Command That Triggered the Issue
```bash
curl -X POST https://99v7n3297h.execute-api.us-west-1.amazonaws.com/dev/saved-searches \
     -H "X-API-Key: [API_KEY_REDACTED]" \
     -H "Authorization: Bearer [JWT_TOKEN_REDACTED]" \
     -H "Content-Type: application/json" \
     -d @- << 'EOF'
{
  "title": "Test Saved Search #1 - LAX to SFO Daily",
  "searchRequest": {
    "origin": "LAX",
    "destination": "SFO",
    "departureDate": "2025-11-07",
    "maxResults": 10
  }
}
EOF
```

### Affected Components
1. **BookmarkHandler** - handleCreateSavedSearch method at line 333
2. **DynamoDbRepository** - toAttributeValue method at line 86
3. **BookmarkRepository** - saveBookmark method at line 100
4. **UserUsageLimitsService** - Usage counting (partially working)

### Error Messages/Logs
```
[main] ERROR com.seatmap.auth.handler.BookmarkHandler - Unexpected error in bookmark handler
java.lang.NullPointerException: Cannot invoke "Object.toString()" because "value" is null
	at com.seatmap.common.repository.DynamoDbRepository.toAttributeValue(DynamoDbRepository.java:86)
	at com.seatmap.common.repository.DynamoDbRepository.toAttributeValue(DynamoDbRepository.java:82)
	at com.seatmap.common.repository.DynamoDbRepository.convertToAttributeValueMap(DynamoDbRepository.java:59)
	at com.seatmap.common.repository.DynamoDbRepository.toAttributeValueMap(DynamoDbRepository.java:38)
	at com.seatmap.auth.repository.BookmarkRepository.saveBookmark(BookmarkRepository.java:100)
	at com.seatmap.auth.handler.BookmarkHandler.handleCreateSavedSearch(BookmarkHandler.java:333)
```

### Working Command for Comparison (Regular Bookmark)
```bash
curl -X POST https://99v7n3297h.execute-api.us-west-1.amazonaws.com/dev/bookmarks \
     -H "X-API-Key: [API_KEY_REDACTED]" \
     -H "Authorization: Bearer [JWT_TOKEN_REDACTED]" \
     -H "Content-Type: application/json" \
     -d @- << 'EOF'
{
  "title": "Test Regular Bookmark #1 - LAX to SFO UA560",
  "flightOfferData": "{\"id\":\"6\",\"dataSource\":\"AMADEUS\",\"type\":\"flight-offer\",...}"
}
EOF
```

## Root Cause Hypotheses

### 🔍 Under Investigation
- **Null Field in Saved Search Model**: One of the fields being serialized to DynamoDB is null and not handled properly
- **Missing Field Initialization**: A required field is not being set during saved search creation
- **Data Model Mismatch**: Saved search object structure differs from what DynamoDbRepository expects
- **Jackson Serialization Issue**: JSON deserialization creating null values in unexpected places

### ✅ Confirmed
- **Usage Count Incremented Before Validation**: The system increments usage limits before validating the object can be saved, leading to potential data inconsistency

### ❌ Ruled Out
- **Authentication Issues**: User is properly authenticated (bookmark creation works)
- **DynamoDB Connectivity**: Table is accessible (bookmark creation works)
- **Request Format Issues**: Request follows documented API format

## Investigation Findings

### Analysis Results
1. **Regular bookmarks work perfectly**: Same user, same handler, same repository - only saved searches fail
2. **Usage counting occurs before save validation**: Usage count goes from 1 to 2 even though save fails
3. **Null value at DynamoDbRepository level**: The error occurs in the generic repository layer, not saved search specific code
4. **Stack trace points to line 86**: The issue is in `toAttributeValue()` method when calling `value.toString()`

### Evidence
```bash
# CloudWatch log group: /aws/lambda/seatmap-bookmarks-dev
# Log stream: 2025/11/05/[$LATEST]99cd91765277441b86d66196cf499921

# Successful bookmark creation
[main] INFO com.seatmap.auth.repository.UserUsageRepository - Recorded bookmark creation for user: [USER_ID]. Total this month: 1

# Failed saved search (but usage still incremented)
[main] INFO com.seatmap.auth.repository.UserUsageRepository - Recorded bookmark creation for user: [USER_ID]. Total this month: 2
[main] ERROR com.seatmap.auth.handler.BookmarkHandler - Unexpected error in bookmark handler
```

## Critical Actions Required

### 🚨 Immediate Actions
- [ ] **Examine saved search object construction**: Check what fields are null in BookmarkHandler.handleCreateSavedSearch()
- [ ] **Review DynamoDbRepository.toAttributeValue()**: Identify which field is null and why it's not being handled
- [ ] **Add null safety checks**: Prevent NullPointerException in repository layer
- [ ] **Fix usage counting order**: Move usage increment after successful validation/save

### 🔄 Short-term Actions
- [ ] **Add comprehensive logging**: Log the saved search object before attempting to save
- [ ] **Implement transaction rollback**: Rollback usage count if save fails
- [ ] **Add integration tests**: Test saved search creation end-to-end
- [ ] **Validate all saved search fields**: Ensure no required fields are missing

### 📋 Long-term Actions  
- [ ] **Repository error handling**: Improve error handling in DynamoDbRepository for null values
- [ ] **Model validation**: Add @NotNull annotations and validation to saved search model
- [ ] **Monitoring**: Add metrics for saved search creation success/failure rates

## Business Impact

### User Impact
- **Saved Search Feature Completely Broken**: Users cannot create saved searches, eliminating a key feature for pro/business tier users
- **Affected Users**: All authenticated users attempting to use saved search functionality
- **User Experience**: Feature appears completely non-functional with generic error message

### System Impact
- **Data Integrity**: Usage counts are incremented even when operations fail, leading to incorrect limit tracking
- **Feature Reliability**: Major feature completely non-functional
- **Performance**: No direct performance impact, but failed requests waste resources

### Financial Impact
- **Pro/Business Tier Value**: Paying customers lose access to a premium feature they're paying for
- **Customer Retention Risk**: Users may churn if premium features don't work

## Investigation Priority

**Severity**: High
- **User Experience**: High - Premium feature completely broken
- **System Reliability**: High - Data consistency issues with usage counting  
- **Data Integrity**: High - Usage counts incorrect when saves fail
- **Operational Impact**: Medium - Affects monitoring and limits

**Priority**: High
- Premium feature affecting paying customers is completely non-functional with potential for data corruption in usage tracking

## Code References

### Primary Investigation Targets
- `src/main/java/com/seatmap/common/repository/DynamoDbRepository.java:86` - Where the NPE occurs
- `src/main/java/com/seatmap/auth/handler/BookmarkHandler.java:333` - Saved search creation logic
- `src/main/java/com/seatmap/auth/repository/BookmarkRepository.java:100` - saveBookmark method
- `src/main/java/com/seatmap/common/repository/DynamoDbRepository.java:59` - convertToAttributeValueMap

### Models to Examine
- Saved search model/class definition
- Bookmark model (for comparison with working feature)
- Request/response models for saved search API

## Reproduction Steps

1. **Authenticate as pro user**:
   ```bash
   curl -X POST https://99v7n3297h.execute-api.us-west-1.amazonaws.com/dev/auth/login \
        -H "X-API-Key: [API_KEY]" \
        -H "Content-Type: application/json" \
        -d '{"email": "nathanial+pro@defnf.com", "password": "TestPass123!"}'
   ```

2. **Attempt to create saved search** (fails):
   ```bash
   curl -X POST https://99v7n3297h.execute-api.us-west-1.amazonaws.com/dev/saved-searches \
        -H "X-API-Key: [API_KEY]" \
        -H "Authorization: Bearer [JWT_TOKEN]" \
        -H "Content-Type: application/json" \
        -d '{"title": "Test Search", "searchRequest": {"origin": "LAX", "destination": "SFO", "departureDate": "2025-11-07", "maxResults": 10}}'
   ```

3. **Create regular bookmark** (works):
   ```bash
   curl -X POST https://99v7n3297h.execute-api.us-west-1.amazonaws.com/dev/bookmarks \
        -H "X-API-Key: [API_KEY]" \
        -H "Authorization: Bearer [JWT_TOKEN]" \
        -H "Content-Type: application/json" \
        -d '{"title": "Test Bookmark", "flightOfferData": "{\"id\":\"test\"}"}'
   ```

## Next Steps

1. **Immediate Code Analysis**: Examine the saved search object being created in BookmarkHandler
2. **Add Debug Logging**: Log the complete saved search object before save attempt
3. **Compare with Working Bookmark**: Identify structural differences between bookmark and saved search objects
4. **Fix Null Handling**: Add proper null checking in DynamoDbRepository
5. **Test Thoroughly**: Comprehensive testing of saved search functionality after fixes

---

**COE Document Version**: 1.0  
**Last Updated**: 2025-11-05  
**Prepared By**: Claude Code Assistant  
**Reviewed By**: Pending  
**Status**: INVESTIGATING