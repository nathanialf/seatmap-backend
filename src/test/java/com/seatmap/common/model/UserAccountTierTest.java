package com.seatmap.common.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserAccountTierTest {
    
    private User user;
    
    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
    }
    
    @Test
    void constructor_DefaultAccountTier_ShouldBeFree() {
        assertEquals(User.AccountTier.FREE, user.getAccountTier());
    }
    
    @Test
    void getAccountTier_WhenNull_ShouldReturnFree() {
        user.setAccountTier(null);
        assertEquals(User.AccountTier.FREE, user.getAccountTier());
    }
    
    @Test
    void setAccountTier_Pro_ShouldSetCorrectly() {
        user.setAccountTier(User.AccountTier.PRO);
        assertEquals(User.AccountTier.PRO, user.getAccountTier());
    }
    
    @Test
    void setAccountTier_Business_ShouldSetCorrectly() {
        user.setAccountTier(User.AccountTier.BUSINESS);
        assertEquals(User.AccountTier.BUSINESS, user.getAccountTier());
    }
    
    @Test
    void setAccountTier_Dev_ShouldSetCorrectly() {
        user.setAccountTier(User.AccountTier.DEV);
        assertEquals(User.AccountTier.DEV, user.getAccountTier());
    }
    
    @Test
    void accountTierEnum_ShouldHaveCorrectValues() {
        assertEquals(4, User.AccountTier.values().length);
        assertEquals("FREE", User.AccountTier.FREE.name());
        assertEquals("PRO", User.AccountTier.PRO.name());
        assertEquals("BUSINESS", User.AccountTier.BUSINESS.name());
        assertEquals("DEV", User.AccountTier.DEV.name());
    }
    
    @Test
    void accountTierEnum_ValueOf_ShouldWork() {
        assertEquals(User.AccountTier.FREE, User.AccountTier.valueOf("FREE"));
        assertEquals(User.AccountTier.PRO, User.AccountTier.valueOf("PRO"));
        assertEquals(User.AccountTier.BUSINESS, User.AccountTier.valueOf("BUSINESS"));
        assertEquals(User.AccountTier.DEV, User.AccountTier.valueOf("DEV"));
    }
    
    @Test
    void accountTierEnum_ToString_ShouldReturnCorrectValues() {
        assertEquals("FREE", User.AccountTier.FREE.toString());
        assertEquals("PRO", User.AccountTier.PRO.toString());
        assertEquals("BUSINESS", User.AccountTier.BUSINESS.toString());
        assertEquals("DEV", User.AccountTier.DEV.toString());
    }
    
    @Test
    void userCreation_WithAccountTier_ShouldMaintainTier() {
        user.setAccountTier(User.AccountTier.PRO);
        user.updateTimestamp();
        
        assertEquals(User.AccountTier.PRO, user.getAccountTier());
        assertNotNull(user.getUpdatedAt());
    }
    
    @Test
    void userCreation_AllTierTransitions_ShouldWork() {
        // Start with FREE (default)
        assertEquals(User.AccountTier.FREE, user.getAccountTier());
        
        // Upgrade to PRO
        user.setAccountTier(User.AccountTier.PRO);
        assertEquals(User.AccountTier.PRO, user.getAccountTier());
        
        // Upgrade to BUSINESS
        user.setAccountTier(User.AccountTier.BUSINESS);
        assertEquals(User.AccountTier.BUSINESS, user.getAccountTier());
        
        // Downgrade to PRO (business logic will prevent this, but model allows it)
        user.setAccountTier(User.AccountTier.PRO);
        assertEquals(User.AccountTier.PRO, user.getAccountTier());
        
        // Downgrade to FREE
        user.setAccountTier(User.AccountTier.FREE);
        assertEquals(User.AccountTier.FREE, user.getAccountTier());
    }
}