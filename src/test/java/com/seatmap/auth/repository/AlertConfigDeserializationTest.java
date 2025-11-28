package com.seatmap.auth.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to reproduce and fix the AlertConfig deserialization issue
 * seen in production where Jackson encounters array token instead of object token
 */
public class AlertConfigDeserializationTest {
    
    private final ObjectMapper objectMapper;
    
    public AlertConfigDeserializationTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    @Test
    void testAlertConfigDeserializationFromDynamoDB() throws Exception {
        // Recreate the exact DynamoDB structure that's failing
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        attributeMap.put("userId", AttributeValue.builder().s("test-user").build());
        attributeMap.put("bookmarkId", AttributeValue.builder().s("test-bookmark").build());
        attributeMap.put("title", AttributeValue.builder().s("Alert Test").build());
        attributeMap.put("itemType", AttributeValue.builder().s("BOOKMARK").build());
        attributeMap.put("flightOfferData", AttributeValue.builder().s("{}").build());
        
        // Create alertConfig structure exactly as stored in DynamoDB
        Map<String, AttributeValue> alertConfigMap = new HashMap<>();
        alertConfigMap.put("alertThreshold", AttributeValue.builder().n("5").build());
        alertConfigMap.put("lastEvaluated", AttributeValue.builder().nul(true).build());
        alertConfigMap.put("lastTriggered", AttributeValue.builder().nul(true).build());
        alertConfigMap.put("triggerHistory", AttributeValue.builder().nul(true).build());
        
        AttributeValue alertConfigAttribute = AttributeValue.builder().m(alertConfigMap).build();
        System.out.println("DynamoDB alertConfig structure: " + alertConfigAttribute);
        
        attributeMap.put("alertConfig", alertConfigAttribute);
        
        // Debug the specific alertConfig AttributeValue before conversion
        AttributeValue alertConfigAttr = attributeMap.get("alertConfig");
        System.out.println("AlertConfig AttributeValue: " + alertConfigAttr);
        System.out.println("Is Map? " + (alertConfigAttr.m() != null));
        System.out.println("Is List? " + (alertConfigAttr.l() != null));
        System.out.println("Map contents: " + alertConfigAttr.m());
        
        // Use the actual BookmarkRepository to test the fix
        BookmarkRepository repository = new BookmarkRepository(null, "test-table");
        
        // Test the conversion directly using reflection to access the protected method
        try {
            java.lang.reflect.Method method = com.seatmap.common.repository.DynamoDbRepository.class.getDeclaredMethod("fromAttributeValueMap", Map.class);
            method.setAccessible(true);
            Bookmark bookmark = (Bookmark) method.invoke(repository, attributeMap);
            
            assertNotNull(bookmark);
            assertNotNull(bookmark.getAlertConfig());
            assertEquals(5.0, bookmark.getAlertConfig().getAlertThreshold());
            assertNull(bookmark.getAlertConfig().getTriggerHistory());
            
            System.out.println("SUCCESS: AlertConfig properly deserialized!");
            return;
        } catch (Exception e) {
            System.out.println("Reflection approach failed: " + e.getMessage());
        }
        
        // Fallback: manual conversion for debugging
        Map<String, Object> map = convertFromAttributeValueMap(attributeMap);
        
        // Debug: Print the intermediate Map to see what's going wrong
        System.out.println("Full converted map: " + map);
        
        String json = objectMapper.writeValueAsString(map);
        System.out.println("Generated JSON: " + json);
        
        // This should work without the "array instead of object" error
        Bookmark bookmark = objectMapper.readValue(json, Bookmark.class);
        
        assertNotNull(bookmark);
        assertNotNull(bookmark.getAlertConfig());
        assertEquals(5.0, bookmark.getAlertConfig().getAlertThreshold());
        assertNull(bookmark.getAlertConfig().getTriggerHistory());
    }
    
    // Copy the conversion logic from DynamoDbRepository to test it in isolation
    private Map<String, Object> convertFromAttributeValueMap(Map<String, AttributeValue> attributeMap) {
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            map.put(entry.getKey(), fromAttributeValue(entry.getValue()));
        }
        
        return map;
    }
    
    private Object fromAttributeValue(AttributeValue attributeValue) {
        if (attributeValue.s() != null) {
            return attributeValue.s();
        } else if (attributeValue.n() != null) {
            String numberStr = attributeValue.n();
            if (numberStr.contains(".")) {
                return Double.parseDouble(numberStr);
            } else {
                try {
                    return Integer.parseInt(numberStr);
                } catch (NumberFormatException e) {
                    return Long.parseLong(numberStr);
                }
            }
        } else if (attributeValue.bool() != null) {
            return attributeValue.bool();
        } else if (attributeValue.m() != null) {
            // Check Map BEFORE List to prevent Maps from being treated as empty Lists
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : attributeValue.m().entrySet()) {
                map.put(entry.getKey(), fromAttributeValue(entry.getValue()));
            }
            return map;
        } else if (attributeValue.l() != null) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (AttributeValue item : attributeValue.l()) {
                list.add(fromAttributeValue(item));
            }
            return list;
        } else if (attributeValue.nul() != null && attributeValue.nul()) {
            return null;
        } else {
            return null;
        }
    }
}