# Correction of Error (COE): [INCIDENT_TITLE]

## Incident Summary
- **Date**: [YYYY-MM-DD]
- **Severity**: [Low/Medium/High/Critical]
- **Impact**: [Brief description of impact]
- **Status**: [INVESTIGATING/IN_PROGRESS/RESOLVED/CLOSED]
- **Duration**: [Time to resolve or ongoing]
- **Components Affected**: [List of affected components]

## Timeline of Events
- **[Timestamp]**: [Event description]
- **[Timestamp]**: [Event description]
- **[Timestamp]**: [Event description]

## Problem Statement

### Brief Description
[Clear, concise description of the problem]

### Expected vs Actual Behavior
**Expected**:
```
[What should happen]
```

**Actual**:
```
[What actually happened]
```

## Technical Details

### Affected Components
1. **[Component Name]** - [Description]
2. **[Component Name]** - [Description]

### Error Messages/Logs
```
[Sanitized error messages and relevant logs]
```

### Code References
```java
// [File path]:[line number] - [Description]
[Code snippet]
```

## Root Cause Hypotheses

### 🔍 Under Investigation
- **[Hypothesis 1]**: [Description and reasoning]
- **[Hypothesis 2]**: [Description and reasoning]

### ✅ Confirmed
- **[Root Cause]**: [Detailed explanation]

### ❌ Ruled Out
- **[Ruled Out Cause]**: [Why it was ruled out]

## Investigation Findings

### Analysis Results
1. **[Finding 1]**: [Description]
2. **[Finding 2]**: [Description]

### Evidence
```bash
# Commands used for investigation
[command]

# Results
[output]
```

## Critical Actions Required

### 🚨 Immediate Actions
- [ ] **[Action 1]**: [Description and priority]
- [ ] **[Action 2]**: [Description and priority]

### 🔄 Short-term Actions
- [ ] **[Action 1]**: [Description and timeline]
- [ ] **[Action 2]**: [Description and timeline]

### 📋 Long-term Actions  
- [ ] **[Action 1]**: [Description and timeline]
- [ ] **[Action 2]**: [Description and timeline]

## Business Impact

### User Impact
- **[Impact Type]**: [Description of user experience impact]
- **Affected Users**: [Number/percentage of affected users]

### System Impact
- **[Impact Type]**: [Description of system-level impact]
- **Performance**: [Any performance degradation]

### Financial Impact
- **[Impact Type]**: [If applicable, estimate of financial impact]

## Investigation Priority

**Severity**: [Low/Medium/High/Critical]
- **User Experience**: [Impact level and description]
- **System Reliability**: [Impact level and description]  
- **Data Integrity**: [Impact level and description]
- **Operational Impact**: [Impact level and description]

**Priority**: [Low/Medium/High/Critical]
- [Justification for priority level]

## Code Changes

### Files Modified
1. **[File Path]** - [Description of changes]
2. **[File Path]** - [Description of changes]

### Files Created
1. **[File Path]** - [Purpose and description]
2. **[File Path]** - [Purpose and description]

### Dependencies Added/Removed
```java
// Added
import [package.name];

// Removed  
import [package.name];
```

## Testing & Validation

### Test Strategy
- [ ] **Unit Tests**: [Description]
- [ ] **Integration Tests**: [Description]
- [ ] **Regression Tests**: [Description]

### Test Results
```bash
# Test execution
./gradlew test

# Results summary
[Pass/fail counts and any notable results]
```

## Post-Incident Actions

### ✅ Completed
- [x] [Action description]
- [x] [Action description]

### 🔄 In Progress
- [ ] [Action description] - [Timeline]
- [ ] [Action description] - [Timeline]

### 📋 Planned
- [ ] [Action description] - [Timeline]
- [ ] [Action description] - [Timeline]

## Lessons Learned

1. **[Lesson 1]**: [Description and implications]
2. **[Lesson 2]**: [Description and implications]
3. **[Lesson 3]**: [Description and implications]

## Prevention Measures

### Immediate
- **[Measure 1]**: [Description]
- **[Measure 2]**: [Description]

### Long-term
- **[Measure 1]**: [Description]
- **[Measure 2]**: [Description]

## Monitoring & Alerting

### Metrics to Track
- **[Metric 1]**: [Description and threshold]
- **[Metric 2]**: [Description and threshold]

### Alerts to Implement
- **[Alert 1]**: [Condition and notification target]
- **[Alert 2]**: [Condition and notification target]

## References

- **Related COE Documents**: [Links to related investigations]
- **Documentation**: [Links to relevant documentation]
- **Code References**: [Links to relevant code sections]
- **External Resources**: [Links to external documentation/resources]

## Communication

### Stakeholders Notified
- [ ] **Engineering Team**: [Date/time notified]
- [ ] **Product Team**: [Date/time notified]
- [ ] **Operations Team**: [Date/time notified]

### Status Updates
- **[Date/Time]**: [Update content and audience]
- **[Date/Time]**: [Update content and audience]

---

**COE Document Version**: [X.X]  
**Last Updated**: [YYYY-MM-DD]  
**Prepared By**: [Name/Role]  
**Reviewed By**: [Name/Role]  
**Status**: [INVESTIGATING/IN_PROGRESS/RESOLVED/CLOSED]

---

## COE Document Guidelines

### Sensitive Information Reminders
- **DO NOT** include:
  - API keys, secrets, or credentials
  - User emails or personal information (use `[USER_EMAIL]` placeholder)
  - Live/active JWT tokens
  - Internal system details that could compromise security

### Acceptable Information
- Expired JWT tokens (for analysis purposes)
- Anonymized user identifiers  
- System logs (sanitized)
- Code snippets and configuration files
- Timestamp analysis and system behavior
- Infrastructure details (non-sensitive)