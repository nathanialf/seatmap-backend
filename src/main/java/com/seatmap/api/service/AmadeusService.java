package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.exception.SeatmapApiException;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.SeatMapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class AmadeusService {
    private static final Logger logger = LoggerFactory.getLogger(AmadeusService.class);
    
    private final String apiKey;
    private final String apiSecret;
    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    private String accessToken;
    private long tokenExpiresAt;
    
    public AmadeusService() {
        this.apiKey = System.getenv("AMADEUS_API_KEY");
        this.apiSecret = System.getenv("AMADEUS_API_SECRET");
        this.endpoint = System.getenv("AMADEUS_ENDPOINT");
        
        if (apiKey == null || apiSecret == null || endpoint == null) {
            throw new IllegalStateException("Amadeus API credentials not configured");
        }
        
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    public JsonNode getSeatMap(String airlineCode, String flightNumber, String departureDate, String origin, String destination) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // Step 1: Search for flight offers using Flight Offers Search API (any travel class)
            JsonNode flightOffers = searchFlightOffersInternal(origin, destination, departureDate, null, airlineCode, flightNumber, 10);
            
            if (flightOffers == null || !flightOffers.has("data") || flightOffers.get("data").size() == 0) {
                throw new SeatmapApiException("No flight offers found for the specified criteria");
            }
            
            // Step 2: Get seat map using the first flight offer
            JsonNode firstOffer = flightOffers.get("data").get(0);
            return getSeatMapFromOfferInternal(firstOffer);
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    public JsonNode searchFlightOffers(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults) throws SeatmapApiException {
        try {
            ensureValidToken();
            return searchFlightOffersInternal(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults);
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    /**
     * Search flight offers with integrated seatmap data
     */
    public List<FlightSearchResult> searchFlightsWithSeatmaps(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // 1. Get flight offers
            JsonNode flightOffers = searchFlightOffersInternal(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults);
            
            if (flightOffers == null || !flightOffers.has("data")) {
                return new ArrayList<>();
            }
            
            // 2. For each offer, fetch seatmap concurrently and filter out failures
            List<FlightSearchResult> results = extractFlightOffers(flightOffers).parallelStream()
                .map(this::buildFlightSearchResult)
                .filter(Objects::nonNull)  // Only include flights with successful seatmaps
                .collect(toList());
            
            logger.info("Successfully processed {} flight offers with seatmaps from Amadeus", results.size());
            return results;
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API for flight search with seatmaps", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    /**
     * Build FlightSearchResult with integrated seatmap data
     * Returns null if seatmap fetch fails (flight will be filtered out)
     */
    private FlightSearchResult buildFlightSearchResult(JsonNode offer) {
        try {
            // Get seatmap data for this offer
            JsonNode seatMapResponse = getSeatMapFromOfferInternal(offer);
            SeatMapData seatMapData = convertToSeatMapData(seatMapResponse);
            
            // Add dataSource field to identify this as AMADEUS data
            ObjectNode offerWithDataSource = offer.deepCopy();
            offerWithDataSource.put("dataSource", "AMADEUS");
            
            return new FlightSearchResult(offerWithDataSource, seatMapData, true, null);
            
        } catch (Exception e) {
            logger.warn("Omitting flight {} - seatmap unavailable: {}", offer.path("id").asText(), e.getMessage());
            return null; // Filter out flights without seatmaps
        }
    }
    
    /**
     * Convert Amadeus seatmap response to SeatMapData model
     */
    public SeatMapData convertToSeatMapData(JsonNode seatMapResponse) {
        logger.info("Converting Amadeus seat map response to SeatMapData");
        SeatMapData seatMapData = new SeatMapData();
        seatMapData.setSource("AMADEUS");
        
        if (seatMapResponse == null || !seatMapResponse.has("data")) {
            logger.info("Seat map response is null or missing data field, returning empty SeatMapData");
            return seatMapData;
        }
        
        try {
            JsonNode data = seatMapResponse.get("data");
            
            // Handle both single segment and multi-segment responses
            if (!data.isArray()) {
                // Single segment response - convert to array for uniform processing
                data = objectMapper.createArrayNode().add(data);
            }
            
            if (data.size() == 0) {
                logger.info("No seat map segments found in response");
                return seatMapData;
            }
            
            logger.info("Processing {} seat map segments", data.size());
            
            // Process ALL segments to collect complete seat map data
            List<SeatMapData.SeatMapDeck> allDecks = new ArrayList<>();
            List<SeatMapData.Seat> allSeats = new ArrayList<>();
            SeatMapData.FlightInfo flightInfo = null;
            SeatMapData.AircraftInfo aircraftInfo = null;
            int totalRows = 0;
            int totalColumns = 0;
            
            // Iterate through ALL segments (handles both single and multi-segment)
            for (int segmentIndex = 0; segmentIndex < data.size(); segmentIndex++) {
                JsonNode segment = data.get(segmentIndex);
                logger.info("Processing segment {}: flight {}", segmentIndex + 1, 
                          segment.path("number").asText("unknown"));
                
                // Extract flight info from first segment with complete data
                if (flightInfo == null) {
                    String flightNumber = segment.path("number").asText();
                    String carrierCode = segment.path("carrierCode").asText();
                    
                    if (!flightNumber.isEmpty() && !carrierCode.isEmpty()) {
                        flightInfo = new SeatMapData.FlightInfo();
                        flightInfo.setNumber(flightNumber);
                        flightInfo.setCarrierCode(carrierCode);
                        
                        // Extract departure info
                        if (segment.has("departure")) {
                            JsonNode departure = segment.get("departure");
                            SeatMapData.FlightInfo.DepartureInfo departureInfo = new SeatMapData.FlightInfo.DepartureInfo();
                            departureInfo.setIataCode(departure.path("iataCode").asText());
                            departureInfo.setTerminal(departure.path("terminal").asText());
                            departureInfo.setAt(departure.path("at").asText());
                            flightInfo.setDeparture(departureInfo);
                        }
                        
                        // Extract arrival info
                        if (segment.has("arrival")) {
                            JsonNode arrival = segment.get("arrival");
                            SeatMapData.FlightInfo.ArrivalInfo arrivalInfo = new SeatMapData.FlightInfo.ArrivalInfo();
                            arrivalInfo.setIataCode(arrival.path("iataCode").asText());
                            arrivalInfo.setTerminal(arrival.path("terminal").asText());
                            arrivalInfo.setAt(arrival.path("at").asText());
                            flightInfo.setArrival(arrivalInfo);
                        }
                        
                        // Extract operating info if present
                        if (segment.has("operating")) {
                            flightInfo.setOperating(segment.get("operating"));
                        }
                        
                        logger.info("Flight info extracted from segment {}: {} {}", 
                                  segmentIndex + 1, carrierCode, flightNumber);
                    }
                }
                
                // Extract aircraft info from first segment with aircraft data
                if (aircraftInfo == null && segment.has("aircraft")) {
                    JsonNode aircraft = segment.get("aircraft");
                    aircraftInfo = new SeatMapData.AircraftInfo();
                    aircraftInfo.setCode(aircraft.path("code").asText());
                    aircraftInfo.setName(""); // Amadeus doesn't provide aircraft name
                    logger.info("Aircraft info extracted from segment {}: {}", 
                              segmentIndex + 1, aircraftInfo.getCode());
                }
                
                // Extract deck information and seats from THIS segment
                if (segment.has("decks")) {
                    int segmentSeatCount = 0;
                    for (JsonNode deckNode : segment.get("decks")) {
                        SeatMapData.SeatMapDeck deck = new SeatMapData.SeatMapDeck();
                        deck.setDeckType(deckNode.path("deckType").asText());
                        
                        // Store deck configuration
                        if (deckNode.has("deckConfiguration")) {
                            deck.setDeckConfiguration(deckNode.get("deckConfiguration"));
                            
                            // Extract layout dimensions from deck configuration
                            JsonNode deckConfig = deckNode.get("deckConfiguration");
                            totalColumns = Math.max(totalColumns, deckConfig.path("width").asInt(0));
                            totalRows = Math.max(totalRows, deckConfig.path("length").asInt(0));
                        }
                        
                        // Store facilities
                        if (deckNode.has("facilities")) {
                            List<JsonNode> facilities = new ArrayList<>();
                            for (JsonNode facility : deckNode.get("facilities")) {
                                facilities.add(facility);
                            }
                            deck.setFacilities(facilities);
                        }
                        
                        // Extract seats from this deck
                        List<SeatMapData.Seat> deckSeats = new ArrayList<>();
                        if (deckNode.has("seats")) {
                            for (JsonNode seatNode : deckNode.get("seats")) {
                                SeatMapData.Seat seat = new SeatMapData.Seat();
                                seat.setNumber(seatNode.path("number").asText());
                                seat.setCabin(seatNode.path("cabin").asText());
                                
                                // Extract characteristics codes
                                if (seatNode.has("characteristicsCodes")) {
                                    List<String> characteristicCodes = new ArrayList<>();
                                    for (JsonNode code : seatNode.get("characteristicsCodes")) {
                                        characteristicCodes.add(code.asText());
                                    }
                                    seat.setCharacteristicsCodes(characteristicCodes);
                                    
                                    // Map to normalized characteristics
                                    List<SeatMapData.SeatCharacteristic> characteristics = SeatCharacteristicMapper.mapAmadeusCharacteristics(characteristicCodes);
                                    seat.setCharacteristics(characteristics);
                                }
                                
                                // Extract coordinates
                                if (seatNode.has("coordinates")) {
                                    seat.setCoordinates(seatNode.get("coordinates"));
                                }
                                
                                // Extract availability status and pricing (single traveler)
                                if (seatNode.has("travelerPricing") && seatNode.get("travelerPricing").isArray() && seatNode.get("travelerPricing").size() > 0) {
                                    JsonNode firstTraveler = seatNode.get("travelerPricing").get(0);
                                    
                                    // Extract availability status
                                    if (firstTraveler.has("seatAvailabilityStatus")) {
                                        seat.setAvailabilityStatus(firstTraveler.get("seatAvailabilityStatus").asText());
                                    }
                                    
                                    // Extract pricing information
                                    if (firstTraveler.has("price")) {
                                        JsonNode priceNode = firstTraveler.get("price");
                                        SeatMapData.SeatPricing pricing = new SeatMapData.SeatPricing();
                                        pricing.setCurrency(priceNode.path("currency").asText(null));
                                        pricing.setTotal(priceNode.path("total").asText(null));
                                        pricing.setBase(priceNode.path("base").asText(null));
                                        
                                        // Extract taxes if present
                                        if (priceNode.has("taxes") && priceNode.get("taxes").isArray()) {
                                            List<SeatMapData.SeatPricing.Tax> taxes = new ArrayList<>();
                                            for (JsonNode taxNode : priceNode.get("taxes")) {
                                                SeatMapData.SeatPricing.Tax tax = new SeatMapData.SeatPricing.Tax();
                                                tax.setAmount(taxNode.path("amount").asText(null));
                                                tax.setCode(taxNode.path("code").asText(null));
                                                taxes.add(tax);
                                            }
                                            pricing.setTaxes(taxes);
                                        }
                                        
                                        seat.setPricing(pricing);
                                    }
                                }
                                
                                deckSeats.add(seat);
                                allSeats.add(seat);
                                segmentSeatCount++;
                            }
                        }
                        
                        deck.setSeats(deckSeats);
                        allDecks.add(deck);
                    }
                    logger.info("Segment {} contributed {} seats", segmentIndex + 1, segmentSeatCount);
                }
            }
            
            // Set the aggregated data from ALL segments
            seatMapData.setFlight(flightInfo);
            seatMapData.setAircraft(aircraftInfo);
            seatMapData.setDecks(allDecks);
            seatMapData.setSeats(allSeats);
            
            // Create layout info
            if (totalRows > 0 || totalColumns > 0) {
                SeatMapData.LayoutInfo layout = new SeatMapData.LayoutInfo();
                layout.setTotalRows(totalRows);
                layout.setTotalColumns(totalColumns);
                layout.setConfiguration(String.format("%dx%d", totalRows, totalColumns));
                seatMapData.setLayout(layout);
            }
            
        } catch (Exception e) {
            logger.error("Error converting Amadeus seat map response: {}", e.getMessage(), e);
            logger.error("Response structure: {}", seatMapResponse != null ? seatMapResponse.toPrettyString() : "null");
            // Return basic seat map data with source only if conversion fails
        }
        
        logger.info("Conversion complete. SeatMapData has {} decks, {} seats, flight: {}", 
                    seatMapData.getDecks() != null ? seatMapData.getDecks().size() : 0,
                    seatMapData.getSeats() != null ? seatMapData.getSeats().size() : 0,
                    seatMapData.getFlight() != null ? seatMapData.getFlight().getNumber() : "null");
        
        return seatMapData;
    }
    
    /**
     * Extract flight offers from Amadeus response
     */
    private List<JsonNode> extractFlightOffers(JsonNode flightOffers) {
        List<JsonNode> offers = new ArrayList<>();
        if (flightOffers.has("data") && flightOffers.get("data").isArray()) {
            for (JsonNode offer : flightOffers.get("data")) {
                offers.add(offer);
            }
        }
        return offers;
    }
    
    /**
     * Get seatmap from flight offer (existing method, made public for reuse)
     */
    public JsonNode getSeatMapFromOffer(JsonNode flightOffer) throws SeatmapApiException {
        try {
            ensureValidToken();
            return getSeatMapFromOfferInternal(flightOffer);
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    public JsonNode getSeatMapFromOfferData(String flightOfferData) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // Parse the flight offer data
            JsonNode flightOffer = objectMapper.readTree(flightOfferData);
            return getSeatMapFromOfferInternal(flightOffer);
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    /**
     * Get seat maps for multiple flight offers in a single batch request
     */
    public JsonNode getBatchSeatMapsFromOffers(List<JsonNode> flightOffers) throws SeatmapApiException {
        try {
            ensureValidToken();
            return getBatchSeatMapsFromOffersInternal(flightOffers);
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus batch seat map API", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    /**
     * Enhanced search method that uses batch seat map requests for better performance
     */
    public List<FlightSearchResult> searchFlightsWithBatchSeatmaps(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults) throws SeatmapApiException {
        try {
            ensureValidToken();
            
            // 1. Get flight offers
            JsonNode flightOffers = searchFlightOffersInternal(origin, destination, departureDate, travelClass, airlineCode, flightNumber, maxResults);
            
            if (flightOffers == null || !flightOffers.has("data")) {
                return new ArrayList<>();
            }
            
            // 2. Extract flight offers into a list
            List<JsonNode> offers = extractFlightOffers(flightOffers);
            
            if (offers.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 3. Make individual seat map requests in parallel for each offer
            List<CompletableFuture<FlightSearchResult>> futures = offers.stream()
                .map(offer -> CompletableFuture.supplyAsync(() -> {
                    try {
                        // Get seatmap data for this offer
                        JsonNode seatMapResponse = getSeatMapFromOfferInternal(offer);
                        SeatMapData seatMapData = convertToSeatMapData(seatMapResponse);
                        
                        // Add dataSource field to identify this as AMADEUS data
                        ObjectNode offerWithDataSource = offer.deepCopy();
                        offerWithDataSource.put("dataSource", "AMADEUS");
                        
                        return new FlightSearchResult(offerWithDataSource, seatMapData, true, null);
                        
                    } catch (Exception e) {
                        logger.warn("Omitting flight {} - seatmap unavailable: {}", offer.path("id").asText(), e.getMessage());
                        return null; // Filter out flights without seatmaps
                    }
                }))
                .collect(Collectors.toList());
            
            // Collect all results from parallel execution, filtering out nulls
            List<FlightSearchResult> results = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            logger.info("Successfully processed {} flight offers with individual parallel seatmaps from Amadeus (filtered from {} offers)", 
                results.size(), offers.size());
            return results;
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error calling Amadeus API for batch flight search with seatmaps", e);
            throw new SeatmapApiException("Network error calling Amadeus API", e);
        }
    }
    
    private JsonNode searchFlightOffersInternal(String origin, String destination, String departureDate, String travelClass, String airlineCode, String flightNumber, Integer maxResults) throws SeatmapApiException, IOException, InterruptedException {
        int max = maxResults != null ? maxResults : 10;
        
        // Build base URL - only include travelClass if specified (minimum cabin quality)
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(String.format("https://%s/v2/shopping/flight-offers?originLocationCode=%s&destinationLocationCode=%s&departureDate=%s&adults=1&max=%d",
            endpoint,
            URLEncoder.encode(origin, StandardCharsets.UTF_8),
            URLEncoder.encode(destination, StandardCharsets.UTF_8),
            URLEncoder.encode(departureDate, StandardCharsets.UTF_8),
            max
        ));
        
        // Add travelClass parameter only if specified (searches minimum quality or higher)
        if (travelClass != null && !travelClass.trim().isEmpty()) {
            urlBuilder.append("&travelClass=").append(URLEncoder.encode(travelClass, StandardCharsets.UTF_8));
        }
        
        String url = urlBuilder.toString();
        
        // Add airline code filter if specified
        if (airlineCode != null && !airlineCode.trim().isEmpty()) {
            url += "&includedAirlineCodes=" + URLEncoder.encode(airlineCode, StandardCharsets.UTF_8);
        }
        
        logger.info("Searching flight offers: {}", url);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode result = objectMapper.readTree(response.body());
            logger.info("Found {} flight offers", result.has("data") ? result.get("data").size() : 0);
            return result;
        } else {
            logger.error("Flight offers search error: {} - {}", response.statusCode(), response.body());
            throw new SeatmapApiException("Failed to search flight offers: " + response.statusCode());
        }
    }
    
    private JsonNode getSeatMapFromOfferInternal(JsonNode flightOffer) throws SeatmapApiException, IOException, InterruptedException {
        String url = "https://" + endpoint + "/v1/shopping/seatmaps";
        
        // Enhance flight offer with operating carrier code if missing
        JsonNode enhancedOffer = enhanceFlightOfferWithOperatingCarrier(flightOffer);
        
        // Create request body with enhanced flight offer
        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode()
                .set("data", objectMapper.createArrayNode().add(enhancedOffer))
        );
        
        logger.info("Getting seat map for flight offer: {}", enhancedOffer.get("id"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode result = objectMapper.readTree(response.body());
            logger.info("Successfully retrieved seat map data");
            return result;
        } else {
            logger.error("Seat map API error: {} - {}", response.statusCode(), response.body());
            throw new SeatmapApiException("Failed to retrieve seat map: " + response.statusCode());
        }
    }
    
    private JsonNode getBatchSeatMapsFromOffersInternal(List<JsonNode> flightOffers) throws SeatmapApiException, IOException, InterruptedException {
        String url = "https://" + endpoint + "/v1/shopping/seatmaps";
        
        // Create request body with enhanced flight offers
        ArrayNode dataArray = objectMapper.createArrayNode();
        for (JsonNode offer : flightOffers) {
            JsonNode enhancedOffer = enhanceFlightOfferWithOperatingCarrier(offer);
            dataArray.add(enhancedOffer);
        }
        
        String requestBody = objectMapper.writeValueAsString(
            objectMapper.createObjectNode().set("data", dataArray)
        );
        
        logger.info("Getting batch seat maps for {} flight offers", flightOffers.size());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode result = objectMapper.readTree(response.body());
            logger.info("Successfully retrieved batch seat map data");
            return result;
        } else {
            logger.error("Batch seat map API error: {} - {}", response.statusCode(), response.body());
            throw new SeatmapApiException("Failed to retrieve batch seat maps: " + response.statusCode());
        }
    }
    
    private List<FlightSearchResult> buildFlightSearchResultsFromBatch(List<JsonNode> offers, JsonNode batchSeatMapResponse) {
        List<FlightSearchResult> results = new ArrayList<>();
        
        // Create a map of flight offer ID to seat map data for quick lookup
        Map<String, JsonNode> seatMapsByOfferId = new HashMap<>();
        
        if (batchSeatMapResponse != null && batchSeatMapResponse.has("data") && batchSeatMapResponse.get("data").isArray()) {
            for (JsonNode seatMapData : batchSeatMapResponse.get("data")) {
                // Extract the associated flight offer ID from the seat map response
                String offerId = extractOfferIdFromSeatMap(seatMapData);
                if (offerId != null) {
                    seatMapsByOfferId.put(offerId, seatMapData);
                }
            }
        }
        
        // Match offers with their seat maps and filter out those without seat maps
        for (JsonNode offer : offers) {
            String offerId = offer.path("id").asText();
            JsonNode seatMapData = seatMapsByOfferId.get(offerId);
            
            if (seatMapData != null) {
                try {
                    // Convert seat map data to our model
                    SeatMapData convertedSeatMap = convertSeatMapDataFromBatchResponse(seatMapData);
                    
                    // Add dataSource field to identify this as AMADEUS data
                    ObjectNode offerWithDataSource = offer.deepCopy();
                    offerWithDataSource.put("dataSource", "AMADEUS");
                    
                    results.add(new FlightSearchResult(offerWithDataSource, convertedSeatMap, true, null));
                    
                } catch (Exception e) {
                    logger.warn("Error converting seat map for offer {}: {}", offerId, e.getMessage());
                    // Skip this flight offer if seat map conversion fails
                }
            } else {
                logger.debug("No seat map available for flight offer: {}", offerId);
                // Filter out flights without seat maps
            }
        }
        
        return results;
    }
    
    private String extractOfferIdFromSeatMap(JsonNode seatMapData) {
        // The seat map response should include the original flight offer ID
        // This might be in different locations depending on Amadeus response structure
        if (seatMapData.has("flightOfferId")) {
            return seatMapData.get("flightOfferId").asText();
        }
        
        // Alternative: check if there's an associated offer in the response
        if (seatMapData.has("associatedOffer") && seatMapData.get("associatedOffer").has("id")) {
            return seatMapData.get("associatedOffer").get("id").asText();
        }
        
        // If no direct ID mapping, we'll need to match by flight details
        // This is less reliable but may be necessary depending on Amadeus response format
        return null;
    }
    
    private SeatMapData convertSeatMapDataFromBatchResponse(JsonNode seatMapData) {
        // Wrap the individual seat map in the expected structure for conversion
        ObjectNode wrappedResponse = objectMapper.createObjectNode();
        wrappedResponse.set("data", objectMapper.createArrayNode().add(seatMapData));
        
        return convertToSeatMapData(wrappedResponse);
    }
    
    /**
     * Enhance flight offer by populating missing operating carrier codes with marketing carrier codes
     * Returns the same flight offer if no enhancement is needed
     */
    private JsonNode enhanceFlightOfferWithOperatingCarrier(JsonNode flightOffer) {
        try {
            boolean needsEnhancement = false;
            ObjectNode enhancedOffer = flightOffer.deepCopy();
            
            // Check if itineraries exist
            if (!enhancedOffer.has("itineraries") || !enhancedOffer.get("itineraries").isArray()) {
                return flightOffer; // Return original if no itineraries
            }
            
            // Process each itinerary
            ArrayNode itineraries = (ArrayNode) enhancedOffer.get("itineraries");
            for (JsonNode itinerary : itineraries) {
                if (!itinerary.has("segments") || !itinerary.get("segments").isArray()) {
                    continue;
                }
                
                // Process each segment
                ArrayNode segments = (ArrayNode) itinerary.get("segments");
                for (int i = 0; i < segments.size(); i++) {
                    ObjectNode segment = (ObjectNode) segments.get(i);
                    
                    // Check if operating carrier code is missing or empty
                    boolean operatingMissing = !segment.has("operating") || 
                                             !segment.get("operating").has("carrierCode") ||
                                             segment.get("operating").get("carrierCode").asText().trim().isEmpty();
                    
                    if (operatingMissing && segment.has("carrierCode")) {
                        String marketingCarrierCode = segment.get("carrierCode").asText();
                        
                        if (!marketingCarrierCode.trim().isEmpty()) {
                            // Create or update operating object
                            ObjectNode operating;
                            if (segment.has("operating")) {
                                operating = (ObjectNode) segment.get("operating");
                            } else {
                                operating = objectMapper.createObjectNode();
                                segment.set("operating", operating);
                            }
                            
                            operating.put("carrierCode", marketingCarrierCode);
                            needsEnhancement = true;
                            
                            logger.debug("Enhanced segment {} with operating carrier code: {}", i, marketingCarrierCode);
                        }
                    }
                }
            }
            
            if (needsEnhancement) {
                logger.info("Enhanced flight offer {} with missing operating carrier codes", enhancedOffer.path("id").asText());
                return enhancedOffer;
            } else {
                return flightOffer; // Return original if no changes needed
            }
            
        } catch (Exception e) {
            logger.warn("Error enhancing flight offer with operating carrier codes: {}", e.getMessage());
            return flightOffer; // Return original on error
        }
    }
    
    private void ensureValidToken() throws SeatmapApiException {
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiresAt) {
            refreshAccessToken();
        }
    }
    
    private void refreshAccessToken() throws SeatmapApiException {
        try {
            String credentials = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
            String requestBody = "grant_type=client_credentials";
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://" + endpoint + "/v1/security/oauth2/token"))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode tokenResponse = objectMapper.readTree(response.body());
                this.accessToken = tokenResponse.get("access_token").asText();
                int expiresIn = tokenResponse.get("expires_in").asInt();
                this.tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L) - 60000; // Refresh 1 minute early
                
                logger.info("Successfully refreshed Amadeus access token");
            } else {
                logger.error("Failed to get Amadeus access token: {} - {}", response.statusCode(), response.body());
                throw new SeatmapApiException("Failed to authenticate with Amadeus API");
            }
            
        } catch (IOException | InterruptedException e) {
            logger.error("Error refreshing Amadeus access token", e);
            throw new SeatmapApiException("Network error during authentication", e);
        }
    }
}