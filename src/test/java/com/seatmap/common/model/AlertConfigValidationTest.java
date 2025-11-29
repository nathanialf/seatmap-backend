package com.seatmap.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.Bookmark.AlertConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for AlertConfig validation focusing on field serialization,
 * deserialization, and the lastEvaluated null value issue.
 */
public class AlertConfigValidationTest {
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Test
    void testThresholdValidation_KnownWorkingField() {
        AlertConfig config = new AlertConfig();
        
        // Test null threshold (alert disabled)
        assertNull(config.getAlertThreshold());
        assertFalse(config.isEnabled());
        
        // Test valid threshold (alert enabled)
        config.setAlertThreshold(5.0);
        assertEquals(5.0, config.getAlertThreshold());
        assertTrue(config.isEnabled());
        
        // Test zero threshold
        config.setAlertThreshold(0.0);
        assertEquals(0.0, config.getAlertThreshold());
        assertTrue(config.isEnabled());
        
        // Test negative threshold
        config.setAlertThreshold(-1.0);
        assertEquals(-1.0, config.getAlertThreshold());
        assertTrue(config.isEnabled());
    }
    
    @Test
    void testLastEvaluatedFieldValidation() {
        AlertConfig config = new AlertConfig();
        
        // Test initial state - should be null
        assertNull(config.getLastEvaluated());
        
        // Test setting a timestamp
        Instant now = Instant.now();
        config.setLastEvaluated(now);
        assertEquals(now, config.getLastEvaluated());
        
        // Test updateLastEvaluated method
        config.updateLastEvaluated();
        assertNotNull(config.getLastEvaluated());
        
        // Should be very close to current time (within 1 second)
        Instant currentTime = Instant.now();
        long secondsDiff = ChronoUnit.SECONDS.between(config.getLastEvaluated(), currentTime);
        assertTrue(Math.abs(secondsDiff) <= 1, 
            "updateLastEvaluated should set time to current instant, but was off by " + secondsDiff + " seconds");
    }
    
    @Test
    void testLastTriggeredFieldValidation() {
        AlertConfig config = new AlertConfig();
        
        // Test initial state - should be null
        assertNull(config.getLastTriggered());
        
        // Test setting a timestamp
        Instant triggerTime = Instant.now().minusSeconds(3600); // 1 hour ago
        config.setLastTriggered(triggerTime);
        assertEquals(triggerTime, config.getLastTriggered());
        
        // Test recordTrigger method
        config.recordTrigger();
        assertNotNull(config.getLastTriggered());
        
        // Should be very close to current time (within 1 second)
        Instant currentTime = Instant.now();
        long secondsDiff = ChronoUnit.SECONDS.between(config.getLastTriggered(), currentTime);
        assertTrue(Math.abs(secondsDiff) <= 1,
            "recordTrigger should set time to current instant, but was off by " + secondsDiff + " seconds");
    }
    
    @Test
    void testTriggerHistoryValidation() {
        AlertConfig config = new AlertConfig();
        
        // Test initial state - should be null
        assertNull(config.getTriggerHistory());
        
        // Test setting JSON trigger history
        String triggerHistory = "{\"triggers\":[{\"timestamp\":\"2024-01-01T00:00:00Z\",\"reason\":\"threshold_exceeded\"}]}";
        config.setTriggerHistory(triggerHistory);
        assertEquals(triggerHistory, config.getTriggerHistory());
        
        // Test empty string
        config.setTriggerHistory("");
        assertEquals("", config.getTriggerHistory());
        
        // Test null
        config.setTriggerHistory(null);
        assertNull(config.getTriggerHistory());
    }
    
    @Test
    void testAlertConfigJsonSerialization() throws Exception {
        AlertConfig config = new AlertConfig();
        config.setAlertThreshold(5.5);
        config.updateLastEvaluated();
        config.recordTrigger();
        config.setTriggerHistory("{\"test\":\"data\"}");
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(config);
        assertNotNull(json);
        assertFalse(json.isEmpty());
        
        // Should contain all fields
        assertTrue(json.contains("alertThreshold"));
        assertTrue(json.contains("lastEvaluated"));
        assertTrue(json.contains("lastTriggered"));
        assertTrue(json.contains("triggerHistory"));
        
        // Deserialize back
        AlertConfig deserializedConfig = objectMapper.readValue(json, AlertConfig.class);
        assertEquals(config.getAlertThreshold(), deserializedConfig.getAlertThreshold());
        assertEquals(config.getLastEvaluated(), deserializedConfig.getLastEvaluated());
        assertEquals(config.getLastTriggered(), deserializedConfig.getLastTriggered());
        assertEquals(config.getTriggerHistory(), deserializedConfig.getTriggerHistory());
    }
    
    @Test
    void testAlertConfigJsonSerializationWithNullValues() throws Exception {
        AlertConfig config = new AlertConfig();
        config.setAlertThreshold(3.0); // Only threshold set, others null
        
        // Serialize to JSON
        String json = objectMapper.writeValueAsString(config);
        
        // Deserialize back
        AlertConfig deserializedConfig = objectMapper.readValue(json, AlertConfig.class);
        assertEquals(3.0, deserializedConfig.getAlertThreshold());
        assertNull(deserializedConfig.getLastEvaluated()); // This should NOT be null if there was a timestamp
        assertNull(deserializedConfig.getLastTriggered());
        assertNull(deserializedConfig.getTriggerHistory());
    }
    
    @Test
    void testAlertConfigNullValueHandling() throws Exception {
        // Test the specific issue: null lastEvaluated despite having a timestamp
        String jsonWithNullLastEvaluated = """
            {
                "alertThreshold": 5.0,
                "lastEvaluated": null,
                "lastTriggered": null,
                "triggerHistory": null
            }
            """;
            
        AlertConfig config = objectMapper.readValue(jsonWithNullLastEvaluated, AlertConfig.class);
        assertEquals(5.0, config.getAlertThreshold());
        assertNull(config.getLastEvaluated());
        assertNull(config.getLastTriggered());
        assertNull(config.getTriggerHistory());
        
        // Test with empty object (the problematic case from DynamoDB)
        String jsonWithEmptyObjects = """
            {
                "alertThreshold": 5.0,
                "lastEvaluated": {},
                "lastTriggered": {},
                "triggerHistory": {}
            }
            """;
            
        // This should fail or be handled correctly
        try {
            AlertConfig configFromEmpty = objectMapper.readValue(jsonWithEmptyObjects, AlertConfig.class);
            // If it doesn't fail, the empty objects should be converted to null
            assertNull(configFromEmpty.getLastEvaluated(), 
                "Empty object {} for lastEvaluated should be converted to null");
            assertNull(configFromEmpty.getLastTriggered(),
                "Empty object {} for lastTriggered should be converted to null");
            assertNull(configFromEmpty.getTriggerHistory(),
                "Empty object {} for triggerHistory should be converted to null");
        } catch (Exception e) {
            // This is expected if Jackson cannot handle empty objects for Instant fields
            assertTrue(e.getMessage().contains("Cannot deserialize") || 
                      e.getMessage().contains("not a valid representation"),
                      "Should get a meaningful Jackson deserialization error for empty objects");
        }
    }
    
    @Test
    void testAlertConfigTimestampPrecision() throws Exception {
        AlertConfig config = new AlertConfig();
        
        // Test with very precise timestamp
        Instant preciseTime = Instant.parse("2024-01-15T10:30:45.123456789Z");
        config.setLastEvaluated(preciseTime);
        
        String json = objectMapper.writeValueAsString(config);
        AlertConfig deserializedConfig = objectMapper.readValue(json, AlertConfig.class);
        
        // Jackson should preserve nanosecond precision
        assertEquals(preciseTime, deserializedConfig.getLastEvaluated());
    }
    
    @Test 
    void testAlertConfigConstructors() {
        // Test default constructor
        AlertConfig defaultConfig = new AlertConfig();
        assertNull(defaultConfig.getAlertThreshold());
        assertNull(defaultConfig.getLastEvaluated());
        assertNull(defaultConfig.getLastTriggered());
        assertNull(defaultConfig.getTriggerHistory());
        assertFalse(defaultConfig.isEnabled());
        
        // Test parameterized constructor
        AlertConfig thresholdConfig = new AlertConfig(7.5);
        assertEquals(7.5, thresholdConfig.getAlertThreshold());
        assertTrue(thresholdConfig.isEnabled());
        assertNull(thresholdConfig.getLastEvaluated());
        assertNull(thresholdConfig.getLastTriggered());
        assertNull(thresholdConfig.getTriggerHistory());
    }
    
    @Test
    void testAlertConfigIsEnabledLogic() {
        AlertConfig config = new AlertConfig();
        
        // Null threshold = disabled
        assertFalse(config.isEnabled());
        
        // Zero threshold = enabled
        config.setAlertThreshold(0.0);
        assertTrue(config.isEnabled());
        
        // Positive threshold = enabled
        config.setAlertThreshold(5.0);
        assertTrue(config.isEnabled());
        
        // Negative threshold = enabled
        config.setAlertThreshold(-1.0);
        assertTrue(config.isEnabled());
        
        // Back to null = disabled
        config.setAlertThreshold(null);
        assertFalse(config.isEnabled());
    }
    
    @Test
    void testAlertConfigUpdateMethods() {
        AlertConfig config = new AlertConfig();
        
        Instant beforeUpdate = Instant.now().minusMillis(100);
        
        // Test updateLastEvaluated
        config.updateLastEvaluated();
        Instant afterFirstUpdate = Instant.now().plusMillis(100);
        
        assertNotNull(config.getLastEvaluated());
        assertTrue(config.getLastEvaluated().isAfter(beforeUpdate));
        assertTrue(config.getLastEvaluated().isBefore(afterFirstUpdate));
        
        // Test recordTrigger
        Instant beforeTrigger = Instant.now().minusMillis(100);
        config.recordTrigger();
        Instant afterTrigger = Instant.now().plusMillis(100);
        
        assertNotNull(config.getLastTriggered());
        assertTrue(config.getLastTriggered().isAfter(beforeTrigger));
        assertTrue(config.getLastTriggered().isBefore(afterTrigger));
        
        // Both timestamps should be different (unless executed in same nanosecond)
        assertNotEquals(config.getLastEvaluated(), config.getLastTriggered());
    }
}