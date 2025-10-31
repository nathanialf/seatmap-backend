package com.seatmap.auth.service;

import com.seatmap.auth.repository.UserUsageRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User.AccountTier;
import com.seatmap.common.model.TierDefinition;
import com.seatmap.common.model.User;
import com.seatmap.common.repository.DynamoDbRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing user usage limits based on account tiers
 * Provides tier-based validation for bookmarks and seatmap requests
 */
public class UserUsageLimitsService {
    private static final Logger logger = LoggerFactory.getLogger(UserUsageLimitsService.class);
    
    private final UserUsageRepository usageRepository;
    private final DynamoDbClient dynamoDbClient;
    private final String tierTableName;
    
    // In-memory cache for tier definitions (loaded lazily)
    private final Map<AccountTier, TierDefinition> tierDefinitionsCache = new ConcurrentHashMap<>();
    private volatile boolean tierDefinitionsLoaded = false;
    
    public UserUsageLimitsService(UserUsageRepository usageRepository, DynamoDbClient dynamoDbClient) {
        this.usageRepository = usageRepository;
        this.dynamoDbClient = dynamoDbClient;
        this.tierTableName = "seatmap-account-tiers-" + getEnvironment();
    }
    
    /**
     * Check if user can create a bookmark
     */
    public boolean canCreateBookmark(User user) {
        try {
            int tierLimit = getTierBookmarkLimit(user.getAccountTier());
            return usageRepository.canCreateBookmark(user.getUserId(), tierLimit);
        } catch (Exception e) {
            logger.error("Error checking bookmark limit for user: {}", user.getUserId(), e);
            return false; // Fail closed for safety
        }
    }
    
    /**
     * Check if user can make a seatmap request
     */
    public boolean canMakeSeatmapRequest(User user) {
        try {
            int tierLimit = getTierSeatmapLimit(user.getAccountTier());
            return usageRepository.canMakeSeatmapRequest(user.getUserId(), tierLimit);
        } catch (Exception e) {
            logger.error("Error checking seatmap limit for user: {}", user.getUserId(), e);
            return false; // Fail closed for safety
        }
    }
    
    /**
     * Record a bookmark creation and validate limit
     */
    public void recordBookmarkCreation(User user) throws SeatmapException {
        if (!canCreateBookmark(user)) {
            int tierLimit = getTierBookmarkLimit(user.getAccountTier());
            int currentCount = usageRepository.getCurrentMonthBookmarkCount(user.getUserId());
            
            throw SeatmapException.forbidden(
                getBookmarkLimitErrorMessage(user.getAccountTier(), tierLimit, currentCount)
            );
        }
        
        usageRepository.recordBookmarkCreation(user.getUserId());
        logger.info("Recorded bookmark creation for user: {} tier: {}", 
            user.getUserId(), user.getAccountTier());
    }
    
    /**
     * Record a seatmap request and validate limit
     */
    public void recordSeatmapRequest(User user) throws SeatmapException {
        if (!canMakeSeatmapRequest(user)) {
            int tierLimit = getTierSeatmapLimit(user.getAccountTier());
            int currentCount = usageRepository.getCurrentMonthSeatmapCount(user.getUserId());
            
            throw SeatmapException.forbidden(
                getSeatmapLimitErrorMessage(user.getAccountTier(), tierLimit, currentCount)
            );
        }
        
        usageRepository.recordSeatmapRequest(user.getUserId());
        logger.info("Recorded seatmap request for user: {} tier: {}", 
            user.getUserId(), user.getAccountTier());
    }
    
    /**
     * Get remaining bookmarks for user's current tier
     */
    public int getRemainingBookmarks(User user) {
        int tierLimit = getTierBookmarkLimit(user.getAccountTier());
        return usageRepository.getRemainingBookmarks(user.getUserId(), tierLimit);
    }
    
    /**
     * Get remaining seatmap requests for user's current tier  
     */
    public int getRemainingSeatmapRequests(User user) {
        int tierLimit = getTierSeatmapLimit(user.getAccountTier());
        return usageRepository.getRemainingBookmarks(user.getUserId(), tierLimit);
    }
    
    /**
     * Validate tier transition (enforce business tier cannot downgrade)
     */
    public void validateTierTransition(AccountTier fromTier, AccountTier toTier) throws SeatmapException {
        if (fromTier == AccountTier.BUSINESS) {
            throw SeatmapException.forbidden(
                "Business tier cannot be downgraded (one-time purchase). Contact support for assistance."
            );
        }
        
        logger.info("Validated tier transition from {} to {}", fromTier, toTier);
    }
    
    /**
     * Get bookmark limit for a tier (deny all if tier definitions unavailable)
     */
    private int getTierBookmarkLimit(AccountTier tier) {
        TierDefinition tierDef = getTierDefinition(tier);
        return tierDef != null ? tierDef.getMaxBookmarks() : 0; // Deny all bookmarks if tier definition missing
    }
    
    /**
     * Get seatmap limit for a tier (deny all if tier definitions unavailable)
     */
    private int getTierSeatmapLimit(AccountTier tier) {
        TierDefinition tierDef = getTierDefinition(tier);
        return tierDef != null ? tierDef.getMaxSeatmapCalls() : 0; // Deny all seatmap calls if tier definition missing
    }
    
    /**
     * Get tier definition from cache, loading if necessary
     */
    private TierDefinition getTierDefinition(AccountTier tier) {
        if (!tierDefinitionsLoaded) {
            synchronized (this) {
                if (!tierDefinitionsLoaded) {
                    loadTierDefinitions();
                    tierDefinitionsLoaded = true;
                }
            }
        }
        
        return tierDefinitionsCache.get(tier);
    }
    
    /**
     * Load tier definitions into cache
     */
    private void loadTierDefinitions() {
        try {
            logger.info("Loading tier definitions into cache");
            
            // Scan tier definitions table
            ScanRequest request = ScanRequest.builder()
                .tableName(tierTableName)
                .filterExpression("active = :active")
                .expressionAttributeValues(Map.of(
                    ":active", AttributeValue.builder().bool(true).build()
                ))
                .build();
                
            ScanResponse response = dynamoDbClient.scan(request);
            
            for (Map<String, AttributeValue> item : response.items()) {
                try {
                    TierDefinition tierDef = fromAttributeValueMap(item);
                    AccountTier tier = AccountTier.valueOf(tierDef.getTierName());
                    tierDefinitionsCache.put(tier, tierDef);
                    
                    logger.debug("Cached tier definition: {} - bookmarks: {}, seatmaps: {}", 
                        tier, tierDef.getMaxBookmarks(), tierDef.getMaxSeatmapCalls());
                        
                } catch (Exception e) {
                    logger.error("Error parsing tier definition: {}", item, e);
                }
            }
            
            logger.info("Loaded {} tier definitions into cache", tierDefinitionsCache.size());
            
            if (tierDefinitionsCache.isEmpty()) {
                logger.error("No active tier definitions found in database - denying all requests");
            }
            
        } catch (Exception e) {
            logger.error("Error loading tier definitions - denying all requests", e);
            tierDefinitionsCache.clear(); // Ensure cache is empty to trigger deny-all behavior
        }
    }
    
    
    /**
     * Convert DynamoDB item to TierDefinition
     */
    private TierDefinition fromAttributeValueMap(Map<String, AttributeValue> item) {
        TierDefinition tierDef = new TierDefinition();
        
        if (item.containsKey("tierId")) tierDef.setTierId(item.get("tierId").s());
        if (item.containsKey("tierName")) tierDef.setTierName(item.get("tierName").s());
        if (item.containsKey("displayName")) tierDef.setDisplayName(item.get("displayName").s());
        if (item.containsKey("maxBookmarks")) tierDef.setMaxBookmarks(Integer.parseInt(item.get("maxBookmarks").n()));
        if (item.containsKey("maxSeatmapCalls")) tierDef.setMaxSeatmapCalls(Integer.parseInt(item.get("maxSeatmapCalls").n()));
        if (item.containsKey("canDowngrade")) tierDef.setCanDowngrade(item.get("canDowngrade").bool());
        
        return tierDef;
    }
    
    /**
     * Get user-friendly error message for bookmark limit exceeded
     */
    private String getBookmarkLimitErrorMessage(AccountTier tier, int limit, int currentCount) {
        if (limit == 0) {
            return String.format("Bookmark creation is not available for %s tier. Upgrade to PRO (50 bookmarks/month) or BUSINESS (unlimited) for bookmark access.", tier);
        }
        
        return String.format("Monthly bookmark limit reached (%d/%d) for %s tier. Upgrade to BUSINESS for unlimited bookmarks.", 
            currentCount, limit, tier);
    }
    
    /**
     * Get user-friendly error message for seatmap limit exceeded
     */
    private String getSeatmapLimitErrorMessage(AccountTier tier, int limit, int currentCount) {
        return String.format("Monthly seat map limit reached (%d/%d) for %s tier. Upgrade for higher limits.", 
            currentCount, limit, tier);
    }
    
    /**
     * Get environment name for table naming
     */
    private static String getEnvironment() {
        String env = System.getenv("ENVIRONMENT");
        return env != null ? env : "dev";
    }
}