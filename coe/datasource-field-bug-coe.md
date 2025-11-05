# Correction of Error (COE): Flight Search DataSource Field Bug

## Incident Summary
- **Date**: November 5, 2025
- **Severity**: Medium (API Functionality Impact)
- **Impact**: Multi-provider flight search routing failures, bookmark endpoint failures
- **Status**: ✅ RESOLVED
- **Duration**: Investigation and fix completed in 1 session
- **Components Affected**: FlightSearchHandler, AmadeusService, SabreService, BookmarkHandler

## Timeline of Events
- **Initial Discovery**: Bug documented in `docs-private/testing-bugs-found.md`
- **Bug Analysis**: Two related issues identified (BUG-001 and BUG-002)
- **Root Cause Identified**: Missing dataSource field population in service layer
- **Fix Implemented**: Added dataSource field setting in both provider services
- **Validation Added**: Made dataSource field required with fail-fast validation
- **Testing Complete**: 433 tests passing, comprehensive regression testing

## Problem Statement

The flight search API responses contained inconsistent and empty `dataSource` field values, causing:

1. **BUG-001**: Flight search responses returning empty `dataSource` instead of provider name
2. **BUG-002**: Bookmark endpoint failures due to inability to route to correct provider

### Expected vs Actual Behavior

**Expected Response Structure**:
```json
{
  "dataSource": "AMADEUS",     // Provider-specific identifier
  "source": "GDS",             // Generic system type
  "seatMap": {
    "source": "AMADEUS"        // Provider-specific for seatmap
  }
}
```

**Actual Broken Response**:
```json
{
  "dataSource": "",            // ❌ Empty string
  "source": "GDS",             // ✅ Correct
  "seatMap": {
    "source": "AMADEUS"        // ✅ Correct
  }
}
```

## Technical Details

### Affected Components
1. **FlightSearchHandler.java** - Multi-provider aggregation logic
2. **AmadeusService.java:124** - `buildFlightSearchResult()` method
3. **SabreService.java:190** - `buildFlightSearchResult()` method
4. **FlightSearchResult.java:38** - Constructor extracting dataSource
5. **BookmarkHandler** - Provider routing logic

### Code Analysis

#### Root Cause Location
```java
// FlightSearchResult.java:38 - BEFORE FIX
this.dataSource = flightOffer.path("dataSource").asText(); // Returns "" if missing

// AmadeusService.java:124 - BEFORE FIX  
return new FlightSearchResult(offer, seatMapData, true, null); // Raw offer missing dataSource

// SabreService.java:190 - BEFORE FIX
return new FlightSearchResult(flight, seatMapData, true, null); // Raw flight missing dataSource
```

#### Provider Routing Failure
```java
// FlightSearchHandler.java:338 - Error Location
private String determineDataSource(JsonNode flightOffer) {
    return flightOffer.path("dataSource").asText("AMADEUS"); // Default to AMADEUS if not found
}

// FlightSearchHandler.java:338 - Bookmark Routing
Exception: Unknown data source: 
    at FlightSearchHandler.fetchFlightWithFreshSeatmap(FlightSearchHandler.java:338)
```

### Data Flow Analysis

1. **Amadeus/Sabre API Response** → Raw JSON (no dataSource field)
2. **Service Layer Processing** → FlightSearchResult creation 
3. **Field Extraction** → `dataSource` extracted as empty string
4. **API Response** → Empty dataSource returned to client
5. **Bookmark Storage** → Flight data saved with empty dataSource
6. **Bookmark Retrieval** → Routing fails due to unknown provider

## Root Cause Hypotheses

### ✅ Confirmed Root Cause
**Missing Service Layer Data Enhancement**: The AmadeusService and SabreService were creating FlightSearchResult objects directly from raw API responses without adding the provider-specific `dataSource` field that's required for multi-provider routing.

### ❌ Ruled Out Causes
- API Gateway transformation issues
- Database serialization problems
- Frontend parsing errors
- Authentication/authorization issues

## Investigation Findings

### Code Review Findings
1. **FlightSearchResult Constructor**: Directly extracts `dataSource` from JsonNode without validation
2. **Service Layer Gap**: Neither AmadeusService nor SabreService add provider identification
3. **Inconsistent Field Population**: `seatMap.source` correctly set, but `dataSource` not set
4. **Silent Failure Pattern**: Empty string returned instead of failing fast

### Testing Evidence
```bash
# Test Results - Before Fix
FlightSearchResultTest > testConstructorRequiresDataSource() FAILED
DataSourceIntegrationTest > testPreventEmptyDataSourceRegression() FAILED

# Test Results - After Fix  
FlightSearchResultTest > testConstructorRequiresDataSource() PASSED
DataSourceIntegrationTest > testPreventEmptyDataSourceRegression() PASSED
```

## Critical Actions Required

### ✅ Immediate Actions (Completed)

1. **Fix Service Layer Data Enhancement**
   ```java
   // AmadeusService.java - AFTER FIX
   ObjectNode offerWithDataSource = offer.deepCopy();
   offerWithDataSource.put("dataSource", "AMADEUS");
   return new FlightSearchResult(offerWithDataSource, seatMapData, true, null);
   ```

2. **Add Constructor Validation**
   ```java
   // FlightSearchResult.java - AFTER FIX
   if (!flightOffer.has("dataSource") || flightOffer.get("dataSource").asText().trim().isEmpty()) {
       throw new IllegalArgumentException("Flight offer must contain a valid dataSource field for provider routing");
   }
   ```

3. **Add Model-Level Validation**
   ```java
   @NotNull(message = "dataSource is required for provider routing")
   @NotBlank(message = "dataSource cannot be blank")
   private String dataSource;
   ```

### ✅ Testing & Validation (Completed)

4. **Comprehensive Test Coverage**
   - 8 new tests in `FlightSearchResultTest.java`
   - 5 new tests in `DataSourceIntegrationTest.java`
   - All 433 existing tests still passing

5. **Regression Prevention**
   - Fail-fast validation prevents future occurrences
   - Required field annotations ensure data integrity
   - Integration tests verify end-to-end functionality

## Business Impact

### Before Fix
- **API Inconsistency**: Consumers couldn't determine flight data provider
- **Bookmark Failures**: Saved flights couldn't be retrieved with fresh data
- **Multi-Provider Transparency**: Broken aggregation intelligence
- **Operational Confusion**: Difficult to troubleshoot provider-specific issues

### After Fix
- **Clear Provider Attribution**: API responses clearly identify AMADEUS vs SABRE
- **Reliable Bookmarks**: Saved flights can be refreshed with latest data
- **Robust Routing**: System properly routes requests to correct provider
- **Operational Clarity**: Easy to track which provider returned specific flights

## Investigation Priority

**Severity**: Medium
- **User Experience**: Bookmark functionality broken (high user impact)
- **System Reliability**: Multi-provider routing unreliable
- **Data Integrity**: Inconsistent API response structure
- **Operational Impact**: Difficult troubleshooting and monitoring

**Priority**: High (Fixed Immediately)
- Critical for multi-provider flight search functionality
- Required for bookmark/saved flight features
- Foundation for reliable provider routing

## Code Changes Summary

### Files Modified
1. **AmadeusService.java** - Added dataSource field population
2. **SabreService.java** - Added dataSource field population  
3. **FlightSearchResult.java** - Added validation and annotations

### Files Created
1. **FlightSearchResultTest.java** - Model validation tests
2. **DataSourceIntegrationTest.java** - Service integration tests

### Import Dependencies Added
```java
import com.fasterxml.jackson.databind.node.ObjectNode;  // For JSON manipulation
import jakarta.validation.constraints.NotBlank;         // For validation
import jakarta.validation.constraints.NotNull;          // For validation
```

## Post-Incident Actions

### ✅ Completed
- [x] Root cause analysis and documentation
- [x] Fix implementation and testing
- [x] Comprehensive regression testing
- [x] Code review and validation
- [x] Documentation of fix in COE

### 🔄 Future Considerations
- [ ] Monitor API response consistency in production
- [ ] Consider adding provider-specific metrics/logging
- [ ] Evaluate need for additional field validation across models
- [ ] Review other multi-provider integration points for similar issues

## Lessons Learned

1. **Service Layer Responsibility**: External API responses need enhancement for internal consistency
2. **Fail-Fast Validation**: Critical fields should validate at construction time
3. **Field Consistency**: Related fields (dataSource, source, seatMap.source) need clear documentation
4. **Integration Testing**: Multi-provider scenarios require comprehensive test coverage
5. **Documentation Value**: Clear bug documentation accelerated resolution

## References

- **Bug Documentation**: `docs-private/testing-bugs-found.md` (BUG-001, BUG-002)
- **API Documentation**: `docs/api/flight-search.md:195`
- **Test Files**: `src/test/java/com/seatmap/api/model/FlightSearchResultTest.java`
- **Integration Tests**: `src/test/java/com/seatmap/api/service/DataSourceIntegrationTest.java`

---

**COE Document Version**: 1.0  
**Last Updated**: November 5, 2025  
**Status**: RESOLVED ✅