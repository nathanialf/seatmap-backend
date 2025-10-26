# Correction of Error (COE) Documentation

This directory contains Correction of Error (COE) documents for investigating and documenting system issues, bugs, and incidents in the Seatmap Backend project.

## Purpose

COE documents serve to:
- Document critical system failures and investigations
- Track root cause analysis progress
- Provide visibility into system reliability issues
- Create institutional knowledge for future reference
- Follow Amazon-style operational excellence practices

## Guidelines

### Sensitive Information
- **DO NOT** include sensitive information such as:
  - API keys, secrets, or credentials
  - User emails or personal information (use `[USER_EMAIL]` placeholder)
  - Internal system details that could compromise security
  - Live/active JWT tokens

### Acceptable Information
- Expired JWT tokens (for analysis purposes)
- Anonymized user identifiers
- System logs (sanitized)
- Code snippets and configuration files
- Timestamp analysis and system behavior
- Infrastructure details (non-sensitive)

## Current Investigations

- **jwt-token-investigation-coe.md** - Critical JWT token timestamp generation issue

## Format

Each COE should include:
1. **Incident Summary** - Date, severity, impact, status
2. **Timeline of Events** - Chronological sequence of what happened
3. **Problem Statement** - Clear description of the issue
4. **Technical Details** - Analysis, evidence, and findings
5. **Root Cause Hypotheses** - Potential causes being investigated
6. **Investigation Findings** - What has been discovered
7. **Critical Actions Required** - Next steps and priorities
8. **Business Impact** - Effect on users and system
9. **Investigation Priority** - Severity and escalation

## Contributing

When creating or updating COE documents:
1. Follow the established format
2. Sanitize all sensitive information
3. Include timestamps in UTC
4. Use clear, technical language
5. Link to relevant code files and line numbers
6. Track investigation progress with checkboxes
7. Update status as investigation progresses