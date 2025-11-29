package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.repository.DynamoDbRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookmarkRepository extends DynamoDbRepository<Bookmark> {
    
    public BookmarkRepository(DynamoDbClient dynamoDbClient, String tableName) {
        super(dynamoDbClient, tableName);
    }
    
    @Override
    protected Class<Bookmark> getEntityClass() {
        return Bookmark.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "userId";
    }
    
    @Override
    protected String getRangeKeyName() {
        return "bookmarkId";
    }
    
    /**
     * Find all bookmarks for a specific user
     */
    public List<Bookmark> findByUserId(String userId) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":userId", AttributeValue.builder().s(userId).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("userId = :userId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            return response.items().stream()
                    .map(item -> {
                        try {
                            return fromAttributeValueMap(item);
                        } catch (SeatmapException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to retrieve bookmarks for user: " + e.getMessage());
        }
    }
    
    /**
     * Find a specific bookmark by user ID and bookmark ID
     */
    public Optional<Bookmark> findByUserIdAndBookmarkId(String userId, String bookmarkId) throws SeatmapException {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("userId", AttributeValue.builder().s(userId).build());
            key.put("bookmarkId", AttributeValue.builder().s(bookmarkId).build());
            
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();
                    
            GetItemResponse response = dynamoDbClient.getItem(request);
            
            if (response.hasItem()) {
                return Optional.of(fromAttributeValueMap(response.item()));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to retrieve bookmark: " + e.getMessage());
        }
    }
    
    /**
     * Save a bookmark (create or update)
     */
    public void saveBookmark(Bookmark bookmark) throws SeatmapException {
        try {
            bookmark.setUpdatedAt(Instant.now());
            
            Map<String, AttributeValue> item = toAttributeValueMap(bookmark);
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
                    
            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to save bookmark: " + e.getMessage());
        }
    }
    
    /**
     * Delete a specific bookmark
     */
    public void deleteBookmark(String userId, String bookmarkId) throws SeatmapException {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("userId", AttributeValue.builder().s(userId).build());
            key.put("bookmarkId", AttributeValue.builder().s(bookmarkId).build());
            
            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();
                    
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to delete bookmark: " + e.getMessage());
        }
    }
    
    /**
     * Count bookmarks for a user (for implementing max limit)
     */
    public int countBookmarksByUserId(String userId) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":userId", AttributeValue.builder().s(userId).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("userId = :userId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .select(Select.COUNT)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            return response.count();
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to count bookmarks for user: " + e.getMessage());
        }
    }
    
    /**
     * Find bookmarks by user ID and item type
     */
    public List<Bookmark> findByUserIdAndItemType(String userId, Bookmark.ItemType itemType) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":userId", AttributeValue.builder().s(userId).build());
            expressionAttributeValues.put(":itemType", AttributeValue.builder().s(itemType.name()).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("userId = :userId")
                    .filterExpression("itemType = :itemType")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            return response.items().stream()
                    .map(item -> {
                        try {
                            return fromAttributeValueMap(item);
                        } catch (SeatmapException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to retrieve items by type for user: " + e.getMessage());
        }
    }
    
    /**
     * Find only flight bookmarks for a user
     */
    public List<Bookmark> findFlightBookmarksByUserId(String userId) throws SeatmapException {
        return findByUserIdAndItemType(userId, Bookmark.ItemType.BOOKMARK);
    }
    
    /**
     * Find only saved searches for a user
     */
    public List<Bookmark> findSavedSearchesByUserId(String userId) throws SeatmapException {
        return findByUserIdAndItemType(userId, Bookmark.ItemType.SAVED_SEARCH);
    }
    
    /**
     * Count items by type for a user
     */
    public int countItemsByUserIdAndType(String userId, Bookmark.ItemType itemType) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":userId", AttributeValue.builder().s(userId).build());
            expressionAttributeValues.put(":itemType", AttributeValue.builder().s(itemType.name()).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .keyConditionExpression("userId = :userId")
                    .filterExpression("itemType = :itemType")
                    .expressionAttributeValues(expressionAttributeValues)
                    .select(Select.COUNT)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            return response.count();
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to count items by type for user: " + e.getMessage());
        }
    }
    
    /**
     * Find all bookmarks with active alerts across all users for batch processing
     * This method performs a full table scan with filter for alertConfig.alertThreshold attribute_exists
     */
    public List<Bookmark> findBookmarksWithActiveAlerts() throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":currentTime", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build());
            
            ScanRequest request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("attribute_exists(alertConfig.alertThreshold) AND (attribute_not_exists(expiresAt) OR expiresAt > :currentTime)")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            ScanResponse response = dynamoDbClient.scan(request);
            
            return response.items().stream()
                    .map(item -> {
                        try {
                            return fromAttributeValueMap(item);
                        } catch (SeatmapException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .filter(bookmark -> bookmark.getAlertConfig() != null && bookmark.getAlertConfig().isEnabled())
                    .filter(bookmark -> !bookmark.isExpired())
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find bookmarks with active alerts: " + e.getMessage());
        }
    }
    
    /**
     * Find bookmarks with active alerts for flights departing within the next 14 days
     */
    public List<Bookmark> findBookmarksWithActiveAlertsForUpcomingFlights() throws SeatmapException {
        List<Bookmark> allActiveAlerts = findBookmarksWithActiveAlerts();
        
        // Filter for flights departing within next 14 days
        Instant fourteenDaysFromNow = Instant.now().plusSeconds(14 * 24 * 60 * 60);
        
        return allActiveAlerts.stream()
                .filter(bookmark -> {
                    Instant flightDepartureTime = getFlightDepartureTime(bookmark);
                    if (flightDepartureTime == null) {
                        return true; // Can't determine departure time, include it to be safe
                    }
                    // Include if flight departs within next 14 days
                    return flightDepartureTime.isBefore(fourteenDaysFromNow) && flightDepartureTime.isAfter(Instant.now());
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Extract earliest flight departure time from all segments in bookmark data
     */
    private Instant getFlightDepartureTime(Bookmark bookmark) {
        try {
            if (bookmark.getItemType() == Bookmark.ItemType.SAVED_SEARCH) {
                // For saved searches, use the departureDate field
                if (bookmark.getDepartureDate() != null) {
                    return java.time.LocalDate.parse(bookmark.getDepartureDate()).atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant();
                }
            } else if (bookmark.getItemType() == Bookmark.ItemType.BOOKMARK) {
                // For individual flight bookmarks, parse the flight offer data and check ALL segments
                if (bookmark.getFlightOfferData() != null) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode flightData = mapper.readTree(bookmark.getFlightOfferData());
                    com.fasterxml.jackson.databind.JsonNode itineraries = flightData.get("itineraries");
                    if (itineraries != null && itineraries.size() > 0) {
                        java.time.Instant earliestDeparture = null;
                        
                        // Check all itineraries (outbound/return)
                        for (int i = 0; i < itineraries.size(); i++) {
                            com.fasterxml.jackson.databind.JsonNode segments = itineraries.get(i).get("segments");
                            if (segments != null && segments.size() > 0) {
                                // Check all segments in this itinerary
                                for (int j = 0; j < segments.size(); j++) {
                                    com.fasterxml.jackson.databind.JsonNode departure = segments.get(j).get("departure");
                                    if (departure != null && departure.has("at")) {
                                        String departureTime = departure.get("at").asText();
                                        java.time.Instant segmentDeparture = java.time.Instant.parse(departureTime);
                                        
                                        // Keep track of earliest departure across all segments
                                        if (earliestDeparture == null || segmentDeparture.isBefore(earliestDeparture)) {
                                            earliestDeparture = segmentDeparture;
                                        }
                                    }
                                }
                            }
                        }
                        return earliestDeparture;
                    }
                }
            }
        } catch (Exception e) {
            // Log error but don't fail - return null to include bookmark in processing
            System.err.println("Error extracting departure time from bookmark " + bookmark.getBookmarkId() + ": " + e.getMessage());
        }
        return null;
    }
}