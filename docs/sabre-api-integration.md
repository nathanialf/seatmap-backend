# Sabre API Integration Guide

## Overview

This document provides comprehensive guidance for integrating with Sabre's ACS_AirportFlightListRQ v3.0.0 and EnhancedSeatMapRQ v8.0.0 APIs. These APIs are specifically designed for airline employee use cases, providing operational flight data and detailed seat availability information.

## Table of Contents
- [Authentication Requirements](#authentication-requirements)
- [Environment Configuration](#environment-configuration)
- [API Usage](#api-usage)
- [Jenkins Integration](#jenkins-integration)
- [Data Flow](#data-flow)
- [Error Handling](#error-handling)
- [Testing](#testing)

## Authentication Requirements

### Sabre Authentication Model

Sabre uses a **session-based authentication** model with SOAP web services. The authentication flow consists of:

1. **Initial Authentication**: Username/Password authentication to obtain a session token
2. **Session Token**: BinarySecurityToken used for subsequent API calls
3. **Session Management**: Tokens expire after 1 hour and must be refreshed

### Required Credentials

#### Primary Authentication Credentials
- **SABRE_USER_ID**: Your Sabre user ID (provided by Sabre)
- **SABRE_PASSWORD**: Your Sabre password (provided by Sabre)
- **SABRE_ENDPOINT**: Sabre SOAP endpoint URL
  - Test: `https://sws-crt.cert.havail.sabre.com`
  - Production: `https://sws.sabre.com`

#### Organization & Domain Configuration
- **SABRE_ORGANIZATION**: Your organization code (e.g., "1S", "AA")
  - This is your Pseudo City Code (PCC) or partition ID
  - Determines which airline data you can access
- **SABRE_DOMAIN**: Your domain (typically "DEFAULT")

#### EPR (Employee Profile Record) Requirements

Based on the Sabre documentation, your EPR must have:

**Required Duty Codes:**
- Duty codes: 4, 5, 7 (with required keywords)
- OR Duty code: 8 (no keywords required)

**Required Keywords (for duty codes 4, 5, 7):**
- `GDSPLY`: GDS Play access
- `SELECT`: Selection capabilities  
- `FNLBDG`: Final Boarding access

**Important**: The agent's AAA city, duty code, and partition ID are configured by Universal Services Gateway (USG) / Sabre Web Services (SWS) during authentication.

### Authentication Flow

#### Step 1: SessionCreateRQ
```xml
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
    <soap-env:Header>
        <eb:MessageHeader eb:version="1.0">
            <eb:From><eb:PartyId type="urn:x12.org:IO5:01">99999</eb:PartyId></eb:From>
            <eb:To><eb:PartyId type="urn:x12.org:IO5:01">123123</eb:PartyId></eb:To>
            <eb:CPAId>YOUR_ORGANIZATION</eb:CPAId>
            <eb:ConversationId>V1@conversation-id</eb:ConversationId>
            <eb:Service eb:type="OTA">SessionCreateRQ</eb:Service>
            <eb:Action>SessionCreateRQ</eb:Action>
        </eb:MessageHeader>
        <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/12/secext">
            <wsse:UsernameToken>
                <wsse:Username>YOUR_USER_ID</wsse:Username>
                <wsse:Password>YOUR_PASSWORD</wsse:Password>
                <Organization>YOUR_ORGANIZATION</Organization>
                <Domain>YOUR_DOMAIN</Domain>
            </wsse:UsernameToken>
        </wsse:Security>
    </soap-env:Header>
    <soap-env:Body>
        <SessionCreateRQ returnContextID="true">
            <POS>
                <Source PseudoCityCode="YOUR_ORGANIZATION"/>
            </POS>
        </SessionCreateRQ>
    </soap-env:Body>
</soap-env:Envelope>
```

#### Step 2: Extract BinarySecurityToken
The response contains a `BinarySecurityToken` that must be used in all subsequent requests:

```xml
<wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/12/secext">
    <wsse:BinarySecurityToken>Shared/IDL:IceSess/SessMgr:1.0.IDL/Common/!ICESMS/STSB!ICESMSLB/STS.LB!1574923658285!805!5</wsse:BinarySecurityToken>
</wsse:Security>
```

### Session Management

- **Session Duration**: 1 hour (3600 seconds)
- **Refresh Buffer**: Refresh 5 minutes before expiry
- **Concurrent Sessions**: Limited per user account
- **Session Cleanup**: Sessions auto-expire; manual cleanup recommended

## Environment Configuration

### Required Environment Variables

```bash
# Primary Authentication
SABRE_USER_ID=your_sabre_user_id
SABRE_PASSWORD=your_sabre_password
SABRE_ENDPOINT=https://sws-crt.cert.havail.sabre.com

# Organization Configuration
SABRE_ORGANIZATION=1S
SABRE_DOMAIN=DEFAULT

# Optional Configuration
SABRE_DEFAULT_AIRLINE=AA
SABRE_CONNECT_TIMEOUT_SECONDS=30
SABRE_REQUEST_TIMEOUT_SECONDS=60
SABRE_MAX_RETRIES=3
```

### Configuration Notes

1. **SABRE_ORGANIZATION**: This is critical - it determines which airline's data you can access
2. **SABRE_ENDPOINT**: Use test endpoint for development, production for live operations
3. **Timeouts**: Sabre APIs can be slow; adjust timeouts based on performance needs
4. **Retries**: Network instability is common; configure appropriate retry logic

## API Usage

### Flight Schedule Search (ACS_AirportFlightListRQ v3.0.0)

#### Purpose
Returns a list of operational flights departing from the specified airport. Essential for airline employees to identify active flights for standby travel.

#### Key Features
- **Operational Flights**: Returns only flights initialized in SabreSonic Check-In (SSCI)
- **Time Windows**: 
  - Hub airports: 4 hours from current time
  - Regular airports: 8 hours from current time
- **Flight Initialization**: Flights must be initialized ~72 hours before departure

#### Usage Patterns

```java
// Basic airport flight list
JsonNode flights = sabreService.searchFlightSchedules(
    "DFW",           // origin airport
    "CLT",           // destination airport (optional)
    "2024-12-01",    // departure date (YYYY-MM-DD)
    "ECONOMY",       // travel class (optional)
    null,            // flight number filter (optional)
    10               // max results (optional)
);
```

#### Request Parameters
- **Origin** (required): 3-letter IATA airport code
- **Destination** (optional): 3-letter IATA airport code
- **DepartureDate** (optional): YYYY-MM-DD format
- **TravelClass** (optional): ECONOMY, BUSINESS, FIRST, PREMIUM_ECONOMY
- **FlightNumber** (optional): Specific flight number filter
- **MaxResults** (optional): Limit results (translated to hours from current time)

#### Response Structure
```json
{
    "data": [
        {
            "type": "flight-offer",
            "id": "sabre_12345678",
            "dataSource": "SABRE",
            "source": "GDS",
            "itineraries": [{
                "segments": [{
                    "departure": {
                        "iataCode": "DFW",
                        "at": "2024-12-01T05:05:00",
                        "gate": "E35"
                    },
                    "arrival": {
                        "iataCode": "CLT",
                        "at": "2024-12-01T08:30:00"
                    },
                    "carrierCode": "AA",
                    "number": "766",
                    "aircraft": {
                        "code": "321"
                    },
                    "status": "OPENCI"
                }]
            }]
        }
    ],
    "meta": {
        "count": 1,
        "source": "SABRE"
    }
}
```

### Seat Map Retrieval (EnhancedSeatMapRQ v8.0.0)

#### Purpose
Provides detailed seat availability and cabin configuration for specific flights. Critical for airline employees to assess seat availability before committing to standby travel.

#### Key Features
- **Enhanced Data**: v8.0.0 provides richer seat characteristics and pricing
- **Real-time Availability**: Current seat occupation status
- **Detailed Characteristics**: Window, aisle, exit row, restricted recline, etc.
- **Pricing Information**: Seat upgrade costs and fees

#### Usage Patterns

```java
// Get seat map for specific flight
JsonNode seatMap = sabreService.getSeatMapFromFlight(
    "AA",            // carrier code
    "766",           // flight number
    "2024-12-01",    // departure date
    "DFW",           // origin
    "CLT"            // destination
);
```

#### Request Parameters
- **CarrierCode** (required): 2-letter airline code
- **FlightNumber** (required): Flight number
- **DepartureDate** (required): YYYY-MM-DD format
- **Origin** (required): 3-letter IATA airport code
- **Destination** (required): 3-letter IATA airport code

#### Response Structure
```json
{
    "success": true,
    "data": {
        "type": "seatmap",
        "source": "SABRE",
        "carrierCode": "AA",
        "flightNumber": "766",
        "departure": {
            "iataCode": "DFW"
        },
        "arrival": {
            "iataCode": "CLT"
        },
        "deck": {
            "deckType": "MAIN",
            "deckConfiguration": {
                "width": 6,
                "length": 33
            },
            "seats": [
                {
                    "number": "1A",
                    "availabilityStatus": "AVAILABLE",
                    "coordinates": {
                        "x": 1,
                        "y": 1
                    },
                    "pricing": {
                        "currency": "USD",
                        "total": "0.00",
                        "base": "0.00"
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
    }
}
```

## Seat Map Data Structure Updates

### Dynamic Seat Characteristics

The seat map response now includes enhanced characteristic mapping with dynamic definitions from API response dictionaries:

**Key Changes:**
- **Removed `characteristicsCodes`**: Raw code arrays no longer included
- **Enhanced `characteristics`**: Full objects with normalized data
- **Dynamic mapping**: Uses seat characteristic definitions from API response dictionaries
- **Removed `facilities`**: No longer includes lavatory/galley locations in deck data
- **Simplified pricing**: Single `pricing` object instead of `travelerPricing` array

**Seat Characteristics Structure:**
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
- `POSITION`: Window, aisle, center seats
- `SPECIAL`: Bulkhead, exit row, leg space
- `PREMIUM`: Chargeable, preferential seats 
- `RESTRICTION`: Not allowed for certain passengers
- `GENERAL`: Other characteristics
- `UNKNOWN`: Unmapped characteristics

**Pricing Structure:**
```json
{
  "pricing": {
    "currency": "USD",
    "total": "25.00",
    "base": "22.00", 
    "taxes": [
      {
        "amount": "3.00",
        "code": "YQ"
      }
    ]
  }
}
```

## Jenkins Integration

### Pipeline Configuration

#### Required Jenkins Credentials

Set up these credentials in Jenkins Credentials Manager:

1. **sabre-user-id** (Secret Text)
   - ID: `sabre-user-id`
   - Description: "Sabre API User ID"

2. **sabre-password** (Secret Text)
   - ID: `sabre-password`
   - Description: "Sabre API Password"

3. **sabre-organization** (Secret Text)
   - ID: `sabre-organization`
   - Description: "Sabre Organization/PCC Code"

4. **sabre-domain** (Secret Text)
   - ID: `sabre-domain`
   - Description: "Sabre Domain"

#### Environment-Specific Endpoints

```groovy
pipeline {
    agent any
    
    environment {
        // Set endpoint based on environment
        SABRE_ENDPOINT = "${params.ENVIRONMENT == 'prod' ? 'https://sws.sabre.com' : 'https://sws-crt.cert.havail.sabre.com'}"
        SABRE_DEFAULT_AIRLINE = 'AA'
        SABRE_CONNECT_TIMEOUT_SECONDS = '30'
        SABRE_REQUEST_TIMEOUT_SECONDS = '60'
        SABRE_MAX_RETRIES = '3'
    }
    
    stages {
        stage('Deploy') {
            steps {
                withCredentials([
                    string(credentialsId: 'sabre-user-id', variable: 'SABRE_USER_ID'),
                    string(credentialsId: 'sabre-password', variable: 'SABRE_PASSWORD'),
                    string(credentialsId: 'sabre-organization', variable: 'SABRE_ORGANIZATION'),
                    string(credentialsId: 'sabre-domain', variable: 'SABRE_DOMAIN')
                ]) {
                    script {
                        // Deploy with Terraform
                        sh """
                            export AWS_PROFILE=seatmap-${params.ENVIRONMENT}
                            cd terraform/environments/${params.ENVIRONMENT}
                            terraform apply -auto-approve \\
                                -var="sabre_user_id=${SABRE_USER_ID}" \\
                                -var="sabre_password=${SABRE_PASSWORD}" \\
                                -var="sabre_endpoint=${SABRE_ENDPOINT}" \\
                                -var="sabre_organization=${SABRE_ORGANIZATION}" \\
                                -var="sabre_domain=${SABRE_DOMAIN}"
                        """
                    }
                }
            }
        }
    }
}
```

#### Terraform Variable Configuration

Add to your `variables.tf`:

```hcl
variable "sabre_user_id" {
  description = "Sabre API User ID"
  type        = string
  sensitive   = true
}

variable "sabre_password" {
  description = "Sabre API Password"  
  type        = string
  sensitive   = true
}

variable "sabre_endpoint" {
  description = "Sabre API Endpoint URL"
  type        = string
  default     = "https://sws-crt.cert.havail.sabre.com"
}

variable "sabre_organization" {
  description = "Sabre Organization/PCC Code"
  type        = string
  sensitive   = true
}

variable "sabre_domain" {
  description = "Sabre Domain"
  type        = string
  default     = "DEFAULT"
  sensitive   = true
}
```

Add to your Lambda environment variables in `main.tf`:

```hcl
resource "aws_lambda_function" "seatmap_handler" {
  # ... other configuration ...
  
  environment {
    variables = {
      # ... other environment variables ...
      SABRE_USER_ID                   = var.sabre_user_id
      SABRE_PASSWORD                  = var.sabre_password
      SABRE_ENDPOINT                  = var.sabre_endpoint
      SABRE_ORGANIZATION              = var.sabre_organization
      SABRE_DOMAIN                    = var.sabre_domain
      SABRE_DEFAULT_AIRLINE          = "AA"
      SABRE_CONNECT_TIMEOUT_SECONDS  = "30"
      SABRE_REQUEST_TIMEOUT_SECONDS  = "60"
      SABRE_MAX_RETRIES              = "3"
    }
  }
}
```

### Security Best Practices

1. **Credential Rotation**: Regularly rotate Sabre credentials
2. **Access Control**: Limit Jenkins credential access to deployment jobs only
3. **Audit Logging**: Enable audit logs for credential access
4. **Environment Separation**: Use different credentials for dev/prod
5. **Secret Scanning**: Implement secret scanning in CI/CD pipeline

## Data Flow

### Incoming Request Flow

```
1. User Request (Flight Search)
   ↓
2. FlightOffersHandler
   ↓
3. SabreService.searchFlightSchedules()
   ↓
4. Session Check/Authentication
   ↓
5. SOAP Request Creation (ACS_AirportFlightListRQ v3.0.0)
   ↓
6. HTTP Call to Sabre Endpoint
   ↓
7. SOAP Response Parsing
   ↓
8. JSON Response Formatting
   ↓
9. Return to Handler
```

### Authentication Flow Detail

```
1. Check Session Validity
   ├─ Valid → Use existing token
   └─ Invalid/Expired → Authenticate
       ↓
2. Create SessionCreateRQ SOAP Message
   ├─ MessageHeader (conversation ID, timestamps)
   ├─ Security (UsernameToken with org/domain)
   └─ SessionCreateRQ body
       ↓
3. Send to Sabre Endpoint
   ↓
4. Parse SessionCreateRS
   ├─ Extract BinarySecurityToken
   ├─ Set expiration time (1 hour)
   └─ Store for subsequent requests
       ↓
5. Use token in all API calls
```

### Data Processing Pipeline

```
API Response → XML Parsing → Element Extraction → JSON Mapping → Response Formatting
```

**Key Processing Steps:**

1. **XML Document Parsing**: Parse SOAP response using DOM
2. **Element Extraction**: Extract specific elements based on API version
3. **Data Transformation**: Convert Sabre format to internal JSON format
4. **Time Conversion**: Convert AM/PM format to ISO 8601
5. **Response Standardization**: Apply consistent response structure

## Error Handling

### Common Error Scenarios

#### Authentication Errors
- **Invalid Credentials**: Wrong username/password
- **Insufficient Privileges**: Missing EPR keywords/duty codes
- **Session Expired**: Token expired, automatic refresh triggered
- **Organization Access**: Invalid organization/PCC code

#### API Errors
- **Flight Not Initialized**: Flight not in SSCI system (72-hour window)
- **Invalid Airport Code**: Non-existent or invalid IATA codes
- **Date Outside Window**: Request outside check-in window
- **No Data Available**: No flights match criteria

#### Network Errors
- **Connection Timeout**: Network connectivity issues
- **SOAP Faults**: Malformed requests or server errors
- **Service Unavailable**: Sabre system maintenance

### Error Response Format

```json
{
    "success": false,
    "message": "Sabre authentication failed: Invalid credentials",
    "error": {
        "code": "AUTH_FAILED",
        "source": "SABRE",
        "details": "Username or password incorrect"
    }
}
```

### Retry Logic

The service implements exponential backoff retry:

1. **Initial Attempt**: Standard request
2. **Retry 1**: Wait 1 second, retry
3. **Retry 2**: Wait 2 seconds, retry  
4. **Retry 3**: Wait 4 seconds, retry
5. **Final Failure**: Return error to client

**No Retry Scenarios:**
- Authentication failures
- Validation errors
- Malformed requests

## Testing

### Unit Tests

Run comprehensive unit tests:

```bash
# Run all SabreService tests
./gradlew test --tests "SabreServiceTest"

# Run integration tests (requires test credentials)
SABRE_INTEGRATION_TEST=true ./gradlew test --tests "SabreServiceIntegrationTest"
```

### Test Environment Setup

```bash
export SABRE_USER_ID="test_user"
export SABRE_PASSWORD="test_password"
export SABRE_ENDPOINT="https://sws-crt.cert.havail.sabre.com"
export SABRE_ORGANIZATION="TEST"
export SABRE_DOMAIN="TEST_DOMAIN"
export SABRE_DEFAULT_AIRLINE="AA"
```

### API Testing with curl

Store credentials securely in temporary files:

```bash
# Store credentials
echo "https://sws-crt.cert.havail.sabre.com" > /tmp/sabre_endpoint
echo "your-session-token" > /tmp/sabre_token

# Create SOAP request
cat > /tmp/flight_request.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"
                   xmlns:n1="http://services.sabre.com/ACS/BSO/airportFlightList/v3">
    <soap-env:Header>
        <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/12/secext">
            <wsse:BinarySecurityToken>SESSION_TOKEN_HERE</wsse:BinarySecurityToken>
        </wsse:Security>
    </soap-env:Header>
    <soap-env:Body>
        <n1:ACS_AirportFlightListRQ>
            <FlightInfo>
                <Airline>AA</Airline>
                <Origin>DFW</Origin>
                <DepartureDate>2024-12-01</DepartureDate>
            </FlightInfo>
            <Client>WEB</Client>
        </n1:ACS_AirportFlightListRQ>
    </soap-env:Body>
</soap-env:Envelope>
EOF

# Execute request
curl -X POST $(cat /tmp/sabre_endpoint) \
     -H "Content-Type: text/xml; charset=utf-8" \
     -H "SOAPAction: ACS_AirportFlightListRQ" \
     -d @/tmp/flight_request.xml

# Cleanup
rm -f /tmp/sabre_endpoint /tmp/sabre_token /tmp/flight_request.xml
```

## Performance Considerations

### API Performance Characteristics

- **Authentication**: ~2-3 seconds per session creation
- **Flight Search**: ~3-5 seconds for typical requests
- **Seat Maps**: ~5-8 seconds for detailed cabin data
- **Rate Limits**: Varies by contract, typically 10-50 requests/minute

### Optimization Strategies

1. **Session Reuse**: Keep sessions alive for 1-hour duration
2. **Connection Pooling**: Reuse HTTP connections where possible
3. **Caching**: Cache flight data for short periods (5-15 minutes)
4. **Concurrent Requests**: Limit concurrent calls to avoid rate limiting
5. **Request Optimization**: Only request necessary data fields

### Monitoring

Key metrics to monitor:

- **Session Success Rate**: Authentication success percentage
- **API Response Times**: Average and 95th percentile response times
- **Error Rates**: Categorized by error type
- **Session Utilization**: How effectively sessions are reused

## Troubleshooting

### Common Issues

1. **"NO DATA FOR THIS SELECTION"**
   - Flight not initialized in SSCI
   - Request outside 72-hour window
   - Invalid airport codes

2. **Authentication Failures**
   - Check EPR keywords and duty codes
   - Verify organization/PCC access
   - Confirm credentials are current

3. **Timeout Issues**
   - Increase timeout values
   - Check network connectivity
   - Verify Sabre endpoint availability

4. **Session Management Issues**
   - Monitor session expiration
   - Implement proper refresh logic
   - Handle concurrent session limits

### Diagnostic Steps

1. **Enable Debug Logging**: Set log level to DEBUG for detailed SOAP messages
2. **Check Endpoint Connectivity**: Verify network access to Sabre endpoints
3. **Validate Credentials**: Test authentication independently
4. **Review API Documentation**: Ensure request format matches v3.0.0/v8.0.0 specs
5. **Contact Sabre Support**: For persistent issues, engage Sabre technical support

---

## Support and Resources

- **Sabre Developer Portal**: https://developer.sabre.com/
- **ACS Flight List API**: https://developer.sabre.com/soap-api/airport-flight-list/3.0.0
- **Enhanced Seat Map API**: https://developer.sabre.com/soap-api/seat-map/8.0.0
- **Internal Documentation**: `docs-private/sabre/` directory
- **Test Credentials**: Contact your Sabre account manager