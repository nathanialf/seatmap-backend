package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.exception.SeatmapApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

public class SabreService {
    private static final Logger logger = LoggerFactory.getLogger(SabreService.class);
    
    private final String userId;
    private final String password;
    private final String endpoint;
    private final String organization;
    private final String domain;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SOAPConnectionFactory soapConnectionFactory;
    private final XPath xpath;
    
    private String sessionToken;
    private long tokenExpiresAt;
    private static final long SESSION_DURATION_MS = 60 * 60 * 1000; // 1 hour
    private static final long TOKEN_REFRESH_BUFFER_MS = 5 * 60 * 1000; // 5 minutes before expiry
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 30;
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_MAX_RETRIES = 3;
    
    // Configuration from environment variables
    private final int connectTimeoutSeconds;
    private final int requestTimeoutSeconds;
    private final int maxRetries;
    
    public SabreService() {
        this.userId = System.getenv("SABRE_USER_ID");
        this.password = System.getenv("SABRE_PASSWORD");
        this.endpoint = System.getenv("SABRE_ENDPOINT");
        this.organization = System.getenv("SABRE_ORGANIZATION") != null ? System.getenv("SABRE_ORGANIZATION") : "1S";
        this.domain = System.getenv("SABRE_DOMAIN") != null ? System.getenv("SABRE_DOMAIN") : "DEFAULT";
        
        if (userId == null || password == null || endpoint == null) {
            throw new IllegalStateException("Sabre API credentials not configured. Required: SABRE_USER_ID, SABRE_PASSWORD, SABRE_ENDPOINT");
        }
        
        // Load configuration from environment variables with defaults
        this.connectTimeoutSeconds = parseIntEnvVar("SABRE_CONNECT_TIMEOUT_SECONDS", DEFAULT_CONNECT_TIMEOUT_SECONDS);
        this.requestTimeoutSeconds = parseIntEnvVar("SABRE_REQUEST_TIMEOUT_SECONDS", DEFAULT_REQUEST_TIMEOUT_SECONDS);
        this.maxRetries = parseIntEnvVar("SABRE_MAX_RETRIES", DEFAULT_MAX_RETRIES);
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
            .build();
        this.objectMapper = new ObjectMapper();
        this.xpath = XPathFactory.newInstance().newXPath();
        
        logger.info("Sabre service initialized with config - Connect timeout: {}s, Request timeout: {}s, Max retries: {}", 
                   connectTimeoutSeconds, requestTimeoutSeconds, maxRetries);
        
        try {
            this.soapConnectionFactory = SOAPConnectionFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException("Failed to initialize SOAP connection factory", e);
        }
    }
    
    private int parseIntEnvVar(String envVarName, int defaultValue) {
        String value = System.getenv(envVarName);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid value for {}: '{}', using default: {}", envVarName, value, defaultValue);
            return defaultValue;
        }
    }
    
    public JsonNode searchFlightSchedules(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SeatmapApiException {
        try {
            // Validate inputs first
            validateInputs(origin, destination, departureDate);
            
            ensureValidSession();
            
            SOAPMessage soapRequest = createFlightSchedulesRequest(origin, destination, departureDate, travelClass, flightNumber, maxResults);
            SOAPMessage soapResponse = sendSoapRequest(soapRequest);
            
            return parseFlightSchedulesResponse(soapResponse);
            
        } catch (SeatmapApiException e) {
            // Re-throw our own exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Error calling Sabre Flight Schedules API", e);
            throw new SeatmapApiException("Network error calling Sabre API: " + e.getMessage(), e);
        }
    }
    
    public JsonNode getSeatMapFromFlight(String carrierCode, String flightNumber, String departureDate, String origin, String destination) throws SeatmapApiException {
        try {
            // Validate inputs
            validateInputs(origin, destination, departureDate);
            if (carrierCode == null || carrierCode.trim().isEmpty()) {
                throw new SeatmapApiException("Carrier code is required");
            }
            if (flightNumber == null || flightNumber.trim().isEmpty()) {
                throw new SeatmapApiException("Flight number is required");
            }
            
            ensureValidSession();
            
            SOAPMessage soapRequest = createSeatMapRequest(carrierCode, flightNumber, departureDate, origin, destination);
            SOAPMessage soapResponse = sendSoapRequest(soapRequest);
            
            return parseSeatMapResponse(soapResponse);
            
        } catch (SeatmapApiException e) {
            // Re-throw our own exceptions
            throw e;
        } catch (Exception e) {
            logger.error("Error calling Sabre Seat Map API", e);
            throw new SeatmapApiException("Network error calling Sabre Seat Map API: " + e.getMessage(), e);
        }
    }
    
    private void ensureValidSession() throws SeatmapApiException {
        long currentTime = System.currentTimeMillis();
        if (sessionToken == null || currentTime >= (tokenExpiresAt - TOKEN_REFRESH_BUFFER_MS)) {
            logger.info("Session token is null or expiring soon, authenticating...");
            authenticateSession();
        }
    }
    
    private void validateInputs(String origin, String destination, String departureDate) throws SeatmapApiException {
        if (origin == null || origin.trim().isEmpty()) {
            throw new SeatmapApiException("Origin airport code is required");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new SeatmapApiException("Destination airport code is required");
        }
        if (departureDate == null || departureDate.trim().isEmpty()) {
            throw new SeatmapApiException("Departure date is required");
        }
        
        // Validate airport codes (3 characters)
        if (origin.trim().length() != 3) {
            throw new SeatmapApiException("Origin must be a 3-letter IATA airport code");
        }
        if (destination.trim().length() != 3) {
            throw new SeatmapApiException("Destination must be a 3-letter IATA airport code");
        }
        
        // Validate date format (YYYY-MM-DD)
        try {
            LocalDate.parse(departureDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new SeatmapApiException("Departure date must be in YYYY-MM-DD format");
        }
    }
    
    private synchronized void authenticateSession() throws SeatmapApiException {
        // Double-check locking pattern to avoid unnecessary authentication
        long currentTime = System.currentTimeMillis();
        if (sessionToken != null && currentTime < (tokenExpiresAt - TOKEN_REFRESH_BUFFER_MS)) {
            return; // Session is still valid
        }
        
        logger.info("Authenticating with Sabre API...");
        
        try {
            SOAPMessage authRequest = createAuthenticationRequest();
            SOAPMessage authResponse = sendSoapRequestWithRetry(authRequest, 2); // Fewer retries for auth
            
            parseAuthenticationResponse(authResponse);
            
            logger.info("Successfully authenticated with Sabre API. Token expires at: {}", 
                       new java.util.Date(tokenExpiresAt));
            
        } catch (SeatmapApiException e) {
            // Clear any invalid token
            sessionToken = null;
            tokenExpiresAt = 0;
            throw e;
        } catch (Exception e) {
            // Clear any invalid token
            sessionToken = null;
            tokenExpiresAt = 0;
            logger.error("Failed to authenticate with Sabre API", e);
            throw new SeatmapApiException("Authentication failed with Sabre API: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if the current session token is valid and not expired
     */
    public boolean isSessionValid() {
        return sessionToken != null && System.currentTimeMillis() < (tokenExpiresAt - TOKEN_REFRESH_BUFFER_MS);
    }
    
    /**
     * Manually refresh the session token
     */
    public void refreshSession() throws SeatmapApiException {
        logger.info("Manually refreshing Sabre session...");
        sessionToken = null; // Force re-authentication
        tokenExpiresAt = 0;
        authenticateSession();
    }
    
    /**
     * Get session token expiration time
     */
    public long getSessionExpirationTime() {
        return tokenExpiresAt;
    }
    
    private SOAPMessage createAuthenticationRequest() throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        
        // Add required namespaces for Sabre authentication
        envelope.addNamespaceDeclaration("eb", "http://www.ebxml.org/namespaces/messageHeader");
        envelope.addNamespaceDeclaration("wsse", "http://schemas.xmlsoap.org/ws/2002/12/secext");
        envelope.addNamespaceDeclaration("wsu", "http://schemas.xmlsoap.org/ws/2002/12/utility");
        
        // Create SOAP Header with MessageHeader and Security
        SOAPHeader header = envelope.getHeader();
        
        // MessageHeader
        SOAPElement messageHeader = header.addChildElement("MessageHeader", "eb");
        messageHeader.addAttribute(envelope.createName("mustUnderstand"), "1");
        messageHeader.addAttribute(envelope.createName("version"), "1.0");
        
        // From element
        SOAPElement from = messageHeader.addChildElement("From", "eb");
        SOAPElement fromPartyId = from.addChildElement("PartyId", "eb");
        fromPartyId.addAttribute(envelope.createName("type"), "urn:x12.org:IO5:01");
        fromPartyId.addTextNode("999999");
        
        // To element
        SOAPElement to = messageHeader.addChildElement("To", "eb");
        SOAPElement toPartyId = to.addChildElement("PartyId", "eb");
        toPartyId.addAttribute(envelope.createName("type"), "urn:x12.org:IO5:01");
        toPartyId.addTextNode("123123");
        
        // Other MessageHeader elements
        messageHeader.addChildElement("CPAId", "eb").addTextNode(organization);
        messageHeader.addChildElement("ConversationId", "eb").addTextNode(generateConversationId());
        
        SOAPElement service = messageHeader.addChildElement("Service", "eb");
        service.addAttribute(envelope.createName("type"), "OTA");
        service.addTextNode("SessionCreateRQ");
        
        messageHeader.addChildElement("Action", "eb").addTextNode("SessionCreateRQ");
        
        // MessageData
        SOAPElement messageData = messageHeader.addChildElement("MessageData", "eb");
        messageData.addChildElement("MessageId", "eb").addTextNode(generateMessageId());
        messageData.addChildElement("Timestamp", "eb").addTextNode(getCurrentTimestamp());
        
        // Security element with UsernameToken
        SOAPElement security = header.addChildElement("Security", "wsse");
        SOAPElement usernameToken = security.addChildElement("UsernameToken", "wsse");
        usernameToken.addChildElement("Username", "wsse").addTextNode(userId);
        usernameToken.addChildElement("Password", "wsse").addTextNode(password);
        
        // Organization and Domain without namespace prefix
        SOAPElement orgElement = usernameToken.addChildElement("Organization");
        orgElement.addTextNode(organization);
        SOAPElement domainElement = usernameToken.addChildElement("Domain");
        domainElement.addTextNode(domain);
        
        // SOAP Body
        SOAPBody body = envelope.getBody();
        SOAPElement sessionCreate = body.addChildElement("SessionCreateRQ");
        sessionCreate.addAttribute(envelope.createName("returnContextID"), "true");
        
        // POS element
        SOAPElement pos = sessionCreate.addChildElement("POS");
        SOAPElement source = pos.addChildElement("Source");
        source.addAttribute(envelope.createName("PseudoCityCode"), organization);
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private String generateConversationId() {
        return "V1@" + UUID.randomUUID().toString() + "@" + UUID.randomUUID().toString() + "@" + UUID.randomUUID().toString();
    }
    
    private String generateMessageId() {
        return "mid:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")) + "@sabre.client.com";
    }
    
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }
    
    private SOAPMessage createFlightSchedulesRequest(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        
        // Add required namespaces for v3.0.0
        envelope.addNamespaceDeclaration("wsse", "http://schemas.xmlsoap.org/ws/2002/12/secext");
        envelope.addNamespaceDeclaration("wsu", "http://schemas.xmlsoap.org/ws/2002/12/utility");
        envelope.addNamespaceDeclaration("n1", "http://services.sabre.com/ACS/BSO/airportFlightList/v3");
        
        // Add session header with BinarySecurityToken
        SOAPHeader header = envelope.getHeader();
        SOAPElement security = header.addChildElement("Security", "wsse");
        SOAPElement binaryToken = security.addChildElement("BinarySecurityToken", "wsse");
        binaryToken.addTextNode(sessionToken);
        
        // SOAP Body with proper ACS_AirportFlightListRQ v3.0.0 format
        SOAPBody body = envelope.getBody();
        SOAPElement flightSchedulesRQ = body.addChildElement("ACS_AirportFlightListRQ", "n1");
        
        // FlightInfo element
        SOAPElement flightInfo = flightSchedulesRQ.addChildElement("FlightInfo", "n1");
        
        // Add airline code - get from environment or use default
        SOAPElement airline = flightInfo.addChildElement("Airline", "n1");
        String airlineCode = System.getenv("SABRE_DEFAULT_AIRLINE");
        if (airlineCode == null || airlineCode.trim().isEmpty()) {
            airlineCode = "U0"; // Default airline code
        }
        airline.addTextNode(airlineCode);
        
        // Add origin airport
        SOAPElement originElement = flightInfo.addChildElement("Origin", "n1");
        originElement.addTextNode(origin.trim().toUpperCase());
        
        // Add departure date if provided
        if (departureDate != null && !departureDate.trim().isEmpty()) {
            SOAPElement depDate = flightInfo.addChildElement("DepartureDate", "n1");
            depDate.addTextNode(departureDate);
        }
        
        // Add destination if provided
        if (destination != null && !destination.trim().isEmpty()) {
            SOAPElement destinationElement = flightInfo.addChildElement("Destination", "n1");
            destinationElement.addTextNode(destination.trim().toUpperCase());
        }
        
        // Add flight number if provided
        if (flightNumber != null && !flightNumber.trim().isEmpty()) {
            SOAPElement flightNum = flightInfo.addChildElement("FlightNumber", "n1");
            flightNum.addTextNode(flightNumber.trim());
        }
        
        // Add departure time range if needed
        if (maxResults != null && maxResults > 0) {
            SOAPElement hoursFromCurrent = flightInfo.addChildElement("HoursFromCurrentTime", "n1");
            hoursFromCurrent.addTextNode(String.valueOf(Math.min(maxResults, 24))); // Limit to 24 hours
        }
        
        // Add client type
        SOAPElement client = flightSchedulesRQ.addChildElement("Client", "n1");
        client.addTextNode("WEB");
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private String mapTravelClassToSabre(String travelClass) {
        if (travelClass == null) return "Y";
        switch (travelClass.toUpperCase()) {
            case "FIRST": return "F";
            case "BUSINESS": return "C";
            case "PREMIUM_ECONOMY": return "W";
            case "ECONOMY":
            default: return "Y";
        }
    }
    
    private SOAPMessage createSeatMapRequest(String carrierCode, String flightNumber, String departureDate, String origin, String destination) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        
        // Add required namespaces for v8.0.0
        envelope.addNamespaceDeclaration("wsse", "http://schemas.xmlsoap.org/ws/2002/12/secext");
        envelope.addNamespaceDeclaration("wsu", "http://schemas.xmlsoap.org/ws/2002/12/utility");
        envelope.addNamespaceDeclaration("ns8", "http://stl.sabre.com/Merchandising/v8");
        
        // Add session header with BinarySecurityToken
        SOAPHeader header = envelope.getHeader();
        SOAPElement security = header.addChildElement("Security", "wsse");
        SOAPElement binaryToken = security.addChildElement("BinarySecurityToken", "wsse");
        binaryToken.addTextNode(sessionToken);
        
        // SOAP Body with EnhancedSeatMapRQ v8.0.0
        SOAPBody body = envelope.getBody();
        SOAPElement seatMapRQ = body.addChildElement("EnhancedSeatMapRQ", "ns8");
        seatMapRQ.addAttribute(envelope.createName("version"), "8");
        
        // SeatMapQueryEnhanced with v8.0.0 structure
        SOAPElement seatMapQuery = seatMapRQ.addChildElement("SeatMapQueryEnhanced", "ns8");
        seatMapQuery.addAttribute(envelope.createName("correlationID"), generateCorrelationId());
        
        // RequestType
        seatMapQuery.addChildElement("RequestType", "ns8").addTextNode("Payload");
        
        // Flight element with enhanced attributes
        SOAPElement flight = seatMapQuery.addChildElement("Flight", "ns8");
        flight.addAttribute(envelope.createName("destination"), destination.trim().toUpperCase());
        flight.addAttribute(envelope.createName("origin"), origin.trim().toUpperCase());
        
        // DepartureDate in YYYY-MM-DD format
        flight.addChildElement("DepartureDate", "ns8").addTextNode(departureDate);
        
        // Operating carrier
        SOAPElement operating = flight.addChildElement("Operating", "ns8");
        operating.addAttribute(envelope.createName("carrier"), carrierCode.trim().toUpperCase());
        operating.addTextNode(flightNumber.trim());
        
        // Marketing carrier (same as operating for most cases)
        SOAPElement marketing = flight.addChildElement("Marketing", "ns8");
        marketing.addAttribute(envelope.createName("carrier"), carrierCode.trim().toUpperCase());
        marketing.addTextNode(flightNumber.trim());
        
        // Add CabinDefinition for v8.0.0
        SOAPElement cabinDefinition = seatMapQuery.addChildElement("CabinDefinition", "ns8");
        SOAPElement rbd = cabinDefinition.addChildElement("RBD", "ns8");
        rbd.addTextNode("Y"); // Default to economy class
        
        // Add Client type
        SOAPElement client = seatMapQuery.addChildElement("Client", "ns8");
        client.addTextNode("WEB");
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private String generateCorrelationId() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
    
    private SOAPMessage sendSoapRequest(SOAPMessage request) throws SOAPException, SeatmapApiException {
        return sendSoapRequestWithRetry(request, maxRetries);
    }
    
    private SOAPMessage sendSoapRequestWithRetry(SOAPMessage request, int maxRetries) throws SOAPException, SeatmapApiException {
        SOAPException lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            SOAPConnection soapConnection = null;
            
            try {
                soapConnection = soapConnectionFactory.createConnection();
                
                // Log the request for debugging (only on first attempt to avoid spam)
                if (attempt == 1 && logger.isDebugEnabled()) {
                    logger.debug("Sending SOAP request to: {} (attempt {})", endpoint, attempt);
                    logSoapMessage(request);
                }
                
                long startTime = System.currentTimeMillis();
                SOAPMessage response = soapConnection.call(request, endpoint);
                long duration = System.currentTimeMillis() - startTime;
                
                logger.debug("SOAP request completed in {}ms (attempt {})", duration, attempt);
                
                if (logger.isDebugEnabled()) {
                    logger.debug("Received SOAP response:");
                    logSoapMessage(response);
                }
                
                return response;
                
            } catch (SOAPException e) {
                lastException = e;
                logger.warn("SOAP request failed on attempt {} of {}: {}", attempt, maxRetries, e.getMessage());
                
                // Don't retry on authentication errors
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("authentication")) {
                    throw new SeatmapApiException("Authentication failed: " + e.getMessage(), e);
                }
                
                // Wait before retry (exponential backoff)
                if (attempt < maxRetries) {
                    try {
                        long waitTime = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s...
                        logger.debug("Waiting {}ms before retry", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SeatmapApiException("Request interrupted during retry", ie);
                    }
                }
                
            } finally {
                if (soapConnection != null) {
                    try {
                        soapConnection.close();
                    } catch (SOAPException e) {
                        logger.warn("Error closing SOAP connection: {}", e.getMessage());
                    }
                }
            }
        }
        
        // All retries failed
        String errorMsg = String.format("SOAP request failed after %d attempts", maxRetries);
        logger.error(errorMsg, lastException);
        throw new SeatmapApiException(errorMsg, lastException);
    }
    
    private void parseAuthenticationResponse(SOAPMessage response) throws SOAPException, SeatmapApiException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            String faultCode = fault.getFaultCode();
            String faultString = fault.getFaultString();
            logger.error("Sabre authentication SOAP fault - Code: {}, Message: {}", faultCode, faultString);
            throw new SeatmapApiException("Sabre authentication failed: " + faultString);
        }
        
        try {
            // Use XPath to extract the BinarySecurityToken from SessionCreateRS
            Document doc = body.getOwnerDocument();
            
            // Look for BinarySecurityToken in the response
            NodeList tokenNodes = doc.getElementsByTagName("BinarySecurityToken");
            if (tokenNodes.getLength() == 0) {
                // Also try with namespace prefix
                tokenNodes = doc.getElementsByTagNameNS("http://schemas.xmlsoap.org/ws/2002/12/secext", "BinarySecurityToken");
            }
            
            if (tokenNodes.getLength() > 0) {
                Node tokenNode = tokenNodes.item(0);
                String token = tokenNode.getTextContent();
                if (token != null && !token.trim().isEmpty()) {
                    this.sessionToken = token.trim();
                    this.tokenExpiresAt = System.currentTimeMillis() + SESSION_DURATION_MS;
                    logger.info("Successfully extracted Sabre session token");
                    return;
                }
            }
            
            // If we can't find BinarySecurityToken, log the response for debugging
            logger.error("No BinarySecurityToken found in authentication response");
            if (logger.isDebugEnabled()) {
                logSoapMessage(response);
            }
            throw new SeatmapApiException("Failed to extract session token from Sabre authentication response");
            
        } catch (Exception e) {
            logger.error("Error parsing Sabre authentication response", e);
            throw new SeatmapApiException("Failed to parse authentication response: " + e.getMessage(), e);
        }
    }
    
    private JsonNode parseFlightSchedulesResponse(SOAPMessage response) throws SOAPException, SeatmapApiException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            String faultCode = fault.getFaultCode();
            String faultString = fault.getFaultString();
            logger.error("Sabre flight schedules SOAP fault - Code: {}, Message: {}", faultCode, faultString);
            throw new SeatmapApiException("Sabre flight schedules error: " + faultString);
        }
        
        try {
            ObjectNode result = objectMapper.createObjectNode();
            ArrayNode dataArray = objectMapper.createArrayNode();
            
            Document doc = body.getOwnerDocument();
            
            // Look for ACS_AirportFlightListRS v3.0.0 format - AirportFlight elements
            NodeList flightOptions = doc.getElementsByTagName("AirportFlight");
            
            logger.info("Found {} flight options in Sabre response", flightOptions.getLength());
            
            for (int i = 0; i < flightOptions.getLength(); i++) {
                Node flightOptionNode = flightOptions.item(i);
                
                try {
                    ObjectNode flight = parseFlightOption(flightOptionNode);
                    if (flight != null) {
                        dataArray.add(flight);
                    }
                } catch (Exception e) {
                    logger.warn("Error parsing flight option {}: {}", i, e.getMessage());
                }
            }
            
            // If no structured flight data found, log response for debugging
            if (dataArray.size() == 0) {
                logger.warn("No flight options found in Sabre response");
                if (logger.isDebugEnabled()) {
                    logSoapMessage(response);
                }
            }
            
            result.set("data", dataArray);
            
            // Add metadata
            ObjectNode meta = objectMapper.createObjectNode();
            meta.put("count", dataArray.size());
            meta.put("source", "SABRE");
            result.set("meta", meta);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing Sabre flight schedules response", e);
            throw new SeatmapApiException("Failed to parse flight schedules response: " + e.getMessage(), e);
        }
    }
    
    private ObjectNode parseFlightOption(Node flightOptionNode) {
        try {
            ObjectNode flight = objectMapper.createObjectNode();
            flight.put("type", "flight-offer");
            flight.put("id", "sabre_" + UUID.randomUUID().toString().substring(0, 8));
            flight.put("dataSource", "SABRE");
            flight.put("source", "GDS");
            flight.put("instantTicketingRequired", false);
            flight.put("nonHomogeneous", false);
            flight.put("oneWay", false);
            
            // Parse flight segments
            ArrayNode itineraries = objectMapper.createArrayNode();
            ObjectNode itinerary = objectMapper.createObjectNode();
            ArrayNode segments = objectMapper.createArrayNode();
            
            // Extract flight information from the node
            ObjectNode segment = objectMapper.createObjectNode();
            
            // Extract flight information using v3.0.0 AirportFlight structure
            Element element = (Element) flightOptionNode;
            
            // Departure information
            ObjectNode departure = objectMapper.createObjectNode();
            
            // Get origin from the parent Origin element (outside AirportFlight)
            Node parentNode = element.getParentNode();
            String departureCode = null;
            if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
                Element parentElement = (Element) parentNode;
                departureCode = getElementTextContent(parentElement, "Origin");
            }
            // If not found at parent level, look for Destination element in flight (opposite direction)
            String arrivalCode = getElementTextContent(element, "Destination");
            if (departureCode != null) {
                departure.put("iataCode", departureCode);
            }
            
            // Parse departure date and time from AirportFlight elements
            String departureDate = getElementTextContent(element, "DepartureDate");
            String departureTime = getElementTextContent(element, "DepartureTime");
            if (departureDate != null && departureTime != null) {
                // Combine date and time
                String fullDepartureTime = departureDate + "T" + convertToISOTime(departureTime);
                departure.put("at", fullDepartureTime);
            } else {
                departure.put("at", getCurrentTimestamp());
            }
            segment.set("departure", departure);
            
            // Arrival information
            ObjectNode arrival = objectMapper.createObjectNode();
            if (arrivalCode != null) {
                arrival.put("iataCode", arrivalCode);
            }
            
            // For arrival time, estimate based on departure + flight duration (placeholder)
            arrival.put("at", getCurrentTimestamp());
            segment.set("arrival", arrival);
            
            // Flight details - get flight number from Flight element
            String flightNumber = getElementTextContent(element, "Flight");
            if (flightNumber != null) {
                segment.put("number", flightNumber);
                // Extract carrier code from airline code (should be separate but extract from number if needed)
                String carrierCode = flightNumber.replaceAll("\\d+", "").trim();
                if (carrierCode.isEmpty()) {
                    carrierCode = "U0"; // Default carrier - should be configured
                }
                segment.put("carrierCode", carrierCode);
            }
            
            // Aircraft information
            ObjectNode aircraft = objectMapper.createObjectNode();
            String equipmentCode = getElementTextContent(element, "AircraftType");
            if (equipmentCode != null) {
                aircraft.put("code", equipmentCode);
            }
            segment.set("aircraft", aircraft);
            
            // Add flight status information
            String status = getElementTextContent(element, "Status");
            if (status != null) {
                segment.put("status", status);
            }
            
            // Add gate information if available
            String gate = getElementTextContent(element, "DepartureGate");
            if (gate != null) {
                departure.put("gate", gate);
            }
            
            segment.put("id", "1");
            segment.put("numberOfStops", 0);
            segment.put("blacklistedInEU", false);
            
            segments.add(segment);
            itinerary.set("segments", segments);
            itineraries.add(itinerary);
            flight.set("itineraries", itineraries);
            
            return flight;
            
        } catch (Exception e) {
            logger.error("Error parsing individual flight option", e);
            return null;
        }
    }
    
    private String getElementAttribute(Element parent, String elementName, String attributeName) {
        NodeList elements = parent.getElementsByTagName(elementName);
        if (elements.getLength() > 0) {
            Element element = (Element) elements.item(0);
            if (attributeName.isEmpty()) {
                return element.getTextContent();
            } else {
                return element.getAttribute(attributeName);
            }
        }
        return null;
    }
    
    private java.util.Optional<String> getElementAttributeOptional(Element parent, String elementName, String attributeName) {
        String value = getElementAttribute(parent, elementName, attributeName);
        return value != null ? java.util.Optional.of(value) : java.util.Optional.empty();
    }
    
    private String getElementTextContent(Element parent, String elementName) {
        NodeList elements = parent.getElementsByTagName(elementName);
        if (elements.getLength() > 0) {
            Element element = (Element) elements.item(0);
            return element.getTextContent();
        }
        return null;
    }
    
    private String convertToISOTime(String sabreTime) {
        // Convert Sabre time format (e.g., "05:05AM") to ISO format (e.g., "05:05:00")
        if (sabreTime == null || sabreTime.trim().isEmpty()) {
            return "00:00:00";
        }
        
        try {
            String cleanTime = sabreTime.trim().toUpperCase();
            boolean isPM = cleanTime.endsWith("PM");
            boolean isAM = cleanTime.endsWith("AM");
            
            if (!isPM && !isAM) {
                // Already in 24-hour format or no AM/PM indicator
                return cleanTime.length() == 5 ? cleanTime + ":00" : cleanTime;
            }
            
            // Remove AM/PM indicator
            String timeOnly = cleanTime.substring(0, cleanTime.length() - 2);
            String[] parts = timeOnly.split(":");
            
            if (parts.length != 2) {
                return "00:00:00";
            }
            
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            
            // Convert to 24-hour format
            if (isPM && hours != 12) {
                hours += 12;
            } else if (isAM && hours == 12) {
                hours = 0;
            }
            
            return String.format("%02d:%02d:00", hours, minutes);
            
        } catch (Exception e) {
            logger.warn("Failed to convert time format: {}", sabreTime, e);
            return "00:00:00";
        }
    }
    
    private JsonNode parseSeatMapResponse(SOAPMessage response) throws SOAPException, SeatmapApiException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            String faultCode = fault.getFaultCode();
            String faultString = fault.getFaultString();
            logger.error("Sabre seat map SOAP fault - Code: {}, Message: {}", faultCode, faultString);
            throw new SeatmapApiException("Sabre seat map error: " + faultString);
        }
        
        try {
            Document doc = body.getOwnerDocument();
            
            ObjectNode result = objectMapper.createObjectNode();
            result.put("success", true);
            
            // Look for seat map data in the v8.0.0 response format
            NodeList seatMapNodes = doc.getElementsByTagName("EnhancedSeatMapRS");
            if (seatMapNodes.getLength() == 0) {
                // Look for SeatMap element directly within EnhancedSeatMapRS
                seatMapNodes = doc.getElementsByTagName("SeatMap");
            }
            
            if (seatMapNodes.getLength() > 0) {
                ObjectNode seatMapData = parseSeatMapData(seatMapNodes.item(0));
                result.set("data", seatMapData);
            } else {
                // Create minimal response structure if no detailed seat map found
                ObjectNode seatMapData = objectMapper.createObjectNode();
                seatMapData.put("type", "seatmap");
                seatMapData.put("source", "SABRE");
                seatMapData.put("carrierCode", "XX");
                seatMapData.put("flightNumber", "000");
                
                // Add basic deck structure
                ObjectNode deck = objectMapper.createObjectNode();
                deck.put("deckType", "MAIN");
                
                ObjectNode deckConfig = objectMapper.createObjectNode();
                deckConfig.put("width", 6);
                deckConfig.put("length", 30);
                deck.set("deckConfiguration", deckConfig);
                
                ArrayNode facilities = objectMapper.createArrayNode();
                deck.set("facilities", facilities);
                
                seatMapData.set("deck", deck);
                result.set("data", seatMapData);
                
                logger.warn("No detailed seat map data found in Sabre response, returning basic structure");
                if (logger.isDebugEnabled()) {
                    logSoapMessage(response);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error parsing Sabre seat map response", e);
            throw new SeatmapApiException("Failed to parse seat map response: " + e.getMessage(), e);
        }
    }
    
    private ObjectNode parseSeatMapData(Node seatMapNode) {
        ObjectNode seatMapData = objectMapper.createObjectNode();
        seatMapData.put("type", "seatmap");
        seatMapData.put("source", "SABRE");
        
        try {
            Element seatMapElement = (Element) seatMapNode;
            
            // Extract flight information
            String carrierCode = getElementAttribute(seatMapElement, "Carrier", "Code");
            if (carrierCode == null) carrierCode = "XX";
            seatMapData.put("carrierCode", carrierCode);
            
            String flightNumber = getElementAttributeOptional(seatMapElement, "FlightNumber", "")
                    .orElse("000");
            seatMapData.put("flightNumber", flightNumber);
            
            // Extract departure/arrival information
            ObjectNode departure = objectMapper.createObjectNode();
            String departureCode = getElementAttribute(seatMapElement, "DepartureAirport", "LocationCode");
            if (departureCode != null) {
                departure.put("iataCode", departureCode);
                seatMapData.set("departure", departure);
            }
            
            ObjectNode arrival = objectMapper.createObjectNode();
            String arrivalCode = getElementAttribute(seatMapElement, "ArrivalAirport", "LocationCode");
            if (arrivalCode != null) {
                arrival.put("iataCode", arrivalCode);
                seatMapData.set("arrival", arrival);
            }
            
            // Parse cabin/deck information
            NodeList cabins = seatMapElement.getElementsByTagName("CabinClass");
            if (cabins.getLength() == 0) {
                cabins = seatMapElement.getElementsByTagName("Cabin");
            }
            
            if (cabins.getLength() > 0) {
                ObjectNode deck = parseCabinInfo(cabins.item(0));
                seatMapData.set("deck", deck);
            } else {
                // Create basic deck structure
                ObjectNode deck = objectMapper.createObjectNode();
                deck.put("deckType", "MAIN");
                
                ObjectNode deckConfig = objectMapper.createObjectNode();
                deckConfig.put("width", 6);
                deckConfig.put("length", 30);
                deck.set("deckConfiguration", deckConfig);
                
                ArrayNode facilities = objectMapper.createArrayNode();
                deck.set("facilities", facilities);
                
                seatMapData.set("deck", deck);
            }
            
        } catch (Exception e) {
            logger.error("Error parsing seat map data details", e);
        }
        
        return seatMapData;
    }
    
    private ObjectNode parseCabinInfo(Node cabinNode) {
        ObjectNode deck = objectMapper.createObjectNode();
        deck.put("deckType", "MAIN");
        
        try {
            Element cabinElement = (Element) cabinNode;
            
            // Extract cabin configuration
            ObjectNode deckConfig = objectMapper.createObjectNode();
            deckConfig.put("width", 6); // Default values
            deckConfig.put("length", 30);
            deck.set("deckConfiguration", deckConfig);
            
            // Parse seats/facilities
            ArrayNode facilities = objectMapper.createArrayNode();
            NodeList seats = cabinElement.getElementsByTagName("Seat");
            
            for (int i = 0; i < seats.getLength() && i < 50; i++) { // Limit to 50 seats to avoid huge responses
                Node seatNode = seats.item(i);
                ObjectNode facility = parseSeatInfo(seatNode, i);
                if (facility != null) {
                    facilities.add(facility);
                }
            }
            
            deck.set("facilities", facilities);
            
        } catch (Exception e) {
            logger.error("Error parsing cabin information", e);
        }
        
        return deck;
    }
    
    private ObjectNode parseSeatInfo(Node seatNode, int index) {
        try {
            ObjectNode facility = objectMapper.createObjectNode();
            Element seatElement = (Element) seatNode;
            
            // Seat identifier
            String seatId = seatElement.getAttribute("SeatNumber");
            if (seatId == null || seatId.isEmpty()) {
                seatId = String.valueOf(index + 1) + "A"; // Generate basic seat ID
            }
            facility.put("code", seatId);
            facility.put("type", "SEAT");
            
            // Coordinates (simplified)
            ObjectNode coordinates = objectMapper.createObjectNode();
            coordinates.put("x", (index % 6) + 1);
            coordinates.put("y", (index / 6) + 1);
            facility.set("coordinates", coordinates);
            
            // Traveler pricing (basic structure)
            ArrayNode travelerPricing = objectMapper.createArrayNode();
            ObjectNode pricing = objectMapper.createObjectNode();
            pricing.put("travelerId", "1");
            pricing.put("seatAvailabilityStatus", "AVAILABLE");
            
            ObjectNode price = objectMapper.createObjectNode();
            price.put("currency", "USD");
            price.put("total", "0.00");
            price.put("base", "0.00");
            pricing.set("price", price);
            
            travelerPricing.add(pricing);
            facility.set("travelerPricing", travelerPricing);
            
            // Characteristics
            ArrayNode characteristics = objectMapper.createArrayNode();
            // Add basic characteristics based on seat position
            if ((index % 6) == 0 || (index % 6) == 5) {
                characteristics.add("W"); // Window
            }
            if ((index % 3) == 1) {
                characteristics.add("A"); // Aisle
            }
            facility.set("characteristicsCodes", characteristics);
            
            return facility;
            
        } catch (Exception e) {
            logger.error("Error parsing seat information for index {}", index, e);
            return null;
        }
    }
    
    private String extractTextFromSoapBody(SOAPBody body) {
        try {
            StringWriter writer = new StringWriter();
            TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(body), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            logger.error("Failed to extract text from SOAP body", e);
            return "";
        }
    }
    
    private void logSoapMessage(SOAPMessage message) {
        try {
            StringWriter writer = new StringWriter();
            TransformerFactory.newInstance().newTransformer()
                .transform(new DOMSource(message.getSOAPPart()), new StreamResult(writer));
            logger.debug("SOAP Message: {}", writer.toString());
        } catch (Exception e) {
            logger.error("Failed to log SOAP message", e);
        }
    }
}