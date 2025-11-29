package com.seatmap.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.*;

/**
 * Debug test to simulate the exact DynamoDB workflow and identify where the Instant fields are lost
 */
public class DynamoDbWorkflowDebugTest {
    
    private final ObjectMapper objectMapper;
    
    public DynamoDbWorkflowDebugTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }
    
    @Test
    void simulateExactDynamoDbWorkflow() throws Exception {
        // Step 1: Create bookmark with Instant fields (like the working ones)
        Bookmark bookmark = createTestBookmark();
        
        System.out.println("=== ORIGINAL BOOKMARK ===");
        printInstantFields("ORIGINAL", bookmark);
        
        // Step 2: Simulate toAttributeValueMap() - exactly as DynamoDbRepository does it
        Map<String, AttributeValue> attributeMap = simulateToAttributeValueMap(bookmark);
        
        System.out.println("\n=== DYNAMODB ATTRIBUTE MAP ===");
        printDynamoDbAttributes(attributeMap);
        
        // Step 3: Simulate fromAttributeValueMap() - exactly as DynamoDbRepository does it
        Bookmark deserializedBookmark = simulateFromAttributeValueMap(attributeMap);
        
        System.out.println("\n=== DESERIALIZED BOOKMARK ===");
        printInstantFields("DESERIALIZED", deserializedBookmark);
        
        // Step 4: Compare what we have vs what we expect
        compareInstantFields(bookmark, deserializedBookmark);
    }
    
    private Bookmark createTestBookmark() {
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("DynamoDB Debug Test");
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        
        // Set the working top-level Instant fields
        Instant now = Instant.now();
        bookmark.setCreatedAt(now);
        bookmark.setUpdatedAt(now.plusSeconds(10));
        bookmark.setExpiresAt(now.plusSeconds(86400));
        bookmark.setLastAccessedAt(now.plusSeconds(20));
        
        // Set the problematic nested AlertConfig Instant fields
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(5.0);
        alertConfig.setLastEvaluated(now.plusSeconds(30));
        alertConfig.setLastTriggered(now.plusSeconds(40));
        alertConfig.setTriggerHistory("{\"test\":\"data\"}");
        bookmark.setAlertConfig(alertConfig);
        
        return bookmark;
    }
    
    // Copy of DynamoDbRepository.toAttributeValueMap()
    private Map<String, AttributeValue> simulateToAttributeValueMap(Bookmark entity) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(entity);
        System.out.println("Step 1 - Entity to JSON: " + json.substring(0, Math.min(200, json.length())) + "...");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        System.out.println("Step 2 - JSON to Map completed");
        
        return convertToAttributeValueMap(map);
    }
    
    // Copy of DynamoDbRepository.fromAttributeValueMap() 
    private Bookmark simulateFromAttributeValueMap(Map<String, AttributeValue> attributeMap) throws JsonProcessingException {
        Map<String, Object> map = convertFromAttributeValueMap(attributeMap);
        String json = objectMapper.writeValueAsString(map);
        System.out.println("Step 3 - AttributeValue to JSON: " + json.substring(0, Math.min(200, json.length())) + "...");
        
        // Apply the cleanup regex from DynamoDbRepository.java line 52
        json = json.replaceAll("\"(lastEvaluated|lastTriggered|triggerHistory)\":\\{\\}", "\"$1\":null");
        System.out.println("Step 4 - After regex cleanup: " + json.substring(0, Math.min(200, json.length())) + "...");
        
        return objectMapper.readValue(json, Bookmark.class);
    }
    
    // Copy of DynamoDbRepository.convertToAttributeValueMap()
    private Map<String, AttributeValue> convertToAttributeValueMap(Map<String, Object> map) {
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                attributeMap.put(entry.getKey(), toAttributeValue(entry.getValue()));
            }
        }
        
        return attributeMap;
    }
    
    // Copy of DynamoDbRepository.toAttributeValue()
    private AttributeValue toAttributeValue(Object value) {
        if (value == null) {
            return AttributeValue.builder().nul(true).build();
        } else if (value instanceof String) {
            return AttributeValue.builder().s((String) value).build();
        } else if (value instanceof Number) {
            return AttributeValue.builder().n(value.toString()).build();
        } else if (value instanceof Boolean) {
            return AttributeValue.builder().bool((Boolean) value).build();
        } else if (value instanceof List) {
            List<AttributeValue> list = new ArrayList<>();
            for (Object item : (List<?>) value) {
                list.add(toAttributeValue(item));
            }
            return AttributeValue.builder().l(list).build();
        } else if (value instanceof Map) {
            Map<String, AttributeValue> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                map.put(entry.getKey().toString(), toAttributeValue(entry.getValue()));
            }
            return AttributeValue.builder().m(map).build();
        } else {
            return AttributeValue.builder().s(value.toString()).build();
        }
    }
    
    // Copy of DynamoDbRepository.convertFromAttributeValueMap()
    private Map<String, Object> convertFromAttributeValueMap(Map<String, AttributeValue> attributeMap) {
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            map.put(entry.getKey(), fromAttributeValue(entry.getValue()));
        }
        
        return map;
    }
    
    // Copy of DynamoDbRepository.fromAttributeValue()
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
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : attributeValue.m().entrySet()) {
                Object value = fromAttributeValue(entry.getValue());
                map.put(entry.getKey(), value);
            }
            return map;
        } else if (attributeValue.l() != null) {
            List<Object> list = new ArrayList<>();
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
    
    private void printInstantFields(String label, Bookmark bookmark) {
        System.out.println(label + " - Top-level createdAt: " + bookmark.getCreatedAt());
        System.out.println(label + " - Top-level updatedAt: " + bookmark.getUpdatedAt());
        System.out.println(label + " - Top-level expiresAt: " + bookmark.getExpiresAt());
        System.out.println(label + " - Top-level lastAccessedAt: " + bookmark.getLastAccessedAt());
        
        if (bookmark.getAlertConfig() != null) {
            System.out.println(label + " - Nested lastEvaluated: " + bookmark.getAlertConfig().getLastEvaluated());
            System.out.println(label + " - Nested lastTriggered: " + bookmark.getAlertConfig().getLastTriggered());
        } else {
            System.out.println(label + " - AlertConfig is NULL!");
        }
    }
    
    private void printDynamoDbAttributes(Map<String, AttributeValue> attributeMap) {
        System.out.println("Top-level createdAt: " + attributeMap.get("createdAt"));
        System.out.println("Top-level updatedAt: " + attributeMap.get("updatedAt"));
        
        AttributeValue alertConfigAttr = attributeMap.get("alertConfig");
        if (alertConfigAttr != null && alertConfigAttr.m() != null) {
            Map<String, AttributeValue> alertConfigMap = alertConfigAttr.m();
            System.out.println("Nested lastEvaluated: " + alertConfigMap.get("lastEvaluated"));
            System.out.println("Nested lastTriggered: " + alertConfigMap.get("lastTriggered"));
        }
    }
    
    private void compareInstantFields(Bookmark original, Bookmark deserialized) {
        System.out.println("\n=== FIELD COMPARISON ===");
        
        // Top-level fields
        boolean createdAtOk = Objects.equals(original.getCreatedAt().getEpochSecond(), 
                                           deserialized.getCreatedAt().getEpochSecond());
        System.out.println("createdAt preserved: " + createdAtOk);
        
        boolean updatedAtOk = Objects.equals(original.getUpdatedAt().getEpochSecond(), 
                                           deserialized.getUpdatedAt().getEpochSecond());
        System.out.println("updatedAt preserved: " + updatedAtOk);
        
        // Nested fields
        if (original.getAlertConfig() != null && deserialized.getAlertConfig() != null) {
            boolean lastEvaluatedOk = Objects.equals(
                original.getAlertConfig().getLastEvaluated().getEpochSecond(),
                deserialized.getAlertConfig().getLastEvaluated().getEpochSecond());
            System.out.println("lastEvaluated preserved: " + lastEvaluatedOk);
            
            boolean lastTriggeredOk = Objects.equals(
                original.getAlertConfig().getLastTriggered().getEpochSecond(),
                deserialized.getAlertConfig().getLastTriggered().getEpochSecond());
            System.out.println("lastTriggered preserved: " + lastTriggeredOk);
        } else {
            System.out.println("AlertConfig comparison failed - one or both are null");
            System.out.println("Original AlertConfig: " + original.getAlertConfig());
            System.out.println("Deserialized AlertConfig: " + deserialized.getAlertConfig());
        }
    }
}