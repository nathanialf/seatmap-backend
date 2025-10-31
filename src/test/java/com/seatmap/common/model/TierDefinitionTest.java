package com.seatmap.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TierDefinitionTest {
    
    private TierDefinition tierDefinition;
    
    @BeforeEach
    void setUp() {
        tierDefinition = new TierDefinition();
    }
    
    @Test
    void constructor_DefaultValues_ShouldBeSetCorrectly() {
        assertTrue(tierDefinition.getActive());
        assertEquals("US", tierDefinition.getRegion());
        assertNotNull(tierDefinition.getCreatedAt());
        assertNotNull(tierDefinition.getUpdatedAt());
        assertTrue(tierDefinition.getCanDowngrade());
    }
    
    @Test
    void getActive_WhenNull_ShouldReturnTrue() {
        tierDefinition.setActive(null);
        assertTrue(tierDefinition.getActive());
    }
    
    @Test
    void getCanDowngrade_WhenNull_ShouldReturnTrue() {
        tierDefinition.setCanDowngrade(null);
        assertTrue(tierDefinition.getCanDowngrade());
    }
    
    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        String tierId = "free-us-2025";
        String tierName = "FREE";
        String displayName = "Free Tier";
        String description = "Basic free tier with limited features";
        Integer maxBookmarks = 5;
        Integer maxSeatmapCalls = 10;
        BigDecimal price = BigDecimal.ZERO;
        String billingType = "free";
        Boolean canDowngrade = false;
        String region = "EU";
        Boolean active = false;
        
        tierDefinition.setTierId(tierId);
        tierDefinition.setTierName(tierName);
        tierDefinition.setDisplayName(displayName);
        tierDefinition.setDescription(description);
        tierDefinition.setMaxBookmarks(maxBookmarks);
        tierDefinition.setMaxSeatmapCalls(maxSeatmapCalls);
        tierDefinition.setPriceUsd(price);
        tierDefinition.setBillingType(billingType);
        tierDefinition.setCanDowngrade(canDowngrade);
        tierDefinition.setRegion(region);
        tierDefinition.setActive(active);
        
        assertEquals(tierId, tierDefinition.getTierId());
        assertEquals(tierName, tierDefinition.getTierName());
        assertEquals(displayName, tierDefinition.getDisplayName());
        assertEquals(description, tierDefinition.getDescription());
        assertEquals(maxBookmarks, tierDefinition.getMaxBookmarks());
        assertEquals(maxSeatmapCalls, tierDefinition.getMaxSeatmapCalls());
        assertEquals(price, tierDefinition.getPriceUsd());
        assertEquals(billingType, tierDefinition.getBillingType());
        assertEquals(canDowngrade, tierDefinition.getCanDowngrade());
        assertEquals(region, tierDefinition.getRegion());
        assertEquals(active, tierDefinition.getActive());
    }
    
    @Test
    void updateTimestamp_ShouldUpdateUpdatedAt() {
        Instant originalTime = tierDefinition.getUpdatedAt();
        
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        
        tierDefinition.updateTimestamp();
        
        assertTrue(tierDefinition.getUpdatedAt().isAfter(originalTime));
    }
    
    @Test
    void freeTierDefinition_ShouldBeValid() {
        tierDefinition.setTierId("free-us-2025");
        tierDefinition.setTierName("FREE");
        tierDefinition.setDisplayName("Free Tier");
        tierDefinition.setDescription("Basic free tier with limited features");
        tierDefinition.setMaxBookmarks(5);
        tierDefinition.setMaxSeatmapCalls(10);
        tierDefinition.setPriceUsd(BigDecimal.ZERO);
        tierDefinition.setBillingType("free");
        tierDefinition.setCanDowngrade(false);
        
        assertEquals("FREE", tierDefinition.getTierName());
        assertEquals(BigDecimal.ZERO, tierDefinition.getPriceUsd());
        assertFalse(tierDefinition.getCanDowngrade());
        assertEquals(Integer.valueOf(5), tierDefinition.getMaxBookmarks());
        assertEquals(Integer.valueOf(10), tierDefinition.getMaxSeatmapCalls());
    }
    
    @Test
    void proTierDefinition_ShouldBeValid() {
        tierDefinition.setTierId("pro-us-2025");
        tierDefinition.setTierName("PRO");
        tierDefinition.setDisplayName("Pro Tier");
        tierDefinition.setDescription("Enhanced tier with more features");
        tierDefinition.setMaxBookmarks(50);
        tierDefinition.setMaxSeatmapCalls(500);
        tierDefinition.setPriceUsd(new BigDecimal("9.99"));
        tierDefinition.setBillingType("monthly");
        tierDefinition.setCanDowngrade(true);
        
        assertEquals("PRO", tierDefinition.getTierName());
        assertEquals(new BigDecimal("9.99"), tierDefinition.getPriceUsd());
        assertTrue(tierDefinition.getCanDowngrade());
        assertEquals(Integer.valueOf(50), tierDefinition.getMaxBookmarks());
        assertEquals(Integer.valueOf(500), tierDefinition.getMaxSeatmapCalls());
    }
    
    @Test
    void businessTierDefinition_ShouldBeValid() {
        tierDefinition.setTierId("business-us-2025");
        tierDefinition.setTierName("BUSINESS");
        tierDefinition.setDisplayName("Business Tier");
        tierDefinition.setDescription("Premium tier with unlimited features");
        tierDefinition.setMaxBookmarks(-1); // Unlimited
        tierDefinition.setMaxSeatmapCalls(-1); // Unlimited
        tierDefinition.setPriceUsd(new BigDecimal("199.99"));
        tierDefinition.setBillingType("one_time");
        tierDefinition.setCanDowngrade(false);
        
        assertEquals("BUSINESS", tierDefinition.getTierName());
        assertEquals(new BigDecimal("199.99"), tierDefinition.getPriceUsd());
        assertFalse(tierDefinition.getCanDowngrade());
        assertEquals(Integer.valueOf(-1), tierDefinition.getMaxBookmarks());
        assertEquals(Integer.valueOf(-1), tierDefinition.getMaxSeatmapCalls());
        assertEquals("one_time", tierDefinition.getBillingType());
    }
    
    @Test
    void regionalTierDefinitions_ShouldWorkCorrectly() {
        // US Tier
        TierDefinition usTier = new TierDefinition();
        usTier.setTierName("PRO");
        usTier.setRegion("US");
        usTier.setPriceUsd(new BigDecimal("9.99"));
        
        // EU Tier (different pricing)
        TierDefinition euTier = new TierDefinition();
        euTier.setTierName("PRO");
        euTier.setRegion("EU");
        euTier.setPriceUsd(new BigDecimal("8.99"));
        
        assertEquals("US", usTier.getRegion());
        assertEquals("EU", euTier.getRegion());
        assertEquals(new BigDecimal("9.99"), usTier.getPriceUsd());
        assertEquals(new BigDecimal("8.99"), euTier.getPriceUsd());
    }
}