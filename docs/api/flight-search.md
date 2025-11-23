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
- `maxResults` (optional): Maximum results to return (1-50, default: 10)
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
        "decks": [...],
        "seats": [...]
      },
      "seatMapError": null
    }
  ],
  "meta": {
    "count": 1,
    "sources": "AMADEUS"
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
    "maxResults": 5
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
- **maxResults**: Must be between 1-50

---

## Integrated Seat Map Data

Flight search results now include embedded seat map availability:

1. **Integrated Response**: Each flight result includes `seatMapAvailable`, `seatMap`, and `seatMapError` fields
2. **No Separate Requests**: Seat map data is fetched concurrently during flight search
3. **Provider Routing**: Seat map requests are automatically routed to correct provider based on flight `dataSource`
4. **Error Handling**: Flights without available seat maps are filtered out; remaining flights guaranteed to have seat map data

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