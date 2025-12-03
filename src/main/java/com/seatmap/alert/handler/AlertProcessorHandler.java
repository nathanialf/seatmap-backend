package com.seatmap.alert.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.alert.service.AlertEvaluationService;
import com.seatmap.api.model.FlightSearchRequest;
import com.seatmap.api.model.FlightSearchResponse;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.api.model.SeatMapData;
import com.seatmap.api.service.AmadeusService;
import com.seatmap.api.service.FlightSearchService;
import com.seatmap.api.service.SabreService;
import com.seatmap.auth.repository.BookmarkRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.model.User;
import com.seatmap.email.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AlertProcessorHandler implements RequestHandler<ScheduledEvent, String> {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertProcessorHandler.class);
    
    private final ObjectMapper objectMapper;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final FlightSearchService flightSearchService;
    private final AlertEvaluationService alertEvaluationService;
    private final EmailService emailService;
    private final AmadeusService amadeusService;
    
    public AlertProcessorHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // Initialize AWS services
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
        
        // Get environment
        String environment = System.getenv("ENVIRONMENT");
        if (environment == null) environment = "dev";
        
        // Initialize repositories
        String bookmarksTable = "seatmap-bookmarks-" + environment;
        String usersTable = "seatmap-users-" + environment;
        
        this.bookmarkRepository = new BookmarkRepository(dynamoDbClient, bookmarksTable);
        this.userRepository = new UserRepository(dynamoDbClient, usersTable);
        
        // Initialize flight search services
        this.amadeusService = new AmadeusService();
        SabreService sabreService = new SabreService();
        this.flightSearchService = new FlightSearchService(amadeusService, sabreService);
        
        // Initialize alert evaluation and email services
        this.alertEvaluationService = new AlertEvaluationService();
        this.emailService = new EmailService();
    }
    
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        logger.info("Starting alert processor batch job");
        
        try {
            // Find all bookmarks with active alerts for upcoming flights
            List<Bookmark> activeAlerts = bookmarkRepository.findBookmarksWithActiveAlertsForUpcomingFlights();
            logger.info("Found {} bookmarks with active alerts", activeAlerts.size());
            
            if (activeAlerts.isEmpty()) {
                return "No active alerts to process";
            }
            
            // Group alerts by search criteria for efficient API usage
            Map<String, List<Bookmark>> groupedAlerts = groupAlertsBySearchCriteria(activeAlerts);
            logger.info("Grouped alerts into {} unique search criteria", groupedAlerts.size());
            
            int processedAlerts = 0;
            int triggeredAlerts = 0;
            
            // Process each group
            for (Map.Entry<String, List<Bookmark>> entry : groupedAlerts.entrySet()) {
                try {
                    String searchKey = entry.getKey();
                    List<Bookmark> bookmarksForSearch = entry.getValue();
                    
                    logger.info("Processing search group: {} with {} bookmarks", searchKey, bookmarksForSearch.size());
                    
                    // Execute flight search for this group
                    FlightSearchResponse searchResponse = executeFlightSearch(bookmarksForSearch.get(0));
                    
                    if (searchResponse == null || searchResponse.getData() == null) {
                        logger.warn("No search results for group: {}", searchKey);
                        continue;
                    }
                    
                    // Evaluate alerts for each bookmark in this group
                    for (Bookmark bookmark : bookmarksForSearch) {
                        try {
                            processedAlerts++;
                            
                            AlertEvaluationService.AlertEvaluationResult result = 
                                alertEvaluationService.evaluateAlert(bookmark, searchResponse);
                            
                            // Log AlertConfig values before updating timestamp
                            logAlertConfigValues("BEFORE_UPDATE_TIMESTAMP", bookmark);
                            
                            // Update last evaluated timestamp
                            bookmark.getAlertConfig().updateLastEvaluated();
                            
                            // Log AlertConfig values after updating timestamp
                            logAlertConfigValues("AFTER_UPDATE_TIMESTAMP", bookmark);
                            
                            if (result.isTriggered()) {
                                // Check if we should send notification (avoid duplicates)
                                if (shouldSendNotification(bookmark, result)) {
                                    sendAlertNotification(bookmark, result);
                                    triggeredAlerts++;
                                    
                                    // Log AlertConfig values before recording trigger
                                    logAlertConfigValues("BEFORE_RECORD_TRIGGER", bookmark);
                                    
                                    // Record trigger
                                    bookmark.getAlertConfig().recordTrigger();
                                    
                                    // Log AlertConfig values after recording trigger
                                    logAlertConfigValues("AFTER_RECORD_TRIGGER", bookmark);
                                }
                            }
                            
                            // Log AlertConfig values before saving bookmark
                            logAlertConfigValues("BEFORE_BOOKMARK_SAVE", bookmark);
                            
                            // Save updated bookmark
                            bookmarkRepository.saveBookmark(bookmark);
                            
                            // Log AlertConfig values after saving bookmark
                            logAlertConfigValues("AFTER_BOOKMARK_SAVE", bookmark);
                            
                        } catch (Exception e) {
                            logger.error("Error processing alert for bookmark {}: {}", 
                                bookmark.getBookmarkId(), e.getMessage(), e);
                        }
                    }
                    
                    // Add delay between search groups to respect API rate limits
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    logger.error("Error processing search group {}: {}", entry.getKey(), e.getMessage(), e);
                }
            }
            
            String result = String.format("Processed %d alerts, triggered %d notifications", 
                processedAlerts, triggeredAlerts);
            logger.info("Alert processor completed: {}", result);
            return result;
            
        } catch (Exception e) {
            logger.error("Error in alert processor batch job", e);
            return "Error processing alerts: " + e.getMessage();
        }
    }
    
    /**
     * Group alerts by search criteria to minimize API calls
     */
    private Map<String, List<Bookmark>> groupAlertsBySearchCriteria(List<Bookmark> alerts) {
        Map<String, List<Bookmark>> groups = new HashMap<>();
        
        for (Bookmark bookmark : alerts) {
            String searchKey = generateSearchKey(bookmark);
            groups.computeIfAbsent(searchKey, k -> new ArrayList<>()).add(bookmark);
        }
        
        return groups;
    }
    
    /**
     * Generate a search key for grouping bookmarks with similar search criteria
     */
    private String generateSearchKey(Bookmark bookmark) {
        if (bookmark.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
            // For saved searches, use the search criteria
            return String.format("%s-%s-%s-%s-%s", 
                bookmark.getOrigin() != null ? bookmark.getOrigin() : "",
                bookmark.getDestination() != null ? bookmark.getDestination() : "",
                bookmark.getDepartureDate() != null ? bookmark.getDepartureDate() : "",
                bookmark.getTravelClass() != null ? bookmark.getTravelClass() : "",
                bookmark.getAirlineCode() != null ? bookmark.getAirlineCode() : "");
        } else {
            // For individual bookmarks, extract full route from flight data (origin → final destination)
            try {
                var flightData = objectMapper.readTree(bookmark.getFlightOfferData());
                var itineraries = flightData.get("itineraries");
                if (itineraries != null && itineraries.size() > 0) {
                    var segments = itineraries.get(0).get("segments");
                    if (segments != null && segments.size() > 0) {
                        // Get origin from first segment and final destination from last segment
                        var firstSegment = segments.get(0);
                        var lastSegment = segments.get(segments.size() - 1);
                        
                        String origin = firstSegment.get("departure").get("iataCode").asText();
                        String finalDestination = lastSegment.get("arrival").get("iataCode").asText();
                        String date = firstSegment.get("departure").get("at").asText().substring(0, 10);
                        
                        return String.format("%s-%s-%s", origin, finalDestination, date);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error extracting search key from bookmark {}: {}", 
                    bookmark.getBookmarkId(), e.getMessage());
            }
            // Fallback to bookmark ID for unique grouping
            return "bookmark-" + bookmark.getBookmarkId();
        }
    }
    
    /**
     * Execute flight search based on bookmark criteria
     */
    private FlightSearchResponse executeFlightSearch(Bookmark bookmark) {
        try {
            if (bookmark.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
                // Use saved search criteria - search for multiple flights
                FlightSearchRequest request = bookmark.toFlightSearchRequest();
                return flightSearchService.searchFlightsWithSeatmaps(request);
            } else {
                // For individual bookmark - get fresh seatmap for the specific flight
                FlightSearchResult result = getBookmarkFlightWithFreshSeatmap(bookmark);
                if (result != null) {
                    // Create a response with single flight
                    FlightSearchResponse response = new FlightSearchResponse();
                    response.setData(List.of(result));
                    return response;
                }
            }
        } catch (Exception e) {
            logger.error("Error executing flight search for bookmark {}: {}", 
                bookmark.getBookmarkId(), e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Get fresh seatmap data for a specific bookmarked flight using same logic as FlightSearchHandler
     */
    private FlightSearchResult getBookmarkFlightWithFreshSeatmap(Bookmark bookmark) {
        try {
            var flightData = objectMapper.readTree(bookmark.getFlightOfferData());
            
            // Determine data source from flight offer structure or default to AMADEUS
            String dataSource = determineDataSource(flightData);
            
            // Get fresh seatmap based on data source
            return fetchFlightWithFreshSeatmap(flightData, dataSource);
            
        } catch (Exception e) {
            logger.error("Error getting fresh seatmap for bookmark {}: {}", 
                bookmark.getBookmarkId(), e.getMessage(), e);
        }
        return null;
    }
    
    private FlightSearchResult fetchFlightWithFreshSeatmap(com.fasterxml.jackson.databind.JsonNode flightOffer, String dataSource) throws Exception {
        try {
            if ("AMADEUS".equals(dataSource)) {
                // Use Amadeus service to get fresh seatmap
                com.fasterxml.jackson.databind.JsonNode seatMapResponse = amadeusService.getSeatMapFromOffer(flightOffer);
                com.seatmap.api.model.SeatMapData seatMapData = amadeusService.convertToSeatMapData(seatMapResponse);
                return new FlightSearchResult(flightOffer, seatMapData, true, null);
            } else if ("SABRE".equals(dataSource)) {
                // TODO: Add Sabre support for alert processor
                throw new Exception("Sabre data source not yet supported in alert processor");
            } else {
                throw new Exception("Unknown data source: " + dataSource);
            }
        } catch (Exception e) {
            // Since this flight was previously bookmarked with working seatmap,
            // failure now indicates temporary API issues (not permanent unsupported flight)
            logger.error("Seatmap temporarily unavailable for bookmarked flight: {}", e.getMessage());
            throw new com.seatmap.common.exception.SeatmapException(
                "SEATMAP_TEMPORARILY_UNAVAILABLE", 
                "Seatmap is temporarily unavailable. Please try again later.", 
                500, 
                e
            );
        }
    }
    
    private String determineDataSource(com.fasterxml.jackson.databind.JsonNode flightOffer) {
        try {
            return flightOffer.path("dataSource").asText("AMADEUS"); // Default to AMADEUS if not found
        } catch (Exception e) {
            logger.warn("Error extracting dataSource from flight offer, defaulting to AMADEUS", e);
            return "AMADEUS";
        }
    }
    
    private String extractFromFlightOffer(com.fasterxml.jackson.databind.JsonNode flightOffer, String field) {
        if (flightOffer.has("itineraries") && flightOffer.get("itineraries").isArray() && flightOffer.get("itineraries").size() > 0) {
            com.fasterxml.jackson.databind.JsonNode firstItinerary = flightOffer.get("itineraries").get(0);
            if (firstItinerary.has("segments") && firstItinerary.get("segments").isArray() && firstItinerary.get("segments").size() > 0) {
                com.fasterxml.jackson.databind.JsonNode firstSegment = firstItinerary.get("segments").get(0);
                
                switch (field) {
                    case "carrierCode":
                        return firstSegment.path("carrierCode").asText("");
                    case "number":
                        return firstSegment.path("number").asText("");
                    case "origin":
                        return firstSegment.path("departure").path("iataCode").asText("");
                    case "destination":
                        return firstSegment.path("arrival").path("iataCode").asText("");
                    case "departureDate":
                        String departureAt = firstSegment.path("departure").path("at").asText("");
                        return departureAt.length() >= 10 ? departureAt.substring(0, 10) : "";
                }
            }
        }
        return "";
    }
    
    
    /**
     * Determine if we should send a notification (avoid spam)
     */
    private boolean shouldSendNotification(Bookmark bookmark, AlertEvaluationService.AlertEvaluationResult result) {
        Instant lastTriggered = bookmark.getAlertConfig().getLastTriggered();
        
        // If never triggered before, send notification
        if (lastTriggered == null) {
            return true;
        }
        
        // Check if flight is departing within 3 hours - if so, always allow alert
        Instant flightDepartureTime = getFlightDepartureTime(bookmark);
        if (flightDepartureTime != null) {
            Instant threeHoursFromNow = Instant.now().plusSeconds(3 * 60 * 60);
            if (flightDepartureTime.isBefore(threeHoursFromNow)) {
                logger.debug("Allowing notification for bookmark {} - flight departs within 3 hours", 
                    bookmark.getBookmarkId());
                return true;
            }
        }
        
        // Don't send notification if triggered within last 45 hours (avoid spam)
        Instant fortyFiveHoursAgo = Instant.now().minusSeconds(45 * 60 * 60);
        if (lastTriggered.isAfter(fortyFiveHoursAgo)) {
            logger.debug("Skipping notification for bookmark {} - already notified within 45 hours", 
                bookmark.getBookmarkId());
            return false;
        }
        
        return true;
    }
    
    /**
     * Extract flight departure time from bookmark data
     */
    private Instant getFlightDepartureTime(Bookmark bookmark) {
        try {
            if (bookmark.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
                // For saved searches, use the departureDate field
                if (bookmark.getDepartureDate() != null) {
                    return java.time.LocalDate.parse(bookmark.getDepartureDate())
                        .atStartOfDay()
                        .atZone(java.time.ZoneOffset.UTC)
                        .toInstant();
                }
            } else if (bookmark.getItemType() == Bookmark.ItemType.BOOKMARK) {
                // For individual flight bookmarks, parse the flight offer data
                if (bookmark.getFlightOfferData() != null) {
                    var flightData = objectMapper.readTree(bookmark.getFlightOfferData());
                    var itineraries = flightData.get("itineraries");
                    if (itineraries != null && itineraries.size() > 0) {
                        // Get departure time from first segment of first itinerary
                        var segments = itineraries.get(0).get("segments");
                        if (segments != null && segments.size() > 0) {
                            var departureAt = segments.get(0).get("departure").get("at");
                            if (departureAt != null) {
                                return Instant.parse(departureAt.asText());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting flight departure time from bookmark {}: {}", 
                bookmark.getBookmarkId(), e.getMessage());
        }
        return null;
    }
    
    /**
     * Send alert notification email
     */
    private void sendAlertNotification(Bookmark bookmark, AlertEvaluationService.AlertEvaluationResult result) {
        try {
            // Get user details
            Optional<User> userOpt = userRepository.findByKey(bookmark.getUserId());
            if (userOpt.isEmpty()) {
                logger.warn("User not found for bookmark {}: {}", 
                    bookmark.getBookmarkId(), bookmark.getUserId());
                return;
            }
            
            User user = userOpt.get();
            
            // Send alert email
            emailService.sendSeatAvailabilityAlert(
                user.getEmail(), 
                user.getFirstName(), 
                bookmark, 
                result
            );
            
            logger.info("Sent alert notification for bookmark {} to user {}", 
                bookmark.getBookmarkId(), user.getEmail());
                
        } catch (Exception e) {
            logger.error("Error sending alert notification for bookmark {}: {}", 
                bookmark.getBookmarkId(), e.getMessage(), e);
        }
    }
    
    /**
     * Log AlertConfig values for debugging in alert processing workflow
     */
    private void logAlertConfigValues(String context, com.seatmap.common.model.Bookmark bookmark) {
        if (bookmark == null) {
            logger.debug("[ALERTCONFIG_PROCESSOR_DEBUG] {}: Bookmark is NULL", context);
            return;
        }
        
        String bookmarkId = bookmark.getBookmarkId();
        String userId = bookmark.getUserId();
        
        if (bookmark.getAlertConfig() == null) {
            logger.debug("[ALERTCONFIG_PROCESSOR_DEBUG] {}: BookmarkId={}, UserId={}, AlertConfig=NULL", 
                context, bookmarkId, userId);
            return;
        }
        
        com.seatmap.common.model.Bookmark.AlertConfig alertConfig = bookmark.getAlertConfig();
        
        logger.info("[ALERTCONFIG_PROCESSOR_DEBUG] {}: BookmarkId={}, UserId={}, " +
            "AlertThreshold={}, LastEvaluated={}, LastTriggered={}, TriggerHistory={}", 
            context, bookmarkId, userId,
            alertConfig.getAlertThreshold(),
            alertConfig.getLastEvaluated(),
            alertConfig.getLastTriggered(),
            alertConfig.getTriggerHistory() != null ? "[" + alertConfig.getTriggerHistory().length() + " chars]" : "null"
        );
    }
}