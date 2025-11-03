# Tiers API

## Overview

Retrieve tier definitions and pricing information for the SeatMap application. **Requires API key authentication (no user authentication needed).**

---

## Get All Tiers

Retrieve all publicly accessible, active tier definitions.

**Endpoint**: `GET /tiers`

**Query Parameters** (optional):
- `region`: Filter tiers by region (e.g., `US`, `EU`) - case insensitive

**Headers**:
```
X-API-Key: your_api_key
```

**Response**:
```json
{
  "success": true,
  "data": {
    "tiers": [
      {
        "tierId": "tier_free",
        "tierName": "FREE",
        "displayName": "Free Plan",
        "description": "Basic access for occasional travelers",
        "maxBookmarks": 0,
        "maxSeatmapCalls": 10,
        "priceUsd": 0.00,
        "billingType": "free",
        "canDowngrade": true,
        "publiclyAccessible": true,
        "region": "US",
        "active": true,
        "createdAt": "2025-10-15T09:30:00Z",
        "updatedAt": "2025-10-30T08:15:00Z"
      },
      {
        "tierId": "tier_pro",
        "tierName": "PRO",
        "displayName": "Pro Plan",
        "description": "Enhanced limits for frequent travelers",
        "maxBookmarks": 50,
        "maxSeatmapCalls": 1000,
        "priceUsd": 9.99,
        "billingType": "monthly",
        "canDowngrade": true,
        "publiclyAccessible": true,
        "region": "US",
        "active": true,
        "createdAt": "2025-10-15T09:30:00Z",
        "updatedAt": "2025-10-30T08:15:00Z"
      },
      {
        "tierId": "tier_business",
        "tierName": "BUSINESS",
        "displayName": "Business Plan",
        "description": "Unlimited access with priority support",
        "maxBookmarks": null,
        "maxSeatmapCalls": null,
        "priceUsd": 49.99,
        "billingType": "one_time",
        "canDowngrade": false,
        "publiclyAccessible": true,
        "region": "US",
        "active": true,
        "createdAt": "2025-10-15T09:30:00Z",
        "updatedAt": "2025-10-30T08:15:00Z"
      }
    ],
    "total": 3,
    "region": "US"
  }
}
```

**Tier Fields**:
- `tierId`: Unique tier identifier
- `tierName`: Tier name (FREE, PRO, BUSINESS)
- `displayName`: Human-readable tier name
- `description`: Tier description for customers
- `maxBookmarks`: Maximum bookmarks allowed (null = unlimited)
- `maxSeatmapCalls`: Maximum seat map API calls per month (null = unlimited)
- `priceUsd`: Price in USD
- `billingType`: Billing frequency (free, monthly, annual, one_time)
- `canDowngrade`: Whether users can downgrade from this tier
- `publiclyAccessible`: Whether tier is available to public (always true in response)
- `region`: Geographic region (US, EU, etc.)
- `active`: Whether tier is currently available (always true in response)
- `createdAt`: Tier creation timestamp
- `updatedAt`: Last tier update timestamp

**Response Fields**:
- `tiers`: Array of tier objects
- `total`: Number of tiers returned
- `region`: Region filter applied (only present when region parameter used)

**Example cURL**:
```bash
# Get all tiers
curl -X GET {BASE_URL}/tiers \
  -H "X-API-Key: {YOUR_API_KEY}"

# Get tiers for specific region
curl -X GET {BASE_URL}/tiers?region=US \
  -H "X-API-Key: {YOUR_API_KEY}"

# Case insensitive region filtering
curl -X GET {BASE_URL}/tiers?region=eu \
  -H "X-API-Key: {YOUR_API_KEY}"
```

---

## Get Tier by Name

Retrieve specific tier details by tier name.

**Endpoint**: `GET /tiers/{tierName}`

**Path Parameters**:
- `tierName`: Tier name (case-insensitive: FREE, PRO, BUSINESS)

**Headers**:
```
X-API-Key: your_api_key
```

**Response**:
```json
{
  "success": true,
  "data": {
    "tierId": "tier_pro",
    "tierName": "PRO",
    "displayName": "Pro Plan",
    "description": "Enhanced limits for frequent travelers",
    "maxBookmarks": 50,
    "maxSeatmapCalls": 1000,
    "priceUsd": 9.99,
    "billingType": "monthly",
    "canDowngrade": true,
    "publiclyAccessible": true,
    "region": "US",
    "active": true,
    "createdAt": "2025-10-15T09:30:00Z",
    "updatedAt": "2025-10-30T08:15:00Z"
  }
}
```

**Example cURL**:
```bash
curl -X GET {BASE_URL}/tiers/PRO \
  -H "X-API-Key: {YOUR_API_KEY}"

# Case insensitive
curl -X GET {BASE_URL}/tiers/pro \
  -H "X-API-Key: {YOUR_API_KEY}"
```

---

## Tier Types

### FREE Tier
- **Flight Search**: Full access
- **Seat Maps**: Limited monthly quota (10 calls)
- **Bookmarks**: Not available (0 bookmarks)
- **Price**: Free
- **Target**: Occasional travelers

### PRO Tier
- **Flight Search**: Full access
- **Seat Maps**: Enhanced monthly limits (1,000 calls)
- **Bookmarks**: Monthly quota (50 bookmarks)
- **Price**: $9.99/month
- **Target**: Frequent travelers

### BUSINESS Tier
- **Flight Search**: Full access
- **Seat Maps**: Unlimited access
- **Bookmarks**: Unlimited
- **Price**: $49.99 one-time
- **Features**: Priority support, beta access
- **Target**: Professional travel managers
- **Note**: Permanent upgrade (cannot be downgraded)

---

## Usage Notes

### Public Access
- Tier information is publicly accessible for pricing displays
- No user authentication required (only API key)
- Perfect for frontend pricing pages and plan comparisons

### Filtering
- Only returns tiers with `publiclyAccessible: true`
- Only returns tiers with `active: true`
- Dev-only tiers are automatically filtered out
- **Region filtering**: Use `?region=US` to filter by specific region
- **All regions**: Omit region parameter to get tiers from all regions

### Rate Limiting
- Protected by API Gateway throttling via API key
- Recommended for frontend caching due to infrequent tier changes

### Frontend Integration
- Use `/tiers` for pricing comparison tables
- Use `/tiers/{tierName}` for specific plan details
- Cache responses for improved performance

---

## Error Responses

**400 Bad Request** (Empty tier name):
```json
{
  "success": false,
  "message": "Tier name is required"
}
```

**403 Forbidden** (Missing/Invalid API Key):
```json
{
  "success": false,
  "message": "Forbidden"
}
```

**404 Not Found** (Tier not found/inactive/private):
```json
{
  "success": false,
  "message": "Tier not found"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```

---

## Security Notes

- **API Key Required**: All tier operations require valid API key
- **Public Data**: Tier information is considered public (pricing, features)
- **No Sensitive Data**: No internal business logic or cost structures exposed
- **Rate Protected**: API Gateway throttling prevents abuse
- **CORS Enabled**: Supports frontend cross-origin requests