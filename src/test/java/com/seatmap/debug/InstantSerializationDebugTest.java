package com.seatmap.debug;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Debug test to understand why top-level Instant fields work but nested AlertConfig Instant fields don't
 */
public class InstantSerializationDebugTest {
    
    @Test
    void debugInstantSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Create a bookmark with both top-level and nested Instant fields
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("Debug Test");
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        
        // Set top-level Instant fields (these work correctly)
        Instant now = Instant.now();
        bookmark.setCreatedAt(now);
        bookmark.setUpdatedAt(now);
        bookmark.setExpiresAt(now.plusSeconds(86400));
        bookmark.setLastAccessedAt(now);
        
        // Set nested AlertConfig Instant fields (these have issues)
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(5.0);
        alertConfig.updateLastEvaluated();
        alertConfig.recordTrigger();
        bookmark.setAlertConfig(alertConfig);
        
        System.out.println("=== BEFORE SERIALIZATION ===");
        System.out.println("Top-level createdAt: " + bookmark.getCreatedAt());
        System.out.println("Top-level updatedAt: " + bookmark.getUpdatedAt());
        System.out.println("Nested lastEvaluated: " + bookmark.getAlertConfig().getLastEvaluated());
        System.out.println("Nested lastTriggered: " + bookmark.getAlertConfig().getLastTriggered());
        
        // Serialize to JSON
        String json = mapper.writeValueAsString(bookmark);
        System.out.println("\n=== JSON REPRESENTATION ===");
        System.out.println(json);
        
        // Check what happens when we parse JSON back to Map (simulating DynamoDB conversion)
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> map = mapper.readValue(json, java.util.Map.class);
        
        System.out.println("\n=== AFTER JSON -> MAP CONVERSION ===");
        System.out.println("Top-level createdAt type: " + map.get("createdAt").getClass());
        System.out.println("Top-level createdAt value: " + map.get("createdAt"));
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> alertConfigMap = (java.util.Map<String, Object>) map.get("alertConfig");
        System.out.println("Nested lastEvaluated type: " + alertConfigMap.get("lastEvaluated").getClass());
        System.out.println("Nested lastEvaluated value: " + alertConfigMap.get("lastEvaluated"));
        
        // Now deserialize back to Bookmark
        String mapBackToJson = mapper.writeValueAsString(map);
        System.out.println("\n=== MAP -> JSON ===");
        System.out.println(mapBackToJson);
        
        Bookmark deserializedBookmark = mapper.readValue(mapBackToJson, Bookmark.class);
        
        System.out.println("\n=== AFTER FULL ROUND-TRIP ===");
        System.out.println("Top-level createdAt: " + deserializedBookmark.getCreatedAt());
        System.out.println("Top-level updatedAt: " + deserializedBookmark.getUpdatedAt());
        System.out.println("Nested lastEvaluated: " + 
            (deserializedBookmark.getAlertConfig() != null ? deserializedBookmark.getAlertConfig().getLastEvaluated() : "NULL"));
        System.out.println("Nested lastTriggered: " + 
            (deserializedBookmark.getAlertConfig() != null ? deserializedBookmark.getAlertConfig().getLastTriggered() : "NULL"));
    }
}