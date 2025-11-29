package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Bookmark;
import com.seatmap.common.repository.DynamoDbRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookmarkRepository extends DynamoDbRepository<Bookmark> {
    
    private static final Logger logger = LoggerFactory.getLogger(BookmarkRepository.class);
    
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
                            // Log DynamoDB raw data before deserialization
                            logDynamoDbAlertConfig("DYNAMODB_QUERY_LOAD", item);
                            
                            Bookmark bookmark = fromAttributeValueMap(item);
                            
                            // Log AlertConfig values after loading from DynamoDB
                            logAlertConfigValues("AFTER_QUERY_LOAD", bookmark);
                            
                            return bookmark;
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
                // Log DynamoDB raw data before deserialization
                logDynamoDbAlertConfig("DYNAMODB_LOAD", response.item());
                
                Bookmark bookmark = fromAttributeValueMap(response.item());
                
                // Log AlertConfig values after loading from DynamoDB
                logAlertConfigValues("AFTER_LOAD", bookmark);
                
                return Optional.of(bookmark);
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
            
            // Log AlertConfig values before saving to DynamoDB
            logAlertConfigValues("BEFORE_SAVE", bookmark);
            
            Map<String, AttributeValue> item = toAttributeValueMap(bookmark);
            
            // Log DynamoDB AttributeValue structure for AlertConfig
            logDynamoDbAlertConfig("DYNAMODB_SAVE", item);
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
                    
            dynamoDbClient.putItem(request);
            
            logger.info("Successfully saved bookmark {} for user {} with AlertConfig", 
                bookmark.getBookmarkId(), bookmark.getUserId());
        } catch (DynamoDbException e) {
            logger.error("Failed to save bookmark {} for user {}: {}", 
                bookmark.getBookmarkId(), bookmark.getUserId(), e.getMessage());
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
    
    /**
     * Log AlertConfig values for debugging field preservation issues
     */
    private void logAlertConfigValues(String context, Bookmark bookmark) {
        if (bookmark == null) {
            logger.debug("[ALERTCONFIG_DEBUG] {}: Bookmark is NULL", context);
            return;
        }
        
        String bookmarkId = bookmark.getBookmarkId();
        String userId = bookmark.getUserId();
        
        if (bookmark.getAlertConfig() == null) {
            logger.debug("[ALERTCONFIG_DEBUG] {}: BookmarkId={}, UserId={}, AlertConfig=NULL", 
                context, bookmarkId, userId);
            return;
        }
        
        Bookmark.AlertConfig alertConfig = bookmark.getAlertConfig();
        
        logger.info("[ALERTCONFIG_DEBUG] {}: BookmarkId={}, UserId={}, " +
            "AlertThreshold={}, LastEvaluated={}, LastTriggered={}, TriggerHistory={}", 
            context, bookmarkId, userId,
            alertConfig.getAlertThreshold(),
            alertConfig.getLastEvaluated(),
            alertConfig.getLastTriggered(),
            alertConfig.getTriggerHistory() != null ? "[" + alertConfig.getTriggerHistory().length() + " chars]" : "null"
        );
    }
    
    /**
     * Log DynamoDB AttributeValue structure for AlertConfig
     */
    private void logDynamoDbAlertConfig(String context, Map<String, AttributeValue> item) {
        if (item == null) {
            logger.debug("[ALERTCONFIG_DDB_DEBUG] {}: DynamoDB item is NULL", context);
            return;
        }
        
        AttributeValue userIdAttr = item.get("userId");
        AttributeValue bookmarkIdAttr = item.get("bookmarkId");
        AttributeValue alertConfigAttr = item.get("alertConfig");
        
        String userId = (userIdAttr != null && userIdAttr.s() != null) ? userIdAttr.s() : "unknown";
        String bookmarkId = (bookmarkIdAttr != null && bookmarkIdAttr.s() != null) ? bookmarkIdAttr.s() : "unknown";
        
        if (alertConfigAttr == null) {
            logger.debug("[ALERTCONFIG_DDB_DEBUG] {}: BookmarkId={}, UserId={}, AlertConfig AttributeValue=NULL", 
                context, bookmarkId, userId);
            return;
        }
        
        if (alertConfigAttr.m() == null) {
            logger.warn("[ALERTCONFIG_DDB_DEBUG] {}: BookmarkId={}, UserId={}, AlertConfig AttributeValue is NOT a Map: {}", 
                context, bookmarkId, userId, alertConfigAttr);
            return;
        }
        
        Map<String, AttributeValue> alertConfigMap = alertConfigAttr.m();
        
        AttributeValue thresholdAttr = alertConfigMap.get("alertThreshold");
        AttributeValue lastEvaluatedAttr = alertConfigMap.get("lastEvaluated");
        AttributeValue lastTriggeredAttr = alertConfigMap.get("lastTriggered");
        AttributeValue triggerHistoryAttr = alertConfigMap.get("triggerHistory");
        
        logger.info("[ALERTCONFIG_DDB_DEBUG] {}: BookmarkId={}, UserId={}, " +
            "AlertThreshold=[Type:{}, Value:{}], " +
            "LastEvaluated=[Type:{}, Value:{}], " +
            "LastTriggered=[Type:{}, Value:{}], " +
            "TriggerHistory=[Type:{}, Value:{}]",
            context, bookmarkId, userId,
            getAttributeType(thresholdAttr), getAttributeValue(thresholdAttr),
            getAttributeType(lastEvaluatedAttr), getAttributeValue(lastEvaluatedAttr),
            getAttributeType(lastTriggeredAttr), getAttributeValue(lastTriggeredAttr),
            getAttributeType(triggerHistoryAttr), getTruncatedAttributeValue(triggerHistoryAttr, 50)
        );
    }
    
    private String getAttributeType(AttributeValue attr) {
        if (attr == null) return "NULL";
        if (attr.s() != null) return "S";
        if (attr.n() != null) return "N";
        if (attr.bool() != null) return "BOOL";
        if (attr.nul() != null && attr.nul()) return "NULL";
        if (attr.m() != null) return "M";
        if (attr.l() != null) return "L";
        return "UNKNOWN";
    }
    
    private String getAttributeValue(AttributeValue attr) {
        if (attr == null) return "null";
        if (attr.s() != null) return attr.s();
        if (attr.n() != null) return attr.n();
        if (attr.bool() != null) return attr.bool().toString();
        if (attr.nul() != null && attr.nul()) return "null";
        if (attr.m() != null) return "Map[" + attr.m().keySet() + "]";
        if (attr.l() != null) return "List[" + attr.l().size() + "]";
        return "unknown";
    }
    
    private String getTruncatedAttributeValue(AttributeValue attr, int maxLength) {
        String value = getAttributeValue(attr);
        if (value != null && value.length() > maxLength) {
            return value.substring(0, maxLength) + "...";
        }
        return value;
    }
}