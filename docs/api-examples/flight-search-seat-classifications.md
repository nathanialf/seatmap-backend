# Flight Search API: Seat Classifications and Real Data Examples

## Overview

This document provides real examples from the Flight Search API showing seat classifications, fare classes, and seat availability data. This data is crucial for implementing bookmark alerts that monitor seat availability thresholds.

## Real API Response Example

**Route:** ORD (Chicago) → DFW (Dallas)  
**Date:** 2025-11-09  
**Search Results:** 3 United Airlines flights found

### United Airlines Flight Examples

#### Flight UA539 - Airbus A319
```json
{
  "id": "8",
  "dataSource": "AMADEUS",
  "type": "flight-offer",
  "source": "GDS",
  "numberOfBookableSeats": 9,
  "itineraries": [
    {
      "duration": "PT2H43M",
      "segments": [
        {
          "departure": {
            "iataCode": "ORD",
            "terminal": "1",
            "at": "2025-11-09T07:25:00"
          },
          "arrival": {
            "iataCode": "DFW",
            "terminal": "E",
            "at": "2025-11-09T10:08:00"
          },
          "carrierCode": "UA",
          "number": "539",
          "aircraft": {
            "code": "319"
          }
        }
      ]
    }
  ],
  "price": {
    "currency": "EUR",
    "total": "353.03",
    "base": "316.00"
  },
  "travelerPricings": [
    {
      "fareDetailsBySegment": [
        {
          "cabin": "ECONOMY",
          "fareBasis": "UAA0PFEN",
          "class": "U",
          "includedCheckedBags": {
            "quantity": 0
          }
        }
      ]
    }
  ],
  "seatMapAvailable": true,
  "seatMapError": null
}
```

## Fare Class Analysis

### "U" Class Seats - Promotional/Restricted Economy

**Classification:** Deep Discount Economy  
**Airline:** United Airlines  
**Characteristics:**
- **Fare Basis:** UAA0PFEN
- **Cabin:** ECONOMY
- **Restrictions:** High (no checked bags included)
- **Flexibility:** Low (restricted fare)
- **Typical Use:** Price-sensitive travelers, employee discounts

### Airline Fare Class Hierarchy

**Economy Class Spectrum (Highest to Lowest Priority):**

1. **Y** - Full Fare Economy (most flexible)
2. **M, B, H** - Premium Economy discounts
3. **K, L** - Standard Economy discounts  
4. **U, V, X, G** - **Promotional/Restricted Economy** ⭐
5. **Q, O** - Deep discount (most restricted)

### Seat Availability for Alerts

**Key Metrics from Real Data:**
- `numberOfBookableSeats: 9` - Current available seats in this fare class
- `class: "U"` - Fare class designation  
- `cabin: "ECONOMY"` - Cabin type
- `seatMapAvailable: true` - Seat map data accessible

## Alert Implementation Implications

### For Bookmark Alerts

When monitoring **"U" class seats** (promotional economy):

**Percentage Calculation:**
```
Alert Threshold = (Current Available Seats / Total Aircraft Capacity for Economy) * 100
```

**Example for Airbus A319:**
- **Total Economy Capacity:** ~120-130 seats
- **Current U-Class Available:** 9 seats
- **Percentage:** ~7% of total economy capacity
- **10% Alert Threshold:** Would trigger when ≤12-13 seats available

### For Saved Search Alerts

**Airline Filtering Examples:**
- `flightNumber: "UA"` → Matches UA539, UA1304, UA1851
- Search returns flights with various fare classes (U, V, X, G possible)
- Alert triggers when ANY matching flight has seats above threshold

## Promotional Seat Patterns by Airline

### United Airlines (UA)
- **Confirmed Classes:** U (from real data)
- **Typical Pattern:** Y, M, H, K, L, U, V, X, G, Q, O
- **Promotional Range:** U, V, X, G (employee-friendly pricing)

### Expected American Airlines (AA)
- **Promotional Classes:** V, X, G (industry standard)
- **Deep Discount:** Similar hierarchy to United

### Expected Delta Airlines (DL)  
- **Promotional Classes:** V, X, G
- **Note:** May use different letter combinations

## API Response Structure for Alerts

### Key Fields for Seat Monitoring

```json
{
  "numberOfBookableSeats": 9,           // Current availability
  "travelerPricings": [{
    "fareDetailsBySegment": [{
      "cabin": "ECONOMY",               // Cabin type
      "fareBasis": "UAA0PFEN",         // Fare basis code  
      "class": "U"                     // Fare class letter
    }]
  }],
  "seatMapAvailable": true,            // Seat map accessibility
  "carrierCode": "UA",                 // Airline code
  "aircraft": {"code": "319"}          // Aircraft type (for capacity calculation)
}
```

### Aircraft Capacity Reference

**Airbus A319 (UA539 example):**
- **Total Seats:** ~120-130
- **Economy:** ~120-130 (single-class configuration)
- **10% Threshold:** ~12-13 seats

## Testing Routes with Available Data

**Working Routes (based on testing):**
- ORD → DFW (United Airlines confirmed)
- Limited international availability in dev environment

**Recommended Test Parameters:**
- **Date Range:** Next 1-3 days from current date
- **Popular Domestic Routes:** ORD-DFW, JFK-LAX, DFW-ORD
- **Airline Filtering:** "UA", "AA", "DL" for better results
- **Max Results:** 10-20 for optimal response time

## Alert Configuration Examples

### Percentage-Based Alert (Promotional Seats)
```json
{
  "alertType": "PERCENTAGE_THRESHOLD",
  "thresholdValue": 10.0,
  "travelClass": "ECONOMY",
  "monitoring": "Promotional economy seats (U, V, X, G classes)"
}
```

### Absolute Count Alert
```json
{
  "alertType": "ABSOLUTE_THRESHOLD", 
  "thresholdValue": 5,
  "travelClass": "ECONOMY",
  "monitoring": "Less than 5 economy seats available"
}
```

---

## Implementation Notes

1. **Real Data Source:** Amadeus and Sabre APIs provide live flight data
2. **Seat Classifications:** Follow standard IATA fare class conventions
3. **Promotional Monitoring:** Focus on U, V, X, G classes for employee travel
4. **Capacity Calculation:** Requires aircraft-specific seat configuration data
5. **Alert Frequency:** Batch processing recommended for cost efficiency

*Based on real API testing conducted November 2025*