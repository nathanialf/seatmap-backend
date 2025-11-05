package com.seatmap.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class SeatMapData {
    private List<SeatMapDeck> decks;
    private AircraftInfo aircraft;
    private FlightInfo flight;
    private List<Seat> seats;
    private LayoutInfo layout;
    private String source; // AMADEUS/SABRE
    
    // Default constructor
    public SeatMapData() {}
    
    // Constructor
    public SeatMapData(List<SeatMapDeck> decks, AircraftInfo aircraft, FlightInfo flight, 
                      List<Seat> seats, LayoutInfo layout, String source) {
        this.decks = decks;
        this.aircraft = aircraft;
        this.flight = flight;
        this.seats = seats;
        this.layout = layout;
        this.source = source;
    }
    
    // Constructor from JsonNode (for compatibility with existing seatmap responses)
    public static SeatMapData fromJsonNode(JsonNode seatMapJson, String source) {
        // This will be implemented to convert existing seatmap JSON responses
        // to the new SeatMapData model
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource(source);
        
        // The actual conversion logic will depend on the current seatmap response format
        // For now, we'll store the raw data and implement conversion later
        
        return seatMapData;
    }
    
    // Getters and setters
    public List<SeatMapDeck> getDecks() { return decks; }
    public void setDecks(List<SeatMapDeck> decks) { this.decks = decks; }
    
    public AircraftInfo getAircraft() { return aircraft; }
    public void setAircraft(AircraftInfo aircraft) { this.aircraft = aircraft; }
    
    public FlightInfo getFlight() { return flight; }
    public void setFlight(FlightInfo flight) { this.flight = flight; }
    
    public List<Seat> getSeats() { return seats; }
    public void setSeats(List<Seat> seats) { this.seats = seats; }
    
    public LayoutInfo getLayout() { return layout; }
    public void setLayout(LayoutInfo layout) { this.layout = layout; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    // Inner classes for seatmap structure
    public static class SeatMapDeck {
        private String deckType;
        private JsonNode deckConfiguration;
        private List<JsonNode> facilities;
        private List<Seat> seats;
        
        // Default constructor
        public SeatMapDeck() {}
        
        // Getters and setters
        public String getDeckType() { return deckType; }
        public void setDeckType(String deckType) { this.deckType = deckType; }
        
        public JsonNode getDeckConfiguration() { return deckConfiguration; }
        public void setDeckConfiguration(JsonNode deckConfiguration) { this.deckConfiguration = deckConfiguration; }
        
        public List<JsonNode> getFacilities() { return facilities; }
        public void setFacilities(List<JsonNode> facilities) { this.facilities = facilities; }
        
        public List<Seat> getSeats() { return seats; }
        public void setSeats(List<Seat> seats) { this.seats = seats; }
    }
    
    public static class AircraftInfo {
        private String code;
        private String name;
        
        // Default constructor
        public AircraftInfo() {}
        
        // Constructor
        public AircraftInfo(String code, String name) {
            this.code = code;
            this.name = name;
        }
        
        // Getters and setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class FlightInfo {
        private String number;
        private String carrierCode;
        private DepartureInfo departure;
        private ArrivalInfo arrival;
        private JsonNode operating;
        
        // Default constructor
        public FlightInfo() {}
        
        // Getters and setters
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        
        public String getCarrierCode() { return carrierCode; }
        public void setCarrierCode(String carrierCode) { this.carrierCode = carrierCode; }
        
        public DepartureInfo getDeparture() { return departure; }
        public void setDeparture(DepartureInfo departure) { this.departure = departure; }
        
        public ArrivalInfo getArrival() { return arrival; }
        public void setArrival(ArrivalInfo arrival) { this.arrival = arrival; }
        
        public JsonNode getOperating() { return operating; }
        public void setOperating(JsonNode operating) { this.operating = operating; }
        
        public static class DepartureInfo {
            private String iataCode;
            private String terminal;
            private String at;
            
            // Default constructor
            public DepartureInfo() {}
            
            // Getters and setters
            public String getIataCode() { return iataCode; }
            public void setIataCode(String iataCode) { this.iataCode = iataCode; }
            
            public String getTerminal() { return terminal; }
            public void setTerminal(String terminal) { this.terminal = terminal; }
            
            public String getAt() { return at; }
            public void setAt(String at) { this.at = at; }
        }
        
        public static class ArrivalInfo {
            private String iataCode;
            private String terminal;
            private String at;
            
            // Default constructor
            public ArrivalInfo() {}
            
            // Getters and setters
            public String getIataCode() { return iataCode; }
            public void setIataCode(String iataCode) { this.iataCode = iataCode; }
            
            public String getTerminal() { return terminal; }
            public void setTerminal(String terminal) { this.terminal = terminal; }
            
            public String getAt() { return at; }
            public void setAt(String at) { this.at = at; }
        }
    }
    
    public static class Seat {
        private String number;
        private String cabin;
        private List<String> characteristicsCodes;
        private JsonNode coordinates;
        private List<JsonNode> travelerPricing;
        
        // Default constructor
        public Seat() {}
        
        // Getters and setters
        public String getNumber() { return number; }
        public void setNumber(String number) { this.number = number; }
        
        public String getCabin() { return cabin; }
        public void setCabin(String cabin) { this.cabin = cabin; }
        
        public List<String> getCharacteristicsCodes() { return characteristicsCodes; }
        public void setCharacteristicsCodes(List<String> characteristicsCodes) { 
            this.characteristicsCodes = characteristicsCodes; 
        }
        
        public JsonNode getCoordinates() { return coordinates; }
        public void setCoordinates(JsonNode coordinates) { this.coordinates = coordinates; }
        
        public List<JsonNode> getTravelerPricing() { return travelerPricing; }
        public void setTravelerPricing(List<JsonNode> travelerPricing) { this.travelerPricing = travelerPricing; }
    }
    
    public static class LayoutInfo {
        private int totalRows;
        private int totalColumns;
        private String configuration;
        
        // Default constructor
        public LayoutInfo() {}
        
        // Constructor
        public LayoutInfo(int totalRows, int totalColumns, String configuration) {
            this.totalRows = totalRows;
            this.totalColumns = totalColumns;
            this.configuration = configuration;
        }
        
        // Getters and setters
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        
        public int getTotalColumns() { return totalColumns; }
        public void setTotalColumns(int totalColumns) { this.totalColumns = totalColumns; }
        
        public String getConfiguration() { return configuration; }
        public void setConfiguration(String configuration) { this.configuration = configuration; }
    }
}