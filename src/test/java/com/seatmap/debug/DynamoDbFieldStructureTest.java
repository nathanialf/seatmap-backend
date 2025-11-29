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
 * Test to examine the exact DynamoDB field structure differences between 
 * top-level Instant fields vs nested AlertConfig Instant fields
 */
public class DynamoDbFieldStructureTest {
    
    private final ObjectMapper objectMapper;
    
    public DynamoDbFieldStructureTest() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    }
    
    @Test
    void examineExactDynamoDbFieldStructure() throws Exception {
        // Create bookmark with both types of Instant fields
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("DynamoDB Structure Test");
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        
        // Set specific timestamps for comparison
        Instant baseTime = Instant.parse("2024-01-15T10:30:45.123456789Z");
        
        // Top-level Instant fields (these work)
        bookmark.setCreatedAt(baseTime);
        bookmark.setUpdatedAt(baseTime.plusSeconds(60));
        bookmark.setExpiresAt(baseTime.plusSeconds(86400));
        bookmark.setLastAccessedAt(baseTime.plusSeconds(120));
        
        // Nested AlertConfig Instant fields (these have issues)
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(5.0);
        alertConfig.setLastEvaluated(baseTime.plusSeconds(180));
        alertConfig.setLastTriggered(baseTime.plusSeconds(240));
        alertConfig.setTriggerHistory("{\"test\":\"data\"}");
        bookmark.setAlertConfig(alertConfig);
        
        // Convert to DynamoDB AttributeValue structure
        Map<String, AttributeValue> dynamoDbItem = toAttributeValueMap(bookmark);
        
        System.out.println("=== DYNAMODB ITEM STRUCTURE ===");
        
        // Examine top-level fields
        System.out.println("\n--- TOP-LEVEL INSTANT FIELDS (Working) ---");
        printAttributeDetails("createdAt", dynamoDbItem.get("createdAt"));
        printAttributeDetails("updatedAt", dynamoDbItem.get("updatedAt"));
        printAttributeDetails("expiresAt", dynamoDbItem.get("expiresAt"));
        printAttributeDetails("lastAccessedAt", dynamoDbItem.get("lastAccessedAt"));
        
        // Examine AlertConfig structure
        System.out.println("\n--- ALERT CONFIG STRUCTURE ---");
        AttributeValue alertConfigAttr = dynamoDbItem.get("alertConfig");
        if (alertConfigAttr != null && alertConfigAttr.m() != null) {
            Map<String, AttributeValue> alertConfigMap = alertConfigAttr.m();
            
            System.out.println("AlertConfig type: " + alertConfigAttr.type());
            System.out.println("AlertConfig keys: " + alertConfigMap.keySet());
            
            System.out.println("\n--- NESTED INSTANT FIELDS (Problematic) ---");
            printAttributeDetails("lastEvaluated", alertConfigMap.get("lastEvaluated"));
            printAttributeDetails("lastTriggered", alertConfigMap.get("lastTriggered"));
            
            System.out.println("\n--- OTHER ALERT CONFIG FIELDS ---");
            printAttributeDetails("alertThreshold", alertConfigMap.get("alertThreshold"));
            printAttributeDetails("triggerHistory", alertConfigMap.get("triggerHistory"));
        }
        
        // Show the raw JSON to see what gets serialized
        System.out.println("\n=== RAW JSON SERIALIZATION ===");
        String json = objectMapper.writeValueAsString(bookmark);
        System.out.println(json);
        
        // Check if there are any differences in the intermediate Map representation
        System.out.println("\n=== INTERMEDIATE MAP STRUCTURE ===");
        @SuppressWarnings("unchecked")
        Map<String, Object> intermediateMap = objectMapper.readValue(json, Map.class);
        
        System.out.println("Top-level createdAt type: " + intermediateMap.get("createdAt").getClass());
        System.out.println("Top-level createdAt value: " + intermediateMap.get("createdAt"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> alertConfigObj = (Map<String, Object>) intermediateMap.get("alertConfig");
        if (alertConfigObj != null) {
            Object lastEval = alertConfigObj.get("lastEvaluated");
            Object lastTrig = alertConfigObj.get("lastTriggered");
            
            System.out.println("Nested lastEvaluated type: " + (lastEval != null ? lastEval.getClass() : "null"));
            System.out.println("Nested lastEvaluated value: " + lastEval);
            System.out.println("Nested lastTriggered type: " + (lastTrig != null ? lastTrig.getClass() : "null"));
            System.out.println("Nested lastTriggered value: " + lastTrig);
        }
    }
    
    @Test
    void compareFieldStorageTypes() throws Exception {
        // Create a simple object with just the timestamp fields to see storage differences
        System.out.println("=== FIELD STORAGE TYPE COMPARISON ===");
        
        Instant testTime = Instant.now();
        
        // Test top-level Instant field
        Map<String, Object> topLevelMap = new HashMap<>();
        topLevelMap.put("timestamp", testTime);
        
        String topLevelJson = objectMapper.writeValueAsString(topLevelMap);
        System.out.println("Top-level JSON: " + topLevelJson);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedTopLevel = objectMapper.readValue(topLevelJson, Map.class);
        Object topLevelValue = deserializedTopLevel.get("timestamp");
        System.out.println("Top-level deserialized type: " + topLevelValue.getClass());
        System.out.println("Top-level deserialized value: " + topLevelValue);
        
        // Test nested Instant field
        Map<String, Object> nestedConfig = new HashMap<>();
        nestedConfig.put("timestamp", testTime);
        
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("config", nestedConfig);
        
        String nestedJson = objectMapper.writeValueAsString(nestedMap);
        System.out.println("\nNested JSON: " + nestedJson);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> deserializedNested = objectMapper.readValue(nestedJson, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedConfigResult = (Map<String, Object>) deserializedNested.get("config");
        Object nestedValue = nestedConfigResult.get("timestamp");
        System.out.println("Nested deserialized type: " + nestedValue.getClass());
        System.out.println("Nested deserialized value: " + nestedValue);
        
        // Convert both to DynamoDB format
        AttributeValue topLevelAttr = toAttributeValue(topLevelValue);
        AttributeValue nestedAttr = toAttributeValue(nestedValue);
        
        System.out.println("\nTop-level DynamoDB type: " + topLevelAttr.type());
        System.out.println("Top-level DynamoDB value: " + (topLevelAttr.n() != null ? topLevelAttr.n() : topLevelAttr.s()));
        
        System.out.println("Nested DynamoDB type: " + nestedAttr.type());
        System.out.println("Nested DynamoDB value: " + (nestedAttr.n() != null ? nestedAttr.n() : nestedAttr.s()));
    }
    
    // Helper methods copied from DynamoDbRepository
    private Map<String, AttributeValue> toAttributeValueMap(Bookmark entity) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(entity);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);
        return convertToAttributeValueMap(map);
    }
    
    private Map<String, AttributeValue> convertToAttributeValueMap(Map<String, Object> map) {
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                attributeMap.put(entry.getKey(), toAttributeValue(entry.getValue()));
            }
        }
        
        return attributeMap;
    }
    
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
    
    private void printAttributeDetails(String fieldName, AttributeValue attr) {
        if (attr == null) {
            System.out.println(fieldName + ": NULL");
            return;
        }
        
        System.out.println(fieldName + ":");
        System.out.println("  Type: " + attr.type());
        
        if (attr.s() != null) {
            System.out.println("  String value: " + attr.s());
        } else if (attr.n() != null) {
            System.out.println("  Number value: " + attr.n());
        } else if (attr.bool() != null) {
            System.out.println("  Boolean value: " + attr.bool());
        } else if (attr.nul() != null) {
            System.out.println("  Null value: " + attr.nul());
        } else if (attr.m() != null) {
            System.out.println("  Map with keys: " + attr.m().keySet());
        } else if (attr.l() != null) {
            System.out.println("  List with " + attr.l().size() + " items");
        }
    }
}