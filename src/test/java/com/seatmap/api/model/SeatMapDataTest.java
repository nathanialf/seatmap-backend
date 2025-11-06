package com.seatmap.api.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void defaultConstructor_CreatesEmptySeatMapData() {
        SeatMapData seatMapData = new SeatMapData();

        assertNull(seatMapData.getDecks());
        assertNull(seatMapData.getAircraft());
        assertNull(seatMapData.getFlight());
        assertNull(seatMapData.getSeats());
        assertNull(seatMapData.getLayout());
        assertNull(seatMapData.getSource());
    }

    @Test
    void constructorWithAllFields_SetsAllFields() {
        // Arrange
        List<SeatMapData.SeatMapDeck> decks = Arrays.asList(new SeatMapData.SeatMapDeck());
        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo("320", "Airbus A320");
        SeatMapData.FlightInfo flight = new SeatMapData.FlightInfo();
        List<SeatMapData.Seat> seats = Arrays.asList(new SeatMapData.Seat());
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo(30, 6, "3-3");
        String source = "AMADEUS";

        // Act
        SeatMapData seatMapData = new SeatMapData(decks, aircraft, flight, seats, layout, source);

        // Assert
        assertEquals(decks, seatMapData.getDecks());
        assertEquals(aircraft, seatMapData.getAircraft());
        assertEquals(flight, seatMapData.getFlight());
        assertEquals(seats, seatMapData.getSeats());
        assertEquals(layout, seatMapData.getLayout());
        assertEquals(source, seatMapData.getSource());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        SeatMapData seatMapData = new SeatMapData();
        List<SeatMapData.SeatMapDeck> decks = Arrays.asList(new SeatMapData.SeatMapDeck());
        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo("737", "Boeing 737");
        SeatMapData.FlightInfo flight = new SeatMapData.FlightInfo();
        List<SeatMapData.Seat> seats = Arrays.asList(new SeatMapData.Seat());
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo(25, 6, "3-3");
        String source = "SABRE";

        seatMapData.setDecks(decks);
        seatMapData.setAircraft(aircraft);
        seatMapData.setFlight(flight);
        seatMapData.setSeats(seats);
        seatMapData.setLayout(layout);
        seatMapData.setSource(source);

        assertEquals(decks, seatMapData.getDecks());
        assertEquals(aircraft, seatMapData.getAircraft());
        assertEquals(flight, seatMapData.getFlight());
        assertEquals(seats, seatMapData.getSeats());
        assertEquals(layout, seatMapData.getLayout());
        assertEquals(source, seatMapData.getSource());
    }

    @Test
    void fromJsonNode_CreatesWithSource() throws Exception {
        JsonNode testJson = objectMapper.createObjectNode();
        String source = "AMADEUS";

        SeatMapData result = SeatMapData.fromJsonNode(testJson, source);

        assertNotNull(result);
        assertEquals(source, result.getSource());
    }

    @Test
    void seatMapDeck_DefaultConstructor_CreatesEmpty() {
        SeatMapData.SeatMapDeck deck = new SeatMapData.SeatMapDeck();

        assertNull(deck.getDeckType());
        assertNull(deck.getDeckConfiguration());
        assertNull(deck.getFacilities());
        assertNull(deck.getSeats());
    }

    @Test
    void seatMapDeck_SettersAndGetters_WorkCorrectly() throws Exception {
        SeatMapData.SeatMapDeck deck = new SeatMapData.SeatMapDeck();
        JsonNode config = objectMapper.createObjectNode();
        List<JsonNode> facilities = Arrays.asList(objectMapper.createObjectNode());
        List<SeatMapData.Seat> seats = Arrays.asList(new SeatMapData.Seat());

        deck.setDeckType("MAIN");
        deck.setDeckConfiguration(config);
        deck.setFacilities(facilities);
        deck.setSeats(seats);

        assertEquals("MAIN", deck.getDeckType());
        assertEquals(config, deck.getDeckConfiguration());
        assertEquals(facilities, deck.getFacilities());
        assertEquals(seats, deck.getSeats());
    }

    @Test
    void aircraftInfo_DefaultConstructor_CreatesEmpty() {
        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo();

        assertNull(aircraft.getCode());
        assertNull(aircraft.getName());
    }

    @Test
    void aircraftInfo_ConstructorWithParameters_SetsFields() {
        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo("A380", "Airbus A380");

        assertEquals("A380", aircraft.getCode());
        assertEquals("Airbus A380", aircraft.getName());
    }

    @Test
    void aircraftInfo_SettersAndGetters_WorkCorrectly() {
        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo();

        aircraft.setCode("777");
        aircraft.setName("Boeing 777");

        assertEquals("777", aircraft.getCode());
        assertEquals("Boeing 777", aircraft.getName());
    }

    @Test
    void flightInfo_DefaultConstructor_CreatesEmpty() {
        SeatMapData.FlightInfo flight = new SeatMapData.FlightInfo();

        assertNull(flight.getNumber());
        assertNull(flight.getCarrierCode());
        assertNull(flight.getDeparture());
        assertNull(flight.getArrival());
        assertNull(flight.getOperating());
    }

    @Test
    void flightInfo_SettersAndGetters_WorkCorrectly() throws Exception {
        SeatMapData.FlightInfo flight = new SeatMapData.FlightInfo();
        SeatMapData.FlightInfo.DepartureInfo departure = new SeatMapData.FlightInfo.DepartureInfo();
        SeatMapData.FlightInfo.ArrivalInfo arrival = new SeatMapData.FlightInfo.ArrivalInfo();
        JsonNode operating = objectMapper.createObjectNode();

        flight.setNumber("AA123");
        flight.setCarrierCode("AA");
        flight.setDeparture(departure);
        flight.setArrival(arrival);
        flight.setOperating(operating);

        assertEquals("AA123", flight.getNumber());
        assertEquals("AA", flight.getCarrierCode());
        assertEquals(departure, flight.getDeparture());
        assertEquals(arrival, flight.getArrival());
        assertEquals(operating, flight.getOperating());
    }

    @Test
    void departureInfo_DefaultConstructor_CreatesEmpty() {
        SeatMapData.FlightInfo.DepartureInfo departure = new SeatMapData.FlightInfo.DepartureInfo();

        assertNull(departure.getIataCode());
        assertNull(departure.getTerminal());
        assertNull(departure.getAt());
    }

    @Test
    void departureInfo_SettersAndGetters_WorkCorrectly() {
        SeatMapData.FlightInfo.DepartureInfo departure = new SeatMapData.FlightInfo.DepartureInfo();

        departure.setIataCode("LAX");
        departure.setTerminal("4");
        departure.setAt("2025-01-15T10:30:00");

        assertEquals("LAX", departure.getIataCode());
        assertEquals("4", departure.getTerminal());
        assertEquals("2025-01-15T10:30:00", departure.getAt());
    }

    @Test
    void arrivalInfo_DefaultConstructor_CreatesEmpty() {
        SeatMapData.FlightInfo.ArrivalInfo arrival = new SeatMapData.FlightInfo.ArrivalInfo();

        assertNull(arrival.getIataCode());
        assertNull(arrival.getTerminal());
        assertNull(arrival.getAt());
    }

    @Test
    void arrivalInfo_SettersAndGetters_WorkCorrectly() {
        SeatMapData.FlightInfo.ArrivalInfo arrival = new SeatMapData.FlightInfo.ArrivalInfo();

        arrival.setIataCode("SFO");
        arrival.setTerminal("1");
        arrival.setAt("2025-01-15T13:45:00");

        assertEquals("SFO", arrival.getIataCode());
        assertEquals("1", arrival.getTerminal());
        assertEquals("2025-01-15T13:45:00", arrival.getAt());
    }

    @Test
    void seat_DefaultConstructor_CreatesEmpty() {
        SeatMapData.Seat seat = new SeatMapData.Seat();

        assertNull(seat.getNumber());
        assertNull(seat.getCabin());
        assertNull(seat.getCharacteristicsCodes());
        assertNull(seat.getCoordinates());
        assertNull(seat.getTravelerPricing());
    }

    @Test
    void seat_SettersAndGetters_WorkCorrectly() throws Exception {
        SeatMapData.Seat seat = new SeatMapData.Seat();
        List<String> characteristics = Arrays.asList("WINDOW", "AISLE");
        JsonNode coordinates = objectMapper.createObjectNode();
        List<JsonNode> pricing = Arrays.asList(objectMapper.createObjectNode());

        seat.setNumber("1A");
        seat.setCabin("ECONOMY");
        seat.setCharacteristicsCodes(characteristics);
        seat.setCoordinates(coordinates);
        seat.setTravelerPricing(pricing);

        assertEquals("1A", seat.getNumber());
        assertEquals("ECONOMY", seat.getCabin());
        assertEquals(characteristics, seat.getCharacteristicsCodes());
        assertEquals(coordinates, seat.getCoordinates());
        assertEquals(pricing, seat.getTravelerPricing());
    }

    @Test
    void layoutInfo_DefaultConstructor_CreatesEmpty() {
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo();

        assertEquals(0, layout.getTotalRows());
        assertEquals(0, layout.getTotalColumns());
        assertNull(layout.getConfiguration());
    }

    @Test
    void layoutInfo_ConstructorWithParameters_SetsFields() {
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo(40, 6, "3-3");

        assertEquals(40, layout.getTotalRows());
        assertEquals(6, layout.getTotalColumns());
        assertEquals("3-3", layout.getConfiguration());
    }

    @Test
    void layoutInfo_SettersAndGetters_WorkCorrectly() {
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo();

        layout.setTotalRows(50);
        layout.setTotalColumns(8);
        layout.setConfiguration("2-4-2");

        assertEquals(50, layout.getTotalRows());
        assertEquals(8, layout.getTotalColumns());
        assertEquals("2-4-2", layout.getConfiguration());
    }

    @Test
    void nestedClasses_CanBeUsedIndependently() {
        // Test that all nested classes can be instantiated independently
        assertDoesNotThrow(() -> new SeatMapData.SeatMapDeck());
        assertDoesNotThrow(() -> new SeatMapData.AircraftInfo());
        assertDoesNotThrow(() -> new SeatMapData.FlightInfo());
        assertDoesNotThrow(() -> new SeatMapData.FlightInfo.DepartureInfo());
        assertDoesNotThrow(() -> new SeatMapData.FlightInfo.ArrivalInfo());
        assertDoesNotThrow(() -> new SeatMapData.Seat());
        assertDoesNotThrow(() -> new SeatMapData.LayoutInfo());
    }

    @Test
    void complexObjectConstruction_WorksCorrectly() {
        // Test building a complex object with all nested components
        SeatMapData.FlightInfo.DepartureInfo departure = new SeatMapData.FlightInfo.DepartureInfo();
        departure.setIataCode("LAX");
        departure.setTerminal("4");

        SeatMapData.FlightInfo.ArrivalInfo arrival = new SeatMapData.FlightInfo.ArrivalInfo();
        arrival.setIataCode("SFO");
        arrival.setTerminal("2");

        SeatMapData.FlightInfo flight = new SeatMapData.FlightInfo();
        flight.setNumber("AA123");
        flight.setCarrierCode("AA");
        flight.setDeparture(departure);
        flight.setArrival(arrival);

        SeatMapData.AircraftInfo aircraft = new SeatMapData.AircraftInfo("320", "Airbus A320");
        SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo(30, 6, "3-3");

        SeatMapData.Seat seat = new SeatMapData.Seat();
        seat.setNumber("1A");
        seat.setCabin("FIRST");

        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setFlight(flight);
        seatMapData.setAircraft(aircraft);
        seatMapData.setLayout(layout);
        seatMapData.setSeats(Arrays.asList(seat));
        seatMapData.setSource("AMADEUS");

        // Assert the complex object is constructed correctly
        assertNotNull(seatMapData.getFlight());
        assertEquals("AA123", seatMapData.getFlight().getNumber());
        assertEquals("LAX", seatMapData.getFlight().getDeparture().getIataCode());
        assertEquals("SFO", seatMapData.getFlight().getArrival().getIataCode());
        assertEquals("320", seatMapData.getAircraft().getCode());
        assertEquals(30, seatMapData.getLayout().getTotalRows());
        assertEquals(1, seatMapData.getSeats().size());
        assertEquals("1A", seatMapData.getSeats().get(0).getNumber());
        assertEquals("AMADEUS", seatMapData.getSource());
    }

    @Test
    void allClasses_HandleNullFields() {
        // Test that all classes can handle null values appropriately
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setDecks(null);
        seatMapData.setAircraft(null);
        seatMapData.setFlight(null);
        seatMapData.setSeats(null);
        seatMapData.setLayout(null);
        seatMapData.setSource(null);

        assertNull(seatMapData.getDecks());
        assertNull(seatMapData.getAircraft());
        assertNull(seatMapData.getFlight());
        assertNull(seatMapData.getSeats());
        assertNull(seatMapData.getLayout());
        assertNull(seatMapData.getSource());
    }
}