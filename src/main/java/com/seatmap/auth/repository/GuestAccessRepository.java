package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.GuestAccessHistory;
import com.seatmap.common.repository.DynamoDbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Repository for managing guest access history and IP-based rate limiting
 */
public class GuestAccessRepository extends DynamoDbRepository<GuestAccessHistory> {
    private static final Logger logger = LoggerFactory.getLogger(GuestAccessRepository.class);
    private static final String TABLE_NAME_PREFIX = "seatmap-guest-access-";
    
    public GuestAccessRepository(DynamoDbClient dynamoDbClient) {
        super(dynamoDbClient, TABLE_NAME_PREFIX + getEnvironment());
    }
    
    @Override
    protected Class<GuestAccessHistory> getEntityClass() {
        return GuestAccessHistory.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "ipAddress";
    }
    
    /**
     * Get guest access history for an IP address
     */
    public Optional<GuestAccessHistory> findByIpAddress(String ipAddress) {
        logger.debug("Finding guest access history for IP: {}", ipAddress);
        
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("ipAddress", AttributeValue.builder().s(ipAddress).build());
            
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
            logger.error("Error finding guest access history for IP: {}", ipAddress, e);
            return Optional.empty();
        }
    }
    
    /**
     * Save or update guest access history
     */
    public void save(GuestAccessHistory guestAccessHistory) {
        logger.debug("Saving guest access history for IP: {}", guestAccessHistory.getIpAddress());
        
        try {
            Map<String, AttributeValue> item = toAttributeValueMap(guestAccessHistory);
            
            PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(item)
                .build();
                
            dynamoDbClient.putItem(request);
            
            logger.info("Guest access history saved for IP: {}", guestAccessHistory.getIpAddress());
            
        } catch (Exception e) {
            logger.error("Error saving guest access history for IP: {}", 
                guestAccessHistory.getIpAddress(), e);
            throw new RuntimeException("Failed to save guest access history", e);
        }
    }
    
    /**
     * Get or create guest access history for an IP address
     */
    public GuestAccessHistory getOrCreate(String ipAddress) {
        Optional<GuestAccessHistory> existing = findByIpAddress(ipAddress);
        
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Create new guest access history
        GuestAccessHistory newHistory = new GuestAccessHistory(ipAddress);
        logger.info("Creating new guest access history for IP: {}", ipAddress);
        return newHistory;
    }
    
    /**
     * Check if an IP address can make a seatmap request
     */
    public boolean canMakeSeatmapRequest(String ipAddress) {
        GuestAccessHistory history = getOrCreate(ipAddress);
        return history.canMakeSeatmapRequest();
    }
    
    /**
     * Record a seatmap request for an IP address
     */
    public void recordSeatmapRequest(String ipAddress) {
        GuestAccessHistory history = getOrCreate(ipAddress);
        history.recordSeatmapRequest();
        save(history);
        
        logger.info("Recorded seatmap request for IP: {}. Total requests: {}/{}", 
            ipAddress, history.getSeatmapRequestsUsed(), 2);
    }
    
    /**
     * Get remaining seatmap requests for an IP address
     */
    public int getRemainingSeatmapRequests(String ipAddress) {
        GuestAccessHistory history = getOrCreate(ipAddress);
        return history.getRemainingSeatmapRequests();
    }
    
    /**
     * Get denial message for seatmap access
     */
    public String getSeatmapDenialMessage(String ipAddress) {
        GuestAccessHistory history = getOrCreate(ipAddress);
        return history.getSeatmapDenialMessage();
    }
    
    /**
     * Get environment name for table naming
     */
    private static String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        return env != null ? env : "dev";
    }
}