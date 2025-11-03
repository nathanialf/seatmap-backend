# Seat Map API

## Overview

Retrieve detailed seat maps for flights with pricing and availability information. Supports both direct flight offer data and bookmark-based retrieval.

---

## Get Seat Map by Flight Offer

Retrieve seat map using complete flight offer data from flight search results.

**Endpoint**: `POST /seat-map`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_jwt_token
Content-Type: application/json
```

**Request Body**:
```json
{
  "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
}
```

**Parameters**:
- `flightOfferData` (required): JSON string of complete flight offer object exactly as received from flight search

**⚠️ CRITICAL**: The `flightOfferData` must include the COMPLETE flight offer object with ALL fields intact, including:
- `travelerPricings` array (MANDATORY for Amadeus API)
- All pricing details and fare segments  
- Complete itinerary and segment data
- All metadata fields

Omitting ANY field from the original flight offer will cause the seat map request to fail with a 400 error.

**Response**:
```json
{
  "success": true,
  "data": {
    "type": "seatmap",
    "segmentId": "1",
    "carrierCode": "AA",
    "flightNumber": "123",
    "aircraft": {
      "code": "321",
      "name": "AIRBUS A321"
    },
    "departure": {
      "iataCode": "LAX",
      "at": "2025-12-15T08:00:00"
    },
    "arrival": {
      "iataCode": "JFK", 
      "at": "2025-12-15T16:35:00"
    },
    "class": {
      "code": "Y",
      "name": "ECONOMY"
    },
    "deck": {
      "deckType": "MAIN",
      "deckConfiguration": {
        "width": 6,
        "length": 30
      },
      "facilities": [
        {
          "code": "1A",
          "type": "SEAT",
          "coordinates": {
            "x": 1,
            "y": 1
          },
          "travelerPricing": [
            {
              "travelerId": "1",
              "seatAvailabilityStatus": "AVAILABLE",
              "price": {
                "currency": "USD",
                "total": "25.00",
                "base": "25.00"
              }
            }
          ],
          "characteristicsCodes": ["CH", "W"]
        }
      ]
    }
  }
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/seat-map \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "flightOfferData": "{\"type\":\"flight-offer\",\"dataSource\":\"AMADEUS\",\"source\":\"GDS\",\"instantTicketingRequired\":false,\"nonHomogeneous\":false,\"oneWay\":false,\"lastTicketingDate\":\"2025-12-10\",\"numberOfBookableSeats\":9,\"itineraries\":[{\"duration\":\"PT5H35M\",\"segments\":[{\"departure\":{\"iataCode\":\"LAX\",\"terminal\":\"4\",\"at\":\"2025-12-15T08:00:00\"},\"arrival\":{\"iataCode\":\"JFK\",\"terminal\":\"8\",\"at\":\"2025-12-15T16:35:00\"},\"carrierCode\":\"AA\",\"number\":\"123\",\"aircraft\":{\"code\":\"321\"},\"operating\":{\"carrierCode\":\"AA\"},\"duration\":\"PT5H35M\",\"id\":\"1\",\"numberOfStops\":0,\"blacklistedInEU\":false}]}],\"price\":{\"currency\":\"USD\",\"total\":\"299.00\",\"base\":\"249.00\"}}"
  }'
```

---

## Get Seat Map by Bookmark

Retrieve seat map directly using a saved bookmark ID. **Requires user authentication (not guest tokens).**

**Endpoint**: `GET /seat-map/bookmark/{bookmarkId}`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_user_jwt_token
```

**Path Parameters**:
- `bookmarkId` (required): Unique bookmark identifier

**Response**:
```json
{
  "success": true,
  "data": {
    "type": "seatmap",
    "segmentId": "1",
    "carrierCode": "AA",
    "flightNumber": "123",
    "aircraft": {
      "code": "321",
      "name": "AIRBUS A321"
    },
    "departure": {
      "iataCode": "LAX",
      "at": "2025-12-15T08:00:00"
    },
    "arrival": {
      "iataCode": "JFK",
      "at": "2025-12-15T16:35:00"
    },
    "class": {
      "code": "Y", 
      "name": "ECONOMY"
    },
    "deck": {
      "deckType": "MAIN",
      "deckConfiguration": {
        "width": 6,
        "length": 30
      },
      "facilities": [
        {
          "code": "1A",
          "type": "SEAT",
          "coordinates": {
            "x": 1,
            "y": 1
          },
          "travelerPricing": [
            {
              "travelerId": "1", 
              "seatAvailabilityStatus": "AVAILABLE",
              "price": {
                "currency": "USD",
                "total": "25.00",
                "base": "25.00"
              }
            }
          ],
          "characteristicsCodes": ["CH", "W"]
        }
      ]
    }
  }
}
```

**Example cURL**:
```bash
curl -X GET {BASE_URL}/seat-map/bookmark/bm_abc123xyz \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_USER_JWT_TOKEN}"
```

---

## Seat Map Data Structure

### Aircraft Information
- `aircraft.code`: Aircraft type code (e.g., "321" for Airbus A321)
- `aircraft.name`: Full aircraft name

### Flight Details  
- `carrierCode`: Airline code (e.g., "AA" for American Airlines)
- `flightNumber`: Flight number
- `departure/arrival`: Airport and timing information

### Deck Configuration
- `deckType`: Aircraft deck (`MAIN`, `UPPER`)
- `deckConfiguration.width`: Number of seat columns
- `deckConfiguration.length`: Number of seat rows

### Seat Facilities
Each seat includes:
- `code`: Seat identifier (e.g., "1A", "12F")
- `coordinates`: Position on seat map (x=column, y=row)
- `seatAvailabilityStatus`: `AVAILABLE`, `OCCUPIED`, `BLOCKED`
- `price`: Seat selection fee if applicable
- `characteristicsCodes`: Seat features

### Seat Characteristics Codes
Common codes include:
- `W`: Window seat
- `A`: Aisle seat  
- `CH`: Seat suitable for child
- `EX`: Exit row seat
- `LEG`: Extra legroom
- `REC`: Reclining seat
- `UP`: Upper deck seat

---

## Data Sources and Routing

Seat maps are automatically routed to the correct provider based on the flight's `dataSource` field in the flight offer data:

### Amadeus Seat Maps
- **Source**: Flights with `dataSource: "AMADEUS"`
- **API**: Amadeus SeatMap API
- **Coverage**: Major airlines and aircraft types

### Sabre Seat Maps  
- **Source**: Flights with `dataSource: "SABRE"`
- **API**: Sabre seat map services
- **Coverage**: Additional airline coverage

The API automatically handles routing based on the `dataSource` field - no manual provider selection needed.

---

## Rate Limiting

Seat map access varies by user type:

### Guest Users
- **Limits**: Restricted access based on IP and timeframe
- **Duration**: 30-day tracking period
- **Reset**: Automatic cleanup after 30 days

### Registered Users
- **Limits**: Monthly quotas based on account tier (FREE, PRO, BUSINESS, DEV)
- **Features**: Tier-based bookmark access, enhanced limits
- **Tracking**: Per-user monthly usage counters
- **Reset**: Monthly limits reset on the 1st of each month

---

## Integration with Flight Search

**Recommended Workflow**:

1. **Search Flights**: Use `/flight-offers` to find available flights
2. **Save Flight Data**: Store the complete flight offer object from the response
3. **Get Seat Map**: Pass the entire flight offer object as `flightOfferData` to `/seat-map` 
4. **Optional Bookmark**: Save flight as bookmark for easier future access
5. **Bookmark Seat Map**: Use `/seat-map/bookmark/{id}` for quick retrieval

**Important**: Always use the complete flight offer object exactly as received from the flight search API. The seat map service extracts all necessary information (including data source routing) from this object.

---

## Error Responses

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Flight offer data is required"
}
```

**401 Unauthorized**:
```json
{
  "success": false,
  "message": "Invalid or expired token"
}
```

**404 Not Found** (Bookmark endpoint):
```json
{
  "success": false,
  "message": "Bookmark not found or access denied"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Error retrieving seat map data"
}
```