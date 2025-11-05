package com.seatmap.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class FlightSearchResult {
    private String id;
    
    @NotNull(message = "dataSource is required for provider routing")
    @NotBlank(message = "dataSource cannot be blank")
    private String dataSource;
    private String type;
    private String source;
    private boolean instantTicketingRequired;
    private boolean nonHomogeneous;
    private boolean oneWay;
    private String lastTicketingDate;
    private String lastTicketingDateTime;
    private int numberOfBookableSeats;
    private List<JsonNode> itineraries;
    private JsonNode price;
    private List<JsonNode> travelerPricings;
    private List<String> validatingAirlineCodes;
    
    // Integrated seatmap data
    private SeatMapData seatMap;
    private boolean seatMapAvailable;
    private String seatMapError;
    
    // Default constructor
    public FlightSearchResult() {}
    
    // Constructor from existing flight offer + seatmap
    public FlightSearchResult(JsonNode flightOffer, SeatMapData seatMap, boolean seatMapAvailable, String seatMapError) {
        // Extract flight offer fields
        this.id = flightOffer.path("id").asText();
        
        // dataSource is required for proper routing - fail fast if missing
        if (!flightOffer.has("dataSource") || flightOffer.get("dataSource").asText().trim().isEmpty()) {
            throw new IllegalArgumentException("Flight offer must contain a valid dataSource field for provider routing");
        }
        this.dataSource = flightOffer.get("dataSource").asText();
        this.type = flightOffer.path("type").asText();
        this.source = flightOffer.path("source").asText();
        this.instantTicketingRequired = flightOffer.path("instantTicketingRequired").asBoolean();
        this.nonHomogeneous = flightOffer.path("nonHomogeneous").asBoolean();
        this.oneWay = flightOffer.path("oneWay").asBoolean();
        this.lastTicketingDate = flightOffer.path("lastTicketingDate").asText();
        this.lastTicketingDateTime = flightOffer.path("lastTicketingDateTime").asText();
        this.numberOfBookableSeats = flightOffer.path("numberOfBookableSeats").asInt();
        
        // Convert arrays to lists
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (flightOffer.has("itineraries") && flightOffer.get("itineraries").isArray()) {
                this.itineraries = mapper.convertValue(flightOffer.get("itineraries"), 
                    mapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
            }
            
            if (flightOffer.has("travelerPricings") && flightOffer.get("travelerPricings").isArray()) {
                this.travelerPricings = mapper.convertValue(flightOffer.get("travelerPricings"), 
                    mapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class));
            }
            
            if (flightOffer.has("validatingAirlineCodes") && flightOffer.get("validatingAirlineCodes").isArray()) {
                this.validatingAirlineCodes = mapper.convertValue(flightOffer.get("validatingAirlineCodes"), 
                    mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            }
        } catch (Exception e) {
            // Handle conversion errors gracefully
        }
        
        this.price = flightOffer.get("price");
        
        // Set seatmap data
        this.seatMap = seatMap;
        this.seatMapAvailable = seatMapAvailable;
        this.seatMapError = seatMapError;
    }
    
    // Convert back to JsonNode (for compatibility with existing code)
    public JsonNode toJsonNode() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        
        node.put("id", id);
        node.put("dataSource", dataSource);
        node.put("type", type);
        node.put("source", source);
        node.put("instantTicketingRequired", instantTicketingRequired);
        node.put("nonHomogeneous", nonHomogeneous);
        node.put("oneWay", oneWay);
        node.put("lastTicketingDate", lastTicketingDate);
        node.put("lastTicketingDateTime", lastTicketingDateTime);
        node.put("numberOfBookableSeats", numberOfBookableSeats);
        
        if (itineraries != null) {
            node.set("itineraries", mapper.valueToTree(itineraries));
        }
        if (price != null) {
            node.set("price", price);
        }
        if (travelerPricings != null) {
            node.set("travelerPricings", mapper.valueToTree(travelerPricings));
        }
        if (validatingAirlineCodes != null) {
            node.set("validatingAirlineCodes", mapper.valueToTree(validatingAirlineCodes));
        }
        
        // Add seatmap data
        if (seatMap != null) {
            node.set("seatMap", mapper.valueToTree(seatMap));
        }
        node.put("seatMapAvailable", seatMapAvailable);
        if (seatMapError != null) {
            node.put("seatMapError", seatMapError);
        }
        
        return node;
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    
    public boolean isInstantTicketingRequired() { return instantTicketingRequired; }
    public void setInstantTicketingRequired(boolean instantTicketingRequired) { 
        this.instantTicketingRequired = instantTicketingRequired; 
    }
    
    public boolean isNonHomogeneous() { return nonHomogeneous; }
    public void setNonHomogeneous(boolean nonHomogeneous) { this.nonHomogeneous = nonHomogeneous; }
    
    public boolean isOneWay() { return oneWay; }
    public void setOneWay(boolean oneWay) { this.oneWay = oneWay; }
    
    public String getLastTicketingDate() { return lastTicketingDate; }
    public void setLastTicketingDate(String lastTicketingDate) { this.lastTicketingDate = lastTicketingDate; }
    
    public String getLastTicketingDateTime() { return lastTicketingDateTime; }
    public void setLastTicketingDateTime(String lastTicketingDateTime) { 
        this.lastTicketingDateTime = lastTicketingDateTime; 
    }
    
    public int getNumberOfBookableSeats() { return numberOfBookableSeats; }
    public void setNumberOfBookableSeats(int numberOfBookableSeats) { 
        this.numberOfBookableSeats = numberOfBookableSeats; 
    }
    
    public List<JsonNode> getItineraries() { return itineraries; }
    public void setItineraries(List<JsonNode> itineraries) { this.itineraries = itineraries; }
    
    public JsonNode getPrice() { return price; }
    public void setPrice(JsonNode price) { this.price = price; }
    
    public List<JsonNode> getTravelerPricings() { return travelerPricings; }
    public void setTravelerPricings(List<JsonNode> travelerPricings) { this.travelerPricings = travelerPricings; }
    
    public List<String> getValidatingAirlineCodes() { return validatingAirlineCodes; }
    public void setValidatingAirlineCodes(List<String> validatingAirlineCodes) { 
        this.validatingAirlineCodes = validatingAirlineCodes; 
    }
    
    public SeatMapData getSeatMap() { return seatMap; }
    public void setSeatMap(SeatMapData seatMap) { this.seatMap = seatMap; }
    
    public boolean isSeatMapAvailable() { return seatMapAvailable; }
    public void setSeatMapAvailable(boolean seatMapAvailable) { this.seatMapAvailable = seatMapAvailable; }
    
    public String getSeatMapError() { return seatMapError; }
    public void setSeatMapError(String seatMapError) { this.seatMapError = seatMapError; }
}