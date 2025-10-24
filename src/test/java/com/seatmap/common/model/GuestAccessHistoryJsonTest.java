package com.seatmap.common.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for @JsonIgnore annotations on computed properties in GuestAccessHistory
 * This test ensures that computed properties are not serialized to DynamoDB
 */
@DisplayName("GuestAccessHistory JSON Serialization Tests")
class GuestAccessHistoryJsonTest {

    private ObjectMapper objectMapper;
    private GuestAccessHistory testHistory;

    @BeforeEach
    void setUp() {
        // Use the same ObjectMapper configuration as DynamoDbRepository
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        testHistory = new GuestAccessHistory("192.168.1.100");
        testHistory.setSeatmapRequestsUsed(1);
        testHistory.setFirstAccess(Instant.parse("2025-01-01T10:00:00Z"));
        testHistory.setLastSeatmapRequest(Instant.parse("2025-01-02T15:30:00Z"));
        testHistory.setExpiresAt(Instant.parse("2025-07-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Computed properties should be ignored during serialization")
    void computedProperties_ShouldBeIgnoredDuringSerialization() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(testHistory);
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);

        // Then - computed properties should NOT be present
        assertFalse(jsonMap.containsKey("remainingSeatmapRequests"), 
            "remainingSeatmapRequests should not be serialized due to @JsonIgnore");
        assertFalse(jsonMap.containsKey("seatmapDenialMessage"), 
            "seatmapDenialMessage should not be serialized due to @JsonIgnore");

        // But actual fields should be present
        assertTrue(jsonMap.containsKey("ipAddress"), "ipAddress should be serialized");
        assertTrue(jsonMap.containsKey("seatmapRequestsUsed"), "seatmapRequestsUsed should be serialized");
        assertTrue(jsonMap.containsKey("firstAccess"), "firstAccess should be serialized");
        assertTrue(jsonMap.containsKey("lastSeatmapRequest"), "lastSeatmapRequest should be serialized");
        assertTrue(jsonMap.containsKey("expiresAt"), "expiresAt should be serialized");
    }

    @Test
    @DisplayName("Computed properties should work correctly after deserialization")
    void computedProperties_ShouldWorkCorrectlyAfterDeserialization() throws Exception {
        // Given
        String json = objectMapper.writeValueAsString(testHistory);

        // When
        GuestAccessHistory deserialized = objectMapper.readValue(json, GuestAccessHistory.class);

        // Then - computed properties should work correctly
        assertEquals(1, deserialized.getRemainingSeatmapRequests(), "Should compute remaining requests correctly");
        assertEquals("Seat map access is temporarily unavailable. Please register for unlimited access.", 
            deserialized.getSeatmapDenialMessage(), "Should compute denial message correctly");
        assertFalse(deserialized.hasExceededSeatmapLimit(), "Should compute limit status correctly");
        assertTrue(deserialized.canMakeSeatmapRequest(), "Should compute request permission correctly");
    }

    @Test
    @DisplayName("Serialization should handle edge cases without computed properties")
    void serialization_ShouldHandleEdgeCasesWithoutComputedProperties() throws Exception {
        // Test case 1: Zero requests used
        GuestAccessHistory zeroRequests = new GuestAccessHistory("192.168.1.200");
        zeroRequests.setSeatmapRequestsUsed(0);

        String json = objectMapper.writeValueAsString(zeroRequests);
        GuestAccessHistory deserialized = objectMapper.readValue(json, GuestAccessHistory.class);

        assertEquals(0, deserialized.getSeatmapRequestsUsed());
        assertEquals(2, deserialized.getRemainingSeatmapRequests());
        assertTrue(deserialized.canMakeSeatmapRequest());

        // Test case 2: Maximum requests used
        GuestAccessHistory maxRequests = new GuestAccessHistory("192.168.1.201");
        maxRequests.setSeatmapRequestsUsed(2);

        json = objectMapper.writeValueAsString(maxRequests);
        deserialized = objectMapper.readValue(json, GuestAccessHistory.class);

        assertEquals(2, deserialized.getSeatmapRequestsUsed());
        assertEquals(0, deserialized.getRemainingSeatmapRequests());
        assertFalse(deserialized.canMakeSeatmapRequest());
        assertTrue(deserialized.hasExceededSeatmapLimit());
    }

    @Test
    @DisplayName("Deserialization should handle JSON with extra computed properties gracefully")
    void deserialization_ShouldHandleJsonWithExtraComputedPropertiesGracefully() throws Exception {
        // Given - JSON that includes the computed properties (simulating corrupted DynamoDB data)
        String jsonWithExtraFields = """
            {
                "ipAddress": "192.168.1.100",
                "seatmapRequestsUsed": 1,
                "firstAccess": "2025-01-01T10:00:00Z",
                "lastSeatmapRequest": "2025-01-02T15:30:00Z",
                "expiresAt": "2025-07-01T10:00:00Z",
                "remainingSeatmapRequests": 999,
                "seatmapDenialMessage": "Some old message"
            }
            """;

        // When - this should not throw an exception due to FAIL_ON_UNKNOWN_PROPERTIES = false
        GuestAccessHistory deserialized = objectMapper.readValue(jsonWithExtraFields, GuestAccessHistory.class);

        // Then - should ignore the extra fields and compute correctly from actual fields
        assertNotNull(deserialized);
        assertEquals("192.168.1.100", deserialized.getIpAddress());
        assertEquals(1, deserialized.getSeatmapRequestsUsed());
        
        // Computed properties should be based on actual fields, not the corrupted extra fields
        assertEquals(1, deserialized.getRemainingSeatmapRequests()); // Should be 2-1=1, not 999
        assertNotEquals("Some old message", deserialized.getSeatmapDenialMessage()); // Should compute fresh
    }

    @Test
    @DisplayName("JSON structure should be clean and minimal")
    void jsonStructure_ShouldBeCleanAndMinimal() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(testHistory);

        // Then - verify JSON structure is clean
        assertFalse(json.contains("remainingSeatmapRequests"), "JSON should not contain computed property");
        assertFalse(json.contains("seatmapDenialMessage"), "JSON should not contain computed property");
        
        // Should contain actual fields
        assertTrue(json.contains("ipAddress"), "JSON should contain actual field");
        assertTrue(json.contains("seatmapRequestsUsed"), "JSON should contain actual field");
        assertTrue(json.contains("firstAccess"), "JSON should contain actual field");
        assertTrue(json.contains("expiresAt"), "JSON should contain actual field");
    }

    @Test
    @DisplayName("Round-trip serialization should preserve all data correctly")
    void roundTripSerialization_ShouldPreserveAllDataCorrectly() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(testHistory);
        GuestAccessHistory deserialized = objectMapper.readValue(json, GuestAccessHistory.class);

        // Then - all actual fields should be preserved
        assertEquals(testHistory.getIpAddress(), deserialized.getIpAddress());
        assertEquals(testHistory.getSeatmapRequestsUsed(), deserialized.getSeatmapRequestsUsed());
        assertEquals(testHistory.getFirstAccess(), deserialized.getFirstAccess());
        assertEquals(testHistory.getLastSeatmapRequest(), deserialized.getLastSeatmapRequest());
        assertEquals(testHistory.getExpiresAt(), deserialized.getExpiresAt());

        // And computed properties should work the same
        assertEquals(testHistory.getRemainingSeatmapRequests(), deserialized.getRemainingSeatmapRequests());
        assertEquals(testHistory.getSeatmapDenialMessage(), deserialized.getSeatmapDenialMessage());
        assertEquals(testHistory.canMakeSeatmapRequest(), deserialized.canMakeSeatmapRequest());
        assertEquals(testHistory.hasExceededSeatmapLimit(), deserialized.hasExceededSeatmapLimit());
    }
}