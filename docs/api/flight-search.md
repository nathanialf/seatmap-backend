# Flight Search API

## Overview

Search for flight offers from multiple providers (Amadeus and Sabre). Results include both pricing and flight details needed for seat map requests.

---

## Search Flight Offers

Search for available flight offers with flexible filtering options.

**Endpoint**: `POST /flight-offers`

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
      ]
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
curl -X POST {BASE_URL}/flight-offers \
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
- `itineraries[].segments[]`: Flight segment details needed for seat map requests

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

## Using Results for Seat Maps

Flight offer data from this endpoint is used directly in seat map requests:

1. **Save Full Flight Offer**: Store the complete flight offer object from this response
2. **Seat Map Request**: Pass the entire flight offer data to the seat map API
3. **Automatic Routing**: Seat map service automatically routes to correct provider based on `dataSource`

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