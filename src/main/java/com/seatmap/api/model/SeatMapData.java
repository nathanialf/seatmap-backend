package com.seatmap.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
        private List<String> characteristicsCodes; // Raw codes for backward compatibility
        private List<SeatCharacteristic> characteristics; // Expanded, normalized characteristics
        private JsonNode coordinates;
        private String availabilityStatus; // AVAILABLE, OCCUPIED, BLOCKED, etc.
        private SeatPricing pricing; // Simplified single traveler pricing
        
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
        
        public List<SeatCharacteristic> getCharacteristics() { return characteristics; }
        public void setCharacteristics(List<SeatCharacteristic> characteristics) { this.characteristics = characteristics; }
        
        public JsonNode getCoordinates() { return coordinates; }
        public void setCoordinates(JsonNode coordinates) { this.coordinates = coordinates; }
        
        public String getAvailabilityStatus() { return availabilityStatus; }
        public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
        
        public SeatPricing getPricing() { return pricing; }
        public void setPricing(SeatPricing pricing) { this.pricing = pricing; }
        
        // Legacy support for existing travelerPricing field
        @Deprecated
        public List<JsonNode> getTravelerPricing() {
            if (pricing != null) {
                List<JsonNode> legacy = new ArrayList<>();
                ObjectMapper mapper = new ObjectMapper();
                try {
                    legacy.add(mapper.valueToTree(pricing));
                } catch (Exception e) {
                    // Fallback to empty list
                }
                return legacy;
            }
            return new ArrayList<>();
        }
        
        @Deprecated
        public void setTravelerPricing(List<JsonNode> travelerPricing) {
            if (travelerPricing != null && !travelerPricing.isEmpty()) {
                JsonNode firstPricing = travelerPricing.get(0);
                ObjectMapper mapper = new ObjectMapper();
                try {
                    // Extract availability status
                    this.availabilityStatus = firstPricing.path("seatAvailabilityStatus").asText(null);
                    
                    // Extract pricing information
                    if (firstPricing.has("price")) {
                        JsonNode priceNode = firstPricing.get("price");
                        SeatPricing seatPricing = new SeatPricing();
                        seatPricing.setCurrency(priceNode.path("currency").asText(null));
                        seatPricing.setTotal(priceNode.path("total").asText(null));
                        seatPricing.setBase(priceNode.path("base").asText(null));
                        this.pricing = seatPricing;
                    }
                } catch (Exception e) {
                    // Ignore conversion errors for legacy compatibility
                }
            }
        }
    }
    
    public static class SeatPricing {
        private String currency;
        private String total;
        private String base;
        private List<Tax> taxes;
        
        public SeatPricing() {}
        
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        
        public String getTotal() { return total; }
        public void setTotal(String total) { this.total = total; }
        
        public String getBase() { return base; }
        public void setBase(String base) { this.base = base; }
        
        public List<Tax> getTaxes() { return taxes; }
        public void setTaxes(List<Tax> taxes) { this.taxes = taxes; }
        
        public static class Tax {
            private String amount;
            private String code;
            
            public Tax() {}
            
            public String getAmount() { return amount; }
            public void setAmount(String amount) { this.amount = amount; }
            
            public String getCode() { return code; }
            public void setCode(String code) { this.code = code; }
        }
    }
    
    public static class SeatCharacteristic {
        private String code; // Original code (e.g., "W", "A", "CH")
        private String category; // POSITION, RESTRICTION, AMENITY, etc.
        private String description; // Human-readable description
        private boolean isRestriction; // Whether this limits seat selection
        private boolean isPremium; // Whether this is a premium feature
        
        public SeatCharacteristic() {}
        
        public SeatCharacteristic(String code, String category, String description) {
            this.code = code;
            this.category = category;
            this.description = description;
        }
        
        public SeatCharacteristic(String code, String category, String description, boolean isRestriction, boolean isPremium) {
            this.code = code;
            this.category = category;
            this.description = description;
            this.isRestriction = isRestriction;
            this.isPremium = isPremium;
        }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isRestriction() { return isRestriction; }
        public void setRestriction(boolean isRestriction) { this.isRestriction = isRestriction; }
        
        public boolean isPremium() { return isPremium; }
        public void setPremium(boolean isPremium) { this.isPremium = isPremium; }
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