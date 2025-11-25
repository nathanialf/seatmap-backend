# Flight Search API

## Overview

Search for flight offers from Amadeus with **integrated seat map data**. Results provide a clean, unified format with flight pricing, details, and embedded seat map availability. Raw API data can optionally be included for debugging or advanced use cases.

---

## Search Flight Offers

Search for available flight offers with flexible filtering options.

**Endpoint**: `POST /flight-search`

**Headers**:
```
X-API-Key: your_api_key
Authorization: Bearer your_jwt_token
Content-Type: application/json
```

**Request Body**:
```json
{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-12-15",
  "travelClass": "ECONOMY",
  "airlineCode": "AA",
  "flightNumber": "123",
  "maxResults": 10,
  "offset": 0,
  "includeRawFlightOffer": false
}
```

**Parameters**:
- `origin` (required): 3-letter IATA airport code for departure
- `destination` (required): 3-letter IATA airport code for arrival  
- `departureDate` (required): Date in YYYY-MM-DD format
- `travelClass` (optional): Cabin class filter - `ECONOMY`, `PREMIUM_ECONOMY`, `BUSINESS`, `FIRST`
- `airlineCode` (optional): 2-3 letter airline code (e.g., "AA", "UA", "DL")
- `flightNumber` (optional): 1-4 digit flight number (e.g., "123", "1679") - requires `airlineCode`
- `maxResults` (optional): Results per page (1-20, default: 10)
- `offset` (optional): Starting point for pagination (0-100, default: 0)
- `includeRawFlightOffer` (optional): Include raw flight offer data from API (default: false)

**Response**:
```json
{
  "data": [
    {
      "id": "amadeus_1",
      "dataSource": "AMADEUS",
      "type": "flight-offer",
      "source": "GDS",
      "instantTicketingRequired": false,
      "nonHomogeneous": false,
      "oneWay": false,
      "lastTicketingDate": "2025-12-10",
      "numberOfBookableSeats": 9,
      "itineraries": [
        {
          "duration": "PT5H35M",
          "segments": [
            {
              "departure": {
                "iataCode": "LAX",
                "terminal": "4", 
                "at": "2025-12-15T08:00:00"
              },
              "arrival": {
                "iataCode": "JFK",
                "terminal": "8",
                "at": "2025-12-15T16:35:00"
              },
              "carrierCode": "AA",
              "number": "123",
              "aircraft": {
                "code": "321"
              },
              "operating": {
                "carrierCode": "AA"
              },
              "duration": "PT5H35M",
              "id": "1",
              "numberOfStops": 0
            }
          ]
        }
      ],
      "price": {
        "currency": "USD",
        "total": "299.00",
        "base": "249.00",
        "fees": [
          {
            "amount": "50.00",
            "type": "SUPPLIER"
          }
        ],
        "grandTotal": "299.00"
      },
      "validatingAirlineCodes": ["AA"],
      "travelerPricings": [
        {
          "travelerId": "1",
          "fareOption": "STANDARD",
          "travelerType": "ADULT",
          "price": {
            "currency": "USD",
            "total": "299.00",
            "base": "249.00"
          },
          "fareDetailsBySegment": [
            {
              "segmentId": "1",
              "cabin": "ECONOMY",
              "fareBasis": "UUA0AFNN",
              "brandedFare": "BASIC",
              "class": "U",
              "includedCheckedBags": {
                "quantity": 0
              }
            }
          ]
        }
      ],
      "seatMapAvailable": true,
      "seatMap": {
        "source": "AMADEUS",
        "aircraft": {"code": "321", "name": "AIRBUS A321"},
        "flight": {"number": "123", "carrierCode": "AA"},
        "decks": [
          {
            "deckType": "MAIN",
            "seats": [
              {
                "number": "12A",
                "cabin": "ECONOMY",
                "availabilityStatus": "AVAILABLE",
                "pricing": {
                  "currency": "USD",
                  "total": "25.00",
                  "base": "22.00"
                },
                "characteristics": [
                  {
                    "code": "W",
                    "category": "POSITION",
                    "description": "Window seat",
                    "isRestriction": false,
                    "isPremium": false
                  }
                ]
              }
            ]
          }
        ]
      },
      "seatMapError": null
    }
  ],
  "meta": {
    "count": 1,
    "sources": "AMADEUS",
    "pagination": {
      "offset": 0,
      "limit": 10,
      "total": -1,
      "hasNext": false,
      "hasPrevious": false
    }
  },
  "dictionaries": {
    "locations": {
      "LAX": {
        "cityCode": "LAX",
        "countryCode": "US"
      },
      "JFK": {
        "cityCode": "NYC", 
        "countryCode": "US"
      }
    },
    "aircraft": {
      "321": "AIRBUS A321"
    },
    "carriers": {
      "AA": "AMERICAN AIRLINES"
    },
    "seatCharacteristics": {
      "W": "Window seat",
      "A": "Aisle seat", 
      "9": "Center seat (not window, not aisle)",
      "E": "Exit row seat",
      "CH": "Chargeable seats",
      "L": "Leg space seat",
      "K": "Bulkhead seat",
      "O": "Preferential seat"
    }
  }
}
```

**Example cURL**:
```bash
curl -X POST {BASE_URL}/flight-search \
  -H "Content-Type: application/json" \
  -H "X-API-Key: {YOUR_API_KEY}" \
  -H "Authorization: Bearer {YOUR_JWT_TOKEN}" \
  -d '{
    "origin": "LAX",
    "destination": "JFK",
    "departureDate": "2025-12-15",
    "travelClass": "ECONOMY",
    "airlineCode": "AA",
    "flightNumber": "123",
    "maxResults": 5,
    "offset": 0
  }'
```

---

## Response Format Options

### Default Unified Format

By default, flight search returns a clean, unified response format with only essential flight information and integrated seat map data. API-specific fields like `pricingOptions` and `blacklistedInEU` are excluded for a cleaner response.

### With Raw Flight Offer Data

When `includeRawFlightOffer: true` is specified in the request, each flight result includes an additional `rawFlightOffer` field containing the complete original API response:

```json
{
  "data": [
    {
      "id": "amadeus_1",
      "dataSource": "AMADEUS",
      // ... unified flight data ...
      "seatMap": { ... },
      "rawFlightOffer": {
        "id": "amadeus_1",
        "type": "flight-offer",
        "pricingOptions": {
          "fareType": ["PUBLISHED"],
          "includedCheckedBagsOnly": true
        },
        "itineraries": [
          {
            "segments": [
              {
                // ... including blacklistedInEU and other raw fields ...
                "blacklistedInEU": false
              }
            ]
          }
        ]
        // ... complete original API response ...
      }
    }
  ]
}
```

---

## Data Sources

Flight offers are currently retrieved from Amadeus:

### Amadeus
- **Coverage**: Global flight data
- **Data Quality**: Comprehensive pricing and availability
- **Seat Maps**: Integrated seat map data with concurrent API calls

### Response Fields

**Key Fields for Seat Maps**:
- `id`: Unique flight offer identifier  
- `dataSource`: Provider source (`AMADEUS`)
- `seatMapAvailable`: Boolean indicating if seat map data was successfully retrieved
- `seatMap`: Embedded seat map data object with `source` field indicating provider
- `seatMapError`: Error message if seat map retrieval failed (null on success)
- `rawFlightOffer`: Complete original API response (only when `includeRawFlightOffer: true`)

**Pricing Information**:
- `price.total`: Total price including taxes and fees
- `price.base`: Base fare excluding taxes
- `travelerPricings[]`: Per-traveler pricing breakdown

**Flight Details**:
- `itineraries[].segments[].carrierCode`: Airline code
- `itineraries[].segments[].number`: Flight number  
- `itineraries[].segments[].aircraft.code`: Aircraft type
- `numberOfBookableSeats`: Available seats for booking

---

## Travel Class Filtering

When `travelClass` is specified, results show flights with that minimum cabin quality:

- `ECONOMY`: Economy class and above
- `PREMIUM_ECONOMY`: Premium economy, business, and first class
- `BUSINESS`: Business and first class only  
- `FIRST`: First class only

If not specified, all cabin classes are included in results.

---

## Flight Number Filtering

### Field Separation

Flight number filtering now uses two separate fields for better API integration:

- `airlineCode`: 2-3 letter airline code (e.g., "AA", "UA", "DL")  
- `flightNumber`: 1-4 digit flight number (e.g., "123", "1679")

### Validation Rules

1. **Airline Code Only**: ✅ Valid
   ```json
   {
     "airlineCode": "UA"
   }
   ```
   → Searches for all United Airlines flights

2. **Both Fields**: ✅ Valid  
   ```json
   {
     "airlineCode": "UA",
     "flightNumber": "1679"
   }
   ```
   → Searches for specific flight UA1679

3. **Flight Number Only**: ❌ Invalid
   ```json
   {
     "flightNumber": "1679"
   }
   ```
   → Returns validation error: "Flight number can only be provided when airline code is also specified"

### Field Formats

- **airlineCode**: Must match pattern `^[A-Z]{2,3}$`
- **flightNumber**: Must match pattern `^[0-9]{1,4}$`
- **maxResults**: Must be between 1-20
- **offset**: Must be between 0-100 (supports up to 5 pages)

---

## Pagination Support

The flight search API supports offset-based pagination to retrieve large result sets efficiently.

### Pagination Parameters

- **maxResults**: Number of results per page (1-20, default: 10)
- **offset**: Starting point for results (0-100, default: 0)

### Pagination Metadata

All responses include pagination information in the `meta.pagination` object:

```json
{
  "meta": {
    "pagination": {
      "offset": 20,
      "limit": 10,
      "total": -1,
      "hasNext": true,
      "hasPrevious": true
    }
  }
}
```

**Fields**:
- `offset`: Current starting position
- `limit`: Results per page (same as maxResults)
- `total`: Total available results (-1 when unknown from API)
- `hasNext`: Whether more results are available 
- `hasPrevious`: Whether previous pages exist

### Pagination Examples

**First page (default)**:
```json
{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-12-15",
  "maxResults": 10,
  "offset": 0
}
```

**Second page**:
```json
{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-12-15", 
  "maxResults": 10,
  "offset": 10
}
```

**Custom page size**:
```json
{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-12-15",
  "maxResults": 5,
  "offset": 15
}
```

### Pagination Limits

- **Maximum pages**: 5 pages (offset cannot exceed 100)
- **Page size**: 1-20 results per page
- **Performance**: Smaller page sizes (10-15) provide better response times

### Backward Compatibility

Pagination is fully backward compatible. Existing requests without `offset` work unchanged:

**Legacy request (still supported)**:
```json
{
  "origin": "LAX",
  "destination": "JFK",
  "departureDate": "2025-12-15",
  "maxResults": 10
}
```

**New paginated request**:
```json
{
  "origin": "LAX",
  "destination": "JFK", 
  "departureDate": "2025-12-15",
  "maxResults": 10,
  "offset": 10
}
```

Both requests return identical response structures, with pagination metadata included in all responses.

---

## Integrated Seat Map Data

Flight search results now include embedded seat map availability:

1. **Integrated Response**: Each flight result includes `seatMapAvailable`, `seatMap`, and `seatMapError` fields
2. **No Separate Requests**: Seat map data is fetched concurrently during flight search
3. **Provider Routing**: Seat map requests are automatically routed to correct provider based on flight `dataSource`
4. **Error Handling**: Flights without available seat maps are filtered out; remaining flights guaranteed to have seat map data

---

## Seat Characteristics and Dictionaries

### Dynamic Seat Characteristics

Seat characteristics are now dynamically mapped using the `seatCharacteristics` dictionary from the API response. This provides accurate, airline-specific characteristic definitions.

**Key Features:**
- **Dynamic mapping**: Uses characteristic definitions from API response dictionaries when available
- **Fallback system**: Falls back to hardcoded mappings for characteristics not in response dictionary
- **Rich metadata**: Each characteristic includes category, description, and flags
- **Automatic categorization**: Characteristics are categorized based on their descriptions

**Seat Characteristic Structure:**
```json
{
  "characteristics": [
    {
      "code": "W",
      "category": "POSITION", 
      "description": "Window seat",
      "isRestriction": false,
      "isPremium": false
    },
    {
      "code": "CH",
      "category": "PREMIUM",
      "description": "Chargeable seats", 
      "isRestriction": false,
      "isPremium": true
    }
  ]
}
```

**Categories:**
- **POSITION**: Window (W), Aisle (A), Center (9) seats
- **SPECIAL**: Exit row (E), Bulkhead (K), Leg space (L), Front of cabin (FC)
- **PREMIUM**: Chargeable (CH), Premium (1A_AQC_PREMIUM_SEAT), Preferential (O) seats
- **RESTRICTION**: Not allowed for infant (1A), medical restrictions (1B), deportee (DE), crew (C)
- **GENERAL**: Other standard characteristics
- **UNKNOWN**: Unmapped characteristics (creates generic mapping with logging)

### Dictionaries Response

The `dictionaries` object contains reference data for locations, aircraft, carriers, and **seat characteristics**:

```json
{
  "dictionaries": {
    "seatCharacteristics": {
      "W": "Window seat",
      "A": "Aisle seat",
      "E": "Exit row seat",
      "CH": "Chargeable seats",
      "H": "High-traffic area seat"
    }
  }
}
```

**Usage:**
- Seat characteristics in the response use these dictionary definitions when available
- Provides airline-specific characteristic descriptions
- Automatically handles new or custom characteristic codes
- Eliminates "unmapped characteristic" errors for codes present in dictionary

---

## Error Responses

**400 Bad Request**:
```json
{
  "success": false,
  "message": "Validation errors: Origin is required; Departure date must be in YYYY-MM-DD format;"
}
```

**401 Unauthorized**:
```json
{
  "success": false,
  "message": "Missing or invalid authorization header"
}
```

**500 Internal Server Error**:
```json
{
  "success": false,
  "message": "Internal server error"
}
```