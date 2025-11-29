package com.seatmap.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.Bookmark;
import org.junit.jupiter.api.Test;

import java.time.Instant;

/**
 * Debug test to check if the issue is in API response serialization
 */
public class ApiResponseSerializationDebugTest {
    
    @Test
    void testApiResponseSerialization() throws Exception {
        // Create ObjectMapper exactly like BookmarkHandler does
        ObjectMapper apiObjectMapper = new ObjectMapper();
        apiObjectMapper.registerModule(new JavaTimeModule());
        
        // Create a bookmark with AlertConfig that has timestamps
        Bookmark bookmark = new Bookmark();
        bookmark.setUserId("test-user");
        bookmark.setBookmarkId("test-bookmark");
        bookmark.setTitle("API Response Debug Test");
        bookmark.setItemType(Bookmark.ItemType.SAVED_SEARCH);
        
        // Set top-level timestamps (these work correctly)
        Instant now = Instant.now();
        bookmark.setCreatedAt(now);
        bookmark.setUpdatedAt(now);
        bookmark.setExpiresAt(now.plusSeconds(86400));
        bookmark.setLastAccessedAt(now);
        
        // Set AlertConfig with timestamps (these reportedly don't work)
        Bookmark.AlertConfig alertConfig = new Bookmark.AlertConfig(5.0);
        alertConfig.updateLastEvaluated(); // This sets lastEvaluated to Instant.now()
        alertConfig.recordTrigger(); // This sets lastTriggered to Instant.now()
        alertConfig.setTriggerHistory("{\"test\":\"data\"}");
        bookmark.setAlertConfig(alertConfig);
        
        System.out.println("=== BEFORE API SERIALIZATION ===");
        System.out.println("Top-level createdAt: " + bookmark.getCreatedAt());
        System.out.println("Top-level updatedAt: " + bookmark.getUpdatedAt());
        System.out.println("Nested lastEvaluated: " + bookmark.getAlertConfig().getLastEvaluated());
        System.out.println("Nested lastTriggered: " + bookmark.getAlertConfig().getLastTriggered());
        System.out.println("Alert threshold: " + bookmark.getAlertConfig().getAlertThreshold());
        
        // Serialize to JSON for API response (this is what gets sent to client)
        String apiResponseJson = apiObjectMapper.writeValueAsString(bookmark);
        
        System.out.println("\n=== API RESPONSE JSON ===");
        System.out.println(apiResponseJson);
        
        // Check if timestamps are present in the JSON
        boolean hasCreatedAt = apiResponseJson.contains("createdAt");
        boolean hasLastEvaluated = apiResponseJson.contains("lastEvaluated");
        boolean hasLastTriggered = apiResponseJson.contains("lastTriggered");
        
        System.out.println("\n=== JSON FIELD PRESENCE ===");
        System.out.println("Contains 'createdAt': " + hasCreatedAt);
        System.out.println("Contains 'lastEvaluated': " + hasLastEvaluated);
        System.out.println("Contains 'lastTriggered': " + hasLastTriggered);
        
        // Parse the JSON back to see what the client would receive
        Bookmark clientBookmark = apiObjectMapper.readValue(apiResponseJson, Bookmark.class);
        
        System.out.println("\n=== WHAT CLIENT RECEIVES ===");
        System.out.println("Top-level createdAt: " + clientBookmark.getCreatedAt());
        System.out.println("Top-level updatedAt: " + clientBookmark.getUpdatedAt());
        
        if (clientBookmark.getAlertConfig() != null) {
            System.out.println("Nested lastEvaluated: " + clientBookmark.getAlertConfig().getLastEvaluated());
            System.out.println("Nested lastTriggered: " + clientBookmark.getAlertConfig().getLastTriggered());
            System.out.println("Alert threshold: " + clientBookmark.getAlertConfig().getAlertThreshold());
        } else {
            System.out.println("AlertConfig is NULL in client object!");
        }
        
        // Verify that all fields are preserved in API response
        System.out.println("\n=== FIELD PRESERVATION CHECK ===");
        System.out.println("createdAt preserved: " + (clientBookmark.getCreatedAt() != null));
        System.out.println("updatedAt preserved: " + (clientBookmark.getUpdatedAt() != null));
        
        if (clientBookmark.getAlertConfig() != null) {
            System.out.println("lastEvaluated preserved: " + (clientBookmark.getAlertConfig().getLastEvaluated() != null));
            System.out.println("lastTriggered preserved: " + (clientBookmark.getAlertConfig().getLastTriggered() != null));
            System.out.println("alertThreshold preserved: " + (clientBookmark.getAlertConfig().getAlertThreshold() != null));
            
            // Check if timestamps match (within a reasonable margin)
            if (clientBookmark.getAlertConfig().getLastEvaluated() != null) {
                long originalEpoch = bookmark.getAlertConfig().getLastEvaluated().getEpochSecond();
                long clientEpoch = clientBookmark.getAlertConfig().getLastEvaluated().getEpochSecond();
                System.out.println("lastEvaluated epoch match: " + (originalEpoch == clientEpoch));
            }
            
            if (clientBookmark.getAlertConfig().getLastTriggered() != null) {
                long originalEpoch = bookmark.getAlertConfig().getLastTriggered().getEpochSecond();
                long clientEpoch = clientBookmark.getAlertConfig().getLastTriggered().getEpochSecond();
                System.out.println("lastTriggered epoch match: " + (originalEpoch == clientEpoch));
            }
        }
    }
    
    @Test
    void testDirectInstantSerialization() throws Exception {
        // Test direct Instant serialization with the API ObjectMapper
        ObjectMapper apiObjectMapper = new ObjectMapper();
        apiObjectMapper.registerModule(new JavaTimeModule());
        
        Instant testInstant = Instant.now();
        System.out.println("=== DIRECT INSTANT SERIALIZATION ===");
        System.out.println("Original Instant: " + testInstant);
        
        // Serialize just the Instant
        String instantJson = apiObjectMapper.writeValueAsString(testInstant);
        System.out.println("Instant as JSON: " + instantJson);
        
        // Deserialize it back
        Instant deserializedInstant = apiObjectMapper.readValue(instantJson, Instant.class);
        System.out.println("Deserialized Instant: " + deserializedInstant);
        
        System.out.println("Instants match: " + testInstant.equals(deserializedInstant));
    }
}