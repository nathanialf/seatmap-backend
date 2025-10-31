package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.UserUsageHistory;
import com.seatmap.common.repository.DynamoDbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing user usage history and tier-based rate limiting
 * Follows the same pattern as GuestAccessRepository for consistency
 */
public class UserUsageRepository extends DynamoDbRepository<UserUsageHistory> {
    private static final Logger logger = LoggerFactory.getLogger(UserUsageRepository.class);
    private static final String TABLE_NAME_PREFIX = "seatmap-user-usage-";
    
    public UserUsageRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME_PREFIX + getEnvironment());
    }
    
    @Override
    protected Class<UserUsageHistory> getEntityClass() {
        return UserUsageHistory.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "userId";
    }
    
    @Override
    protected String getRangeKeyName() {
        return "monthYear";
    }
    
    /**
     * Get user usage history for current month
     */
    public Optional<UserUsageHistory> findCurrentMonthUsage(String userId) {
        String currentMonth = UserUsageHistory.getCurrentMonthYear();
        return findByUserIdAndMonth(userId, currentMonth);
    }
    
    /**
     * Get user usage history for a specific month
     */
    public Optional<UserUsageHistory> findByUserIdAndMonth(String userId, String monthYear) {
        logger.debug("Finding usage history for user: {} month: {}", userId, monthYear);
        
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("userId", AttributeValue.builder().s(userId).build());
            key.put("monthYear", AttributeValue.builder().s(monthYear).build());
            
            GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(key)
                .build();
                
            GetItemResponse response = dynamoDbClient.getItem(request);
            
            if (response.hasItem()) {
                return Optional.of(fromAttributeValueMap(response.item()));
            }
            
            return Optional.empty();
            
        } catch (Exception e) {
            logger.error("Error finding usage history for user: {} month: {}", userId, monthYear, e);
            return Optional.empty();
        }
    }
    
    /**
     * Save or update user usage history
     */
    public void save(UserUsageHistory userUsageHistory) throws SeatmapException {
        logger.debug("Saving usage history for user: {} month: {}", 
            userUsageHistory.getUserId(), userUsageHistory.getMonthYear());
        
        try {
            userUsageHistory.updateTimestamp();
            Map<String, AttributeValue> item = toAttributeValueMap(userUsageHistory);
            
            PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
                
            dynamoDbClient.putItem(request);
            
            logger.info("Usage history saved for user: {} month: {}", 
                userUsageHistory.getUserId(), userUsageHistory.getMonthYear());
            
        } catch (Exception e) {
            logger.error("Error saving usage history for user: {} month: {}", 
                userUsageHistory.getUserId(), userUsageHistory.getMonthYear(), e);
            throw SeatmapException.internalError("Failed to save user usage history: " + e.getMessage());
        }
    }
    
    /**
     * Get or create usage history for current month
     */
    public UserUsageHistory getOrCreateCurrentMonth(String userId) {
        Optional<UserUsageHistory> existing = findCurrentMonthUsage(userId);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new usage history for current month
        UserUsageHistory newHistory = new UserUsageHistory(userId);
        logger.info("Creating new usage history for user: {} month: {}", userId, newHistory.getMonthYear());
        return newHistory;
    }
    
    /**
     * Record a bookmark creation for the current month
     */
    public void recordBookmarkCreation(String userId) throws SeatmapException {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        history.recordBookmarkCreation();
        save(history);
        
        logger.info("Recorded bookmark creation for user: {}. Total this month: {}", 
            userId, history.getBookmarksCreated());
    }
    
    /**
     * Record a seatmap request for the current month
     */
    public void recordSeatmapRequest(String userId) throws SeatmapException {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        history.recordSeatmapRequest();
        save(history);
        
        logger.info("Recorded seatmap request for user: {}. Total this month: {}", 
            userId, history.getSeatmapRequestsUsed());
    }
    
    /**
     * Get current month bookmark count for a user
     */
    public int getCurrentMonthBookmarkCount(String userId) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.getBookmarksCreated();
    }
    
    /**
     * Get current month seatmap request count for a user
     */
    public int getCurrentMonthSeatmapCount(String userId) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.getSeatmapRequestsUsed();
    }
    
    /**
     * Check if user can create a bookmark based on tier limit
     */
    public boolean canCreateBookmark(String userId, int tierLimit) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.canCreateBookmark(tierLimit);
    }
    
    /**
     * Check if user can make a seatmap request based on tier limit
     */
    public boolean canMakeSeatmapRequest(String userId, int tierLimit) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.canMakeSeatmapRequest(tierLimit);
    }
    
    /**
     * Get remaining bookmarks for the current month
     */
    public int getRemainingBookmarks(String userId, int tierLimit) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.getRemainingBookmarks(tierLimit);
    }
    
    /**
     * Get remaining seatmap requests for the current month
     */
    public int getRemainingSeatmapRequests(String userId, int tierLimit) {
        UserUsageHistory history = getOrCreateCurrentMonth(userId);
        return history.getRemainingSeatmapRequests(tierLimit);
    }
    
    /**
     * Get environment name for table naming
     */
    private static String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        return env != null ? env : "dev";
    }
}