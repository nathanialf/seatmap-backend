package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.exception.SeatmapException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.soap.*;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class SabreService {
    private static final Logger logger = LoggerFactory.getLogger(SabreService.class);
    
    private final String userId;
    private final String password;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final SOAPConnectionFactory soapConnectionFactory;
    
    private String sessionToken;
    private long tokenExpiresAt;
    
    public SabreService() {
        this.userId = System.getenv("SABRE_USER_ID");
        this.password = System.getenv("SABRE_PASSWORD");
        this.endpoint = System.getenv("SABRE_ENDPOINT");
        
        if (userId == null || password == null || endpoint == null) {
            throw new IllegalStateException("Sabre API credentials not configured");
        }
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
        
        try {
            this.soapConnectionFactory = SOAPConnectionFactory.newInstance();
        } catch (SOAPException e) {
            throw new RuntimeException("Failed to initialize SOAP connection factory", e);
        }
    }
    
    public JsonNode searchFlightSchedules(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SeatmapException {
        try {
            ensureValidSession();
            
            SOAPMessage soapRequest = createFlightSchedulesRequest(origin, destination, departureDate, travelClass, flightNumber, maxResults);
            SOAPMessage soapResponse = sendSoapRequest(soapRequest);
            
            return parseFlightSchedulesResponse(soapResponse);
            
        } catch (Exception e) {
            logger.error("Error calling Sabre Flight Schedules API", e);
            throw new SeatmapException("Network error calling Sabre API", e);
        }
    }
    
    public JsonNode getSeatMapFromFlight(String carrierCode, String flightNumber, String departureDate, String origin, String destination) throws SeatmapException {
        try {
            ensureValidSession();
            
            SOAPMessage soapRequest = createSeatMapRequest(carrierCode, flightNumber, departureDate, origin, destination);
            SOAPMessage soapResponse = sendSoapRequest(soapRequest);
            
            return parseSeatMapResponse(soapResponse);
            
        } catch (Exception e) {
            logger.error("Error calling Sabre Seat Map API", e);
            throw new SeatmapException("Network error calling Sabre Seat Map API", e);
        }
    }
    
    private void ensureValidSession() throws SeatmapException {
        if (sessionToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            authenticateSession();
        }
    }
    
    private void authenticateSession() throws SeatmapException {
        try {
            SOAPMessage authRequest = createAuthenticationRequest();
            SOAPMessage authResponse = sendSoapRequest(authRequest);
            
            parseAuthenticationResponse(authResponse);
            
            logger.info("Successfully authenticated with Sabre API");
            
        } catch (Exception e) {
            logger.error("Failed to authenticate with Sabre API", e);
            throw new SeatmapException("Authentication failed with Sabre API", e);
        }
    }
    
    private SOAPMessage createAuthenticationRequest() throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        envelope.addNamespaceDeclaration("sec", "http://schemas.sabre.com/ws/2005/05/security");
        
        // SOAP Header with Security
        SOAPHeader header = envelope.getHeader();
        SOAPElement security = header.addChildElement("Security", "sec");
        SOAPElement usernameToken = security.addChildElement("UsernameToken");
        usernameToken.addChildElement("Username").addTextNode(userId);
        usernameToken.addChildElement("Password").addTextNode(password);
        usernameToken.addChildElement("Organization").addTextNode("1S"); // Default Sabre org
        usernameToken.addChildElement("Domain").addTextNode("DEFAULT");
        
        // SOAP Body
        SOAPBody body = envelope.getBody();
        SOAPElement sessionCreate = body.addChildElement("SessionCreateRQ", "", "http://www.opentravel.org/OTA/2002/11");
        sessionCreate.addAttribute(envelope.createName("Version"), "2.0.0");
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private SOAPMessage createFlightSchedulesRequest(String origin, String destination, String departureDate, String travelClass, String flightNumber, Integer maxResults) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        
        // Add session header
        SOAPHeader header = envelope.getHeader();
        SOAPElement security = header.addChildElement("Security", "sec", "http://schemas.sabre.com/ws/2005/05/security");
        SOAPElement binaryToken = security.addChildElement("BinarySecurityToken");
        binaryToken.addTextNode(sessionToken);
        
        // SOAP Body
        SOAPBody body = envelope.getBody();
        SOAPElement flightSchedulesRQ = body.addChildElement("OTA_AirScheduleRQ", "", "http://www.opentravel.org/OTA/2002/11");
        flightSchedulesRQ.addAttribute(envelope.createName("Version"), "2.2.0");
        
        // Origin and Destination
        SOAPElement originDestination = flightSchedulesRQ.addChildElement("OriginDestinationInformation");
        originDestination.addChildElement("DepartureDateTime").addTextNode(departureDate + "T00:00:00");
        originDestination.addChildElement("OriginLocation").addAttribute(envelope.createName("LocationCode"), origin);
        originDestination.addChildElement("DestinationLocation").addAttribute(envelope.createName("LocationCode"), destination);
        
        // Flight number filter if specified
        if (flightNumber != null && !flightNumber.trim().isEmpty()) {
            SOAPElement flightTypePref = flightSchedulesRQ.addChildElement("FlightTypePref");
            flightTypePref.addAttribute(envelope.createName("FlightTypeCode"), flightNumber);
        }
        
        // Max results
        if (maxResults != null) {
            flightSchedulesRQ.addAttribute(envelope.createName("MaxResponses"), maxResults.toString());
        }
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private SOAPMessage createSeatMapRequest(String carrierCode, String flightNumber, String departureDate, String origin, String destination) throws SOAPException {
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        
        // Add session header
        SOAPHeader header = envelope.getHeader();
        SOAPElement security = header.addChildElement("Security", "sec", "http://schemas.sabre.com/ws/2005/05/security");
        SOAPElement binaryToken = security.addChildElement("BinarySecurityToken");
        binaryToken.addTextNode(sessionToken);
        
        // SOAP Body
        SOAPBody body = envelope.getBody();
        SOAPElement seatMapRQ = body.addChildElement("GetSeatMapRQ", "", "http://webservices.sabre.com/servicesplatform/eiapi/1.0.0");
        seatMapRQ.addAttribute(envelope.createName("Version"), "3.1.0");
        
        // Flight info
        SOAPElement flightInfo = seatMapRQ.addChildElement("FlightInfo");
        flightInfo.addChildElement("Flight")
            .addAttribute(envelope.createName("Number"), flightNumber)
            .getParentNode()
            .appendChild(flightInfo.getOwnerDocument().createElement("CarrierCode"))
            .setTextContent(carrierCode);
        
        flightInfo.addChildElement("DepartureDateTime").addTextNode(departureDate + "T00:00:00");
        flightInfo.addChildElement("OriginLocation").addAttribute(envelope.createName("LocationCode"), origin);
        flightInfo.addChildElement("DestinationLocation").addAttribute(envelope.createName("LocationCode"), destination);
        
        soapMessage.saveChanges();
        return soapMessage;
    }
    
    private SOAPMessage sendSoapRequest(SOAPMessage request) throws SOAPException {
        SOAPConnection soapConnection = soapConnectionFactory.createConnection();
        
        try {
            // Log the request for debugging
            if (logger.isDebugEnabled()) {
                logger.debug("Sending SOAP request to: {}", endpoint);
                logSoapMessage(request);
            }
            
            SOAPMessage response = soapConnection.call(request, endpoint);
            
            if (logger.isDebugEnabled()) {
                logger.debug("Received SOAP response:");
                logSoapMessage(response);
            }
            
            return response;
            
        } finally {
            soapConnection.close();
        }
    }
    
    private void parseAuthenticationResponse(SOAPMessage response) throws SOAPException, SeatmapException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            throw new SeatmapException("Sabre authentication failed: " + fault.getFaultString());
        }
        
        // Extract session token from response
        // This is a simplified extraction - actual implementation depends on Sabre's response format
        String responseText = extractTextFromSoapBody(body);
        if (responseText.contains("BinarySecurityToken")) {
            // Extract token between tags (simplified parsing)
            int startIndex = responseText.indexOf("<BinarySecurityToken>") + 21;
            int endIndex = responseText.indexOf("</BinarySecurityToken>");
            if (startIndex > 20 && endIndex > startIndex) {
                this.sessionToken = responseText.substring(startIndex, endIndex);
                this.tokenExpiresAt = System.currentTimeMillis() + (60 * 60 * 1000); // 1 hour
            }
        }
        
        if (sessionToken == null) {
            throw new SeatmapException("Failed to extract session token from Sabre authentication response");
        }
    }
    
    private JsonNode parseFlightSchedulesResponse(SOAPMessage response) throws SOAPException, SeatmapException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            throw new SeatmapException("Sabre flight schedules error: " + fault.getFaultString());
        }
        
        // Convert SOAP response to JSON format compatible with our existing structure
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode dataArray = objectMapper.createArrayNode();
        
        // Parse the SOAP XML and extract flight information
        // This is a simplified parsing - actual implementation depends on Sabre's response format
        String responseText = extractTextFromSoapBody(body);
        
        // Create mock flight offers in Amadeus-compatible format with Sabre source
        ObjectNode flight = objectMapper.createObjectNode();
        flight.put("type", "flight-offer");
        flight.put("id", "sabre_" + System.currentTimeMillis());
        flight.put("source", "SABRE");
        flight.put("instantTicketingRequired", false);
        flight.put("nonHomogeneous", false);
        flight.put("oneWay", false);
        
        // Add to results
        dataArray.add(flight);
        result.set("data", dataArray);
        
        return result;
    }
    
    private JsonNode parseSeatMapResponse(SOAPMessage response) throws SOAPException, SeatmapException {
        SOAPBody body = response.getSOAPBody();
        
        if (body.hasFault()) {
            SOAPFault fault = body.getFault();
            throw new SeatmapException("Sabre seat map error: " + fault.getFaultString());
        }
        
        // Convert SOAP response to JSON format compatible with Amadeus structure
        ObjectNode result = objectMapper.createObjectNode();
        ObjectNode meta = objectMapper.createObjectNode();
        meta.put("count", 1);
        result.set("meta", meta);
        
        ArrayNode dataArray = objectMapper.createArrayNode();
        ObjectNode seatMapData = objectMapper.createObjectNode();
        seatMapData.put("type", "seatmap");
        seatMapData.put("source", "SABRE");
        
        dataArray.add(seatMapData);
        result.set("data", dataArray);
        
        return result;
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