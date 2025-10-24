package com.seatmap.common.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seatmap.common.model.GuestAccessHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DynamoDB repository serialization/deserialization issues
 * This test prevents regressions like the Jackson computed property serialization bug
 */
@DisplayName("DynamoDB Repository Serialization Tests")
class DynamoDbRepositorySerializationTest {

    private TestDynamoDbRepository repository;
    private GuestAccessHistory testHistory;

    // Test implementation of DynamoDbRepository for testing
    private static class TestDynamoDbRepository extends DynamoDbRepository<GuestAccessHistory> {
        public TestDynamoDbRepository() {
            super(null, "test-table");
        }

        @Override
        protected Class<GuestAccessHistory> getEntityClass() {
            return GuestAccessHistory.class;
        }

        @Override
        protected String getHashKeyName() {
            return "ipAddress";
        }

        // Expose protected methods for testing
        public Map<String, AttributeValue> testToAttributeValueMap(GuestAccessHistory entity) throws Exception {
            return super.toAttributeValueMap(entity);
        }

        public GuestAccessHistory testFromAttributeValueMap(Map<String, AttributeValue> attributeMap) throws Exception {
            return super.fromAttributeValueMap(attributeMap);
        }
    }

    @BeforeEach
    void setUp() {
        repository = new TestDynamoDbRepository();
        
        testHistory = new GuestAccessHistory("192.168.1.100");
        testHistory.setSeatmapRequestsUsed(1);
        testHistory.setFirstAccess(Instant.parse("2025-01-01T10:00:00Z"));
        testHistory.setLastSeatmapRequest(Instant.parse("2025-01-02T15:30:00Z"));
        testHistory.setExpiresAt(Instant.parse("2025-07-01T10:00:00Z"));
    }

    @Test
    @DisplayName("Serialization should not include computed properties")
    void serialization_ShouldNotIncludeComputedProperties() throws Exception {
        // When
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(testHistory);

        // Then - computed properties should NOT be present
        assertFalse(attributeMap.containsKey("remainingSeatmapRequests"), 
            "remainingSeatmapRequests should not be serialized due to @JsonIgnore");
        assertFalse(attributeMap.containsKey("seatmapDenialMessage"), 
            "seatmapDenialMessage should not be serialized due to @JsonIgnore");

        // But actual fields should be present
        assertTrue(attributeMap.containsKey("ipAddress"));
        assertTrue(attributeMap.containsKey("seatmapRequestsUsed"));
        assertTrue(attributeMap.containsKey("firstAccess"));
        assertTrue(attributeMap.containsKey("lastSeatmapRequest"));
        assertTrue(attributeMap.containsKey("expiresAt"));
    }

    @Test
    @DisplayName("Round-trip serialization should preserve all actual fields")
    void roundTripSerialization_ShouldPreserveAllActualFields() throws Exception {
        // When
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(testHistory);
        GuestAccessHistory deserialized = repository.testFromAttributeValueMap(attributeMap);

        // Then
        assertEquals(testHistory.getIpAddress(), deserialized.getIpAddress());
        assertEquals(testHistory.getSeatmapRequestsUsed(), deserialized.getSeatmapRequestsUsed());
        assertEquals(testHistory.getFirstAccess(), deserialized.getFirstAccess());
        assertEquals(testHistory.getLastSeatmapRequest(), deserialized.getLastSeatmapRequest());
        assertEquals(testHistory.getExpiresAt(), deserialized.getExpiresAt());
    }

    @Test
    @DisplayName("Computed properties should work after deserialization")
    void computedProperties_ShouldWorkAfterDeserialization() throws Exception {
        // When
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(testHistory);
        GuestAccessHistory deserialized = repository.testFromAttributeValueMap(attributeMap);

        // Then - computed properties should work correctly
        assertEquals(1, deserialized.getRemainingSeatmapRequests()); // 2 - 1 = 1
        assertFalse(deserialized.hasExceededSeatmapLimit());
        assertTrue(deserialized.canMakeSeatmapRequest());
        assertNotNull(deserialized.getSeatmapDenialMessage());
    }

    @Test
    @DisplayName("Deserialization should handle unknown properties gracefully")
    void deserialization_ShouldHandleUnknownPropertiesGracefully() throws Exception {
        // Given - simulate DynamoDB entry with extra fields (like the bug we fixed)
        Map<String, AttributeValue> attributeMapWithExtraFields = repository.testToAttributeValueMap(testHistory);
        
        // Add the problematic extra fields that caused the original bug
        attributeMapWithExtraFields.put("remainingSeatmapRequests", 
            AttributeValue.builder().n("1").build());
        attributeMapWithExtraFields.put("seatmapDenialMessage", 
            AttributeValue.builder().s("Some denial message").build());
        attributeMapWithExtraFields.put("someOtherUnknownField", 
            AttributeValue.builder().s("unknown").build());

        // When - this should not throw an exception due to FAIL_ON_UNKNOWN_PROPERTIES = false
        GuestAccessHistory deserialized = repository.testFromAttributeValueMap(attributeMapWithExtraFields);

        // Then
        assertNotNull(deserialized);
        assertEquals(testHistory.getIpAddress(), deserialized.getIpAddress());
        assertEquals(testHistory.getSeatmapRequestsUsed(), deserialized.getSeatmapRequestsUsed());
        
        // Computed properties should still work correctly based on actual fields
        assertEquals(1, deserialized.getRemainingSeatmapRequests());
    }

    @Test
    @DisplayName("ObjectMapper configuration should ignore unknown properties")
    void objectMapperConfiguration_ShouldIgnoreUnknownProperties() {
        // Given
        ObjectMapper mapper = repository.objectMapper;

        // Then
        assertFalse(mapper.getDeserializationConfig()
            .isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES),
            "ObjectMapper should be configured to ignore unknown properties");
    }

    @Test
    @DisplayName("Serialization should handle edge cases correctly")
    void serialization_ShouldHandleEdgeCasesCorrectly() throws Exception {
        // Given - edge case: zero requests used
        GuestAccessHistory zeroRequests = new GuestAccessHistory("192.168.1.200");
        zeroRequests.setSeatmapRequestsUsed(0);

        // When
        Map<String, AttributeValue> attributeMap = repository.testToAttributeValueMap(zeroRequests);
        GuestAccessHistory deserialized = repository.testFromAttributeValueMap(attributeMap);

        // Then
        assertEquals(0, deserialized.getSeatmapRequestsUsed());
        assertEquals(2, deserialized.getRemainingSeatmapRequests());
        assertTrue(deserialized.canMakeSeatmapRequest());

        // Given - edge case: maximum requests used
        GuestAccessHistory maxRequests = new GuestAccessHistory("192.168.1.201");
        maxRequests.setSeatmapRequestsUsed(2);

        // When
        attributeMap = repository.testToAttributeValueMap(maxRequests);
        deserialized = repository.testFromAttributeValueMap(attributeMap);

        // Then
        assertEquals(2, deserialized.getSeatmapRequestsUsed());
        assertEquals(0, deserialized.getRemainingSeatmapRequests());
        assertFalse(deserialized.canMakeSeatmapRequest());
        assertTrue(deserialized.hasExceededSeatmapLimit());
    }
}