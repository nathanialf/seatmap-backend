# API Overview

## Authentication Requirements

All API requests require **both** an API Key and appropriate JWT tokens:

### API Key (Required for ALL requests)
- **Header**: `X-API-Key`
- **Value**: Your assigned API key
- **Scope**: Required for every single API call

### JWT Bearer Token (Required for most endpoints)
- **Header**: `Authorization`
- **Value**: `Bearer {your_jwt_token}`
- **Types**: Guest tokens or User tokens

## User Types & Access Levels

### Guest Users
- **Access**: Limited trial access
- **Duration**: Temporary tokens 
- **Limitations**: Restricted seat map views (varies by IP/timeframe)
- **Features**: Flight search, limited seat map access

### Registered Users
- **Access**: Tiered access levels (FREE, PRO, BUSINESS)
- **Duration**: Extended token validity
- **Features**: Tier-based API access including bookmarks and enhanced limits
- **Tiers**: Monthly usage quotas and features vary by account tier

## Rate Limiting

Rate limits vary by:
- **User type** (Guest vs Registered)
- **Account tier** (FREE, PRO, BUSINESS for registered users) 
- **Endpoint category** (bookmarks, seat maps, etc.)
- **Time windows** (monthly limits reset on the 1st)

### Tier-Based Limits
- **FREE Tier**: Basic monthly quotas for core functionality
- **PRO Tier**: Enhanced monthly limits for regular users
- **BUSINESS Tier**: Unlimited access for premium users

Rate limit details and current usage can be checked via the user profile endpoint. Specific limits are provided in individual endpoint documentation.

## Data Sources

The API integrates with multiple flight data providers:
- **Amadeus**: Primary flight data provider
- **Sabre**: Secondary provider for specific routes/carriers

Flight responses include a `dataSource` field indicating the provider. Seat map requests are automatically routed to the appropriate service based on the flight data source.

## Request/Response Format

### Request Headers (Standard)
```
X-API-Key: your_api_key
Authorization: Bearer your_jwt_token  
Content-Type: application/json
```

### Response Format (Standard)
```json
{
  "success": boolean,
  "data": object,
  "message": "string",
  "meta": object
}
```

## Environment Endpoints

Contact your administrator for:
- Base API URLs
- API key credentials  
- Environment-specific configuration
- Rate limit specifications

## Data Retention

- **Guest sessions**: Temporary storage during session
- **User data**: Persistent storage with automatic cleanup
- **Bookmarks**: Auto-expire based on flight departure dates
- **Search history**: Varies by user tier and retention policies