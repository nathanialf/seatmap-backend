# Flight Search API

## Overview

Search for flight offers from multiple providers (Amadeus and Sabre) with **integrated seat map data**. Results include flight pricing, details, and embedded seat map availability - providing a complete view for travel planning.

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
  "flightNumber": "AA123",
  "maxResults": 10
}
```

**Parameters**:
- `origin` (required): 3-letter IATA airport code for departure
- `destination` (required): 3-letter IATA airport code for arrival  
- `departureDate` (required): Date in YYYY-MM-DD format
- `travelClass` (optional): Cabin class filter - `ECONOMY`, `PREMIUM_ECONOMY`, `BUSINESS`, `FIRST`
- `flightNumber` (optional): Specific flight number filter
- `maxResults` (optional): Maximum results to return (default: 10)

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
              "numberOfStops": 0,
              "blacklistedInEU": false
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
      "pricingOptions": {
        "fareType": ["PUBLISHED"],
        "includedCheckedBagsOnly": true
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
        "source": "AMADEUS"
      },
      "seatMapError": null
    }
  ],
  "meta": {
    "count": 1,
    "sources": "AMADEUS,SABRE"
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
    "maxResults": 5
  }'
```

---

## Data Sources

Flight offers are retrieved from multiple providers:

### Amadeus (Primary)
- **Priority**: Higher priority in results
- **Coverage**: Global flight data
- **Data Quality**: Comprehensive pricing and availability

### Sabre (Secondary) 
- **Priority**: Lower priority, supplements Amadeus data
- **Coverage**: Additional route coverage
- **Deduplication**: Duplicate flights are automatically removed

### Response Fields

**Key Fields for Seat Maps**:
- `id`: Unique flight offer identifier  
- `dataSource`: Provider source (`AMADEUS` or `SABRE`)
- `seatMapAvailable`: Boolean indicating if seat map data was successfully retrieved
- `seatMap`: Embedded seat map data object with `source` field indicating provider
- `seatMapError`: Error message if seat map retrieval failed (null on success)

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