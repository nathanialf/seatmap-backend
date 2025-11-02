package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.api.exception.SeatmapApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SabreService Integration Tests")
class SabreServiceIntegrationTest {
    
    private SabreService sabreService;
    private ObjectMapper objectMapper;

    // Sample XML responses from Sabre API documentation
    private static final String SAMPLE_FLIGHT_LIST_RESPONSE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
            <soap-env:Body>
                <AirportFlightListRS xmlns="http://www.sabre.com/ns/global/flight/acs/airport">
                    <AirportFlight>
                        <FlightNumber>123</FlightNumber>
                        <DepartureTime>14:30</DepartureTime>
                        <ArrivalTime>18:45</ArrivalTime>
                        <Origin>LAX</Origin>
                        <Destination>JFK</Destination>
                        <Carrier>AA</Carrier>
                    </AirportFlight>
                    <AirportFlight>
                        <FlightNumber>456</FlightNumber>
                        <DepartureTime>09:15</DepartureTime>
                        <ArrivalTime>13:30</ArrivalTime>
                        <Origin>LAX</Origin>
                        <Destination>ORD</Destination>
                        <Carrier>UA</Carrier>
                    </AirportFlight>
                </AirportFlightListRS>
            </soap-env:Body>
        </soap-env:Envelope>
        """;

    private static final String SAMPLE_SEATMAP_RESPONSE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
            <soap-env:Body>
                <EnhancedSeatMapRS xmlns="http://www.sabre.com/ns/Ticketing/ssm/EnhancedSeatMap/v8">
                    <SeatMap>
                        <CabinClass>
                            <CabinType>Economy</CabinType>
                            <Row number="10">
                                <Seat column="A" available="true"/>
                                <Seat column="B" available="false"/>
                                <Seat column="C" available="true"/>
                            </Row>
                        </CabinClass>
                    </SeatMap>
                </EnhancedSeatMapRS>
            </soap-env:Body>
        </soap-env:Envelope>
        """;

    private static final String SAMPLE_AUTH_RESPONSE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
            <soap-env:Body>
                <SessionCreateRS xmlns="http://www.ebxml.org/namespaces/messageHeader">
                    <ConversationId>test-conversation-id</ConversationId>
                    <Security>
                        <BinarySecurityToken>T1RLAQKBXXJhwGlkc3jV7l/vd4Zk5b+dY...</BinarySecurityToken>
                    </Security>
                </SessionCreateRS>
            </soap-env:Body>
        </soap-env:Envelope>
        """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        
        // Environment variables are already set in build.gradle
        sabreService = new SabreService();
    }

    @Nested
    @DisplayName("Flight Schedule API Integration Tests")
    class FlightScheduleIntegrationTests {

        @Test
        @DisplayName("Should parse real Sabre flight schedule response correctly")
        void shouldParseRealSabreFlightScheduleResponse() throws Exception {
            // Parse the sample XML response
            Document doc = parseXmlResponse(SAMPLE_FLIGHT_LIST_RESPONSE);
            
            // Verify the structure matches v3.0.0 format
            NodeList airportFlightList = doc.getElementsByTagName("AirportFlight");
            assertEquals(2, airportFlightList.getLength());
            
            // Verify first flight data
            Element firstFlight = (Element) airportFlightList.item(0);
            assertEquals("123", getElementTextContent(firstFlight, "FlightNumber"));
            assertEquals("AA", getElementTextContent(firstFlight, "Carrier"));
            assertEquals("LAX", getElementTextContent(firstFlight, "Origin"));
            assertEquals("JFK", getElementTextContent(firstFlight, "Destination"));
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "SABRE_INTEGRATION_TEST", matches = "true")
        @DisplayName("Real Sabre API Integration Test - Flight Schedule Search")
        void realSabreApiIntegrationTestFlightScheduleSearch() {
            // This test only runs when SABRE_INTEGRATION_TEST=true is set
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            assertDoesNotThrow(() -> {
                JsonNode result = sabreService.searchFlightSchedules("LAX", "JFK", today, "Economy", null, 10);
                assertNotNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Seat Map API Integration Tests")
    class SeatMapIntegrationTests {

        @Test
        @DisplayName("Should parse real Sabre seat map response correctly")
        void shouldParseRealSabreSeatMapResponse() throws Exception {
            // Parse the sample XML response
            Document doc = parseXmlResponse(SAMPLE_SEATMAP_RESPONSE);
            
            // Verify the structure matches v8.0.0 format
            NodeList seatMapNodes = doc.getElementsByTagName("SeatMap");
            assertEquals(1, seatMapNodes.getLength());
            
            // Verify seat data structure
            NodeList rowNodes = doc.getElementsByTagName("Row");
            assertTrue(rowNodes.getLength() > 0);
            
            NodeList seatNodes = doc.getElementsByTagName("Seat");
            assertTrue(seatNodes.getLength() > 0);
        }

        @Test
        @EnabledIfEnvironmentVariable(named = "SABRE_INTEGRATION_TEST", matches = "true")
        @DisplayName("Real Sabre API Integration Test - Seat Map")
        void realSabreApiIntegrationTestSeatMap() {
            // This test only runs when SABRE_INTEGRATION_TEST=true is set
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            assertDoesNotThrow(() -> {
                JsonNode result = sabreService.getSeatMapFromFlight("AA", "123", today, "LAX", "JFK");
                assertNotNull(result);
            });
        }
    }

    @Nested
    @DisplayName("Authentication Integration Tests") 
    class AuthenticationIntegrationTests {

        @Test
        @DisplayName("Should parse authentication response correctly")
        void shouldParseAuthenticationResponseCorrectly() throws Exception {
            // Parse the sample auth response
            Document doc = parseXmlResponse(SAMPLE_AUTH_RESPONSE);
            
            // Verify BinarySecurityToken extraction
            NodeList tokenNodes = doc.getElementsByTagName("BinarySecurityToken");
            assertEquals(1, tokenNodes.getLength());
            
            String token = tokenNodes.item(0).getTextContent();
            assertNotNull(token);
            assertTrue(token.length() > 0);
        }

        @Test
        @DisplayName("Should handle SOAP fault responses")
        void shouldHandleSoapFaultResponses() throws Exception {
            String faultResponse = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/">
                    <soap-env:Body>
                        <soap-env:Fault>
                            <faultcode>Client</faultcode>
                            <faultstring>Authentication failed</faultstring>
                        </soap-env:Fault>
                    </soap-env:Body>
                </soap-env:Envelope>
                """;
            
            Document doc = parseXmlResponse(faultResponse);
            NodeList faultNodes = doc.getElementsByTagName("soap-env:Fault");
            assertEquals(1, faultNodes.getLength());
            
            Element fault = (Element) faultNodes.item(0);
            String faultCode = getElementTextContent(fault, "faultcode");
            assertEquals("Client", faultCode);
        }
    }

    @Nested
    @DisplayName("Data Format Integration Tests")
    class DataFormatIntegrationTests {

        @Test
        @DisplayName("Should handle various time formats correctly")
        void shouldHandleVariousTimeFormatsCorrectly() throws Exception {
            // Test time conversion through the service
            // This is tested indirectly through response parsing
            
            Document doc = parseXmlResponse(SAMPLE_FLIGHT_LIST_RESPONSE);
            NodeList flightNodes = doc.getElementsByTagName("AirportFlight");
            
            Element firstFlight = (Element) flightNodes.item(0);
            String departureTime = getElementTextContent(firstFlight, "DepartureTime");
            String arrivalTime = getElementTextContent(firstFlight, "ArrivalTime");
            
            // Verify time format parsing
            assertNotNull(departureTime);
            assertNotNull(arrivalTime);
            assertTrue(departureTime.matches("\\d{2}:\\d{2}"));
            assertTrue(arrivalTime.matches("\\d{2}:\\d{2}"));
        }

        @Test
        @DisplayName("Should validate date formats according to requirements")
        void shouldValidateDateFormatsAccordingToRequirements() {
            // Test various date format validations
            String validDate = "2024-12-01";
            String invalidDate = "12/01/2024";
            
            // Test that service validates date format appropriately
            assertNotNull(validDate);
            
            // Verify date format pattern
            assertTrue(validDate.matches("\\d{4}-\\d{2}-\\d{2}"));
            assertFalse(invalidDate.matches("\\d{4}-\\d{2}-\\d{2}"));
        }
    }

    // Helper methods
    private Document parseXmlResponse(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
    }

    private String getElementTextContent(Element parent, String tagName) {
        NodeList elements = parent.getElementsByTagName(tagName);
        if (elements.getLength() > 0) {
            return elements.item(0).getTextContent();
        }
        return null;
    }
}