package com.seatmap.auth.repository;

import com.seatmap.common.model.GuestAccessHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for GuestAccessRepository rate limiting functionality
 * These tests verify the end-to-end flow that was broken by the Jackson serialization bug
 */
@DisplayName("Guest Access Repository Integration Tests")
class GuestAccessRepositoryIntegrationTest {

    @Mock
    private DynamoDbClient mockDynamoDbClient;

    private GuestAccessRepository repository;
    private final String testIp = "192.168.1.100";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new GuestAccessRepository(mockDynamoDbClient);
    }

    @Test
    @DisplayName("End-to-end flow: First-time user should be allowed and record should be created")
    void endToEndFlow_FirstTimeUser_ShouldBeAllowedAndRecordCreated() throws Exception {
        // Given - no existing record
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().build()); // Empty response

        // When - check if can make request (first time)
        boolean canMakeRequest = repository.canMakeSeatmapRequest(testIp);

        // Then
        assertTrue(canMakeRequest, "First-time user should be allowed to make request");
        assertEquals(2, repository.getRemainingSeatmapRequests(testIp), "Should have 2 requests remaining");

        // When - record the request
        repository.recordSeatmapRequest(testIp);

        // Then - verify save was called
        verify(mockDynamoDbClient, times(1)).putItem(any(PutItemRequest.class));
    }

    @Test
    @DisplayName("End-to-end flow: User with 1 request used should be allowed")
    void endToEndFlow_UserWith1RequestUsed_ShouldBeAllowed() throws Exception {
        // Given - existing record with 1 request used
        Map<String, AttributeValue> existingRecord = createDynamoDbRecord(testIp, 1);
        GetItemResponse response = GetItemResponse.builder()
            .item(existingRecord)
            .build();
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(response);

        // When
        boolean canMakeRequest = repository.canMakeSeatmapRequest(testIp);
        int remainingRequests = repository.getRemainingSeatmapRequests(testIp);

        // Then
        assertTrue(canMakeRequest, "User with 1 request used should be allowed");
        assertEquals(1, remainingRequests, "Should have 1 request remaining");
    }

    @Test
    @DisplayName("End-to-end flow: User with 2 requests used should be blocked")
    void endToEndFlow_UserWith2RequestsUsed_ShouldBeBlocked() throws Exception {
        // Given - existing record with 2 requests used (at limit)
        Map<String, AttributeValue> existingRecord = createDynamoDbRecord(testIp, 2);
        GetItemResponse response = GetItemResponse.builder()
            .item(existingRecord)
            .build();
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(response);

        // When
        boolean canMakeRequest = repository.canMakeSeatmapRequest(testIp);
        int remainingRequests = repository.getRemainingSeatmapRequests(testIp);
        String denialMessage = repository.getSeatmapDenialMessage(testIp);

        // Then
        assertFalse(canMakeRequest, "User with 2 requests used should be blocked");
        assertEquals(0, remainingRequests, "Should have 0 requests remaining");
        assertNotNull(denialMessage, "Should provide denial message");
        assertTrue(denialMessage.contains("2 free seat map views"), "Denial message should mention limit");
    }

    @Test
    @DisplayName("End-to-end flow: Recording request should increment counter correctly")
    void endToEndFlow_RecordingRequest_ShouldIncrementCounterCorrectly() throws Exception {
        // Given - existing record with 0 requests used
        Map<String, AttributeValue> existingRecord = createDynamoDbRecord(testIp, 0);
        GetItemResponse getResponse = GetItemResponse.builder()
            .item(existingRecord)
            .build();
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(getResponse);

        // When - record a seatmap request
        repository.recordSeatmapRequest(testIp);

        // Then - verify the save operation was called with incremented counter
        verify(mockDynamoDbClient).putItem(argThat((PutItemRequest request) -> {
            Map<String, AttributeValue> item = request.item();
            // Check that seatmapRequestsUsed was incremented to 1
            return item.get("seatmapRequestsUsed").n().equals("1");
        }));
    }

    @Test
    @DisplayName("Rate limiting should handle DynamoDB record with extra fields gracefully")
    void rateLimiting_ShouldHandleRecordWithExtraFieldsGracefully() throws Exception {
        // Given - corrupted DynamoDB record with extra fields (simulates the bug we fixed)
        Map<String, AttributeValue> corruptedRecord = createDynamoDbRecord(testIp, 1);
        
        // Add the problematic extra fields that caused the original serialization bug
        corruptedRecord.put("remainingSeatmapRequests", AttributeValue.builder().n("1").build());
        corruptedRecord.put("seatmapDenialMessage", AttributeValue.builder()
            .s("You've used your 2 free seat map views. Please register for unlimited seat map access.")
            .build());
        
        GetItemResponse response = GetItemResponse.builder()
            .item(corruptedRecord)
            .build();
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(response);

        // When - this should not throw an exception due to our Jackson configuration fix
        boolean canMakeRequest = repository.canMakeSeatmapRequest(testIp);
        int remainingRequests = repository.getRemainingSeatmapRequests(testIp);

        // Then - should work correctly based on actual seatmapRequestsUsed field, ignoring extra fields
        assertTrue(canMakeRequest, "Should be able to make request despite extra fields");
        assertEquals(1, remainingRequests, "Should calculate remaining requests correctly from actual field");
    }

    @Test
    @DisplayName("Rate limiting should work correctly after multiple sequential operations")
    void rateLimiting_ShouldWorkCorrectlyAfterMultipleSequentialOperations() throws Exception {
        // Simulate the exact sequence that was failing in production

        // Step 1: First request - no existing record
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().build()); // Empty response

        assertTrue(repository.canMakeSeatmapRequest(testIp), "First request should be allowed");
        repository.recordSeatmapRequest(testIp);

        // Step 2: Second request - record exists with 1 request used
        Map<String, AttributeValue> recordWith1Request = createDynamoDbRecord(testIp, 1);
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().item(recordWith1Request).build());

        assertTrue(repository.canMakeSeatmapRequest(testIp), "Second request should be allowed");
        repository.recordSeatmapRequest(testIp);

        // Step 3: Third request - record exists with 2 requests used (at limit)
        Map<String, AttributeValue> recordWith2Requests = createDynamoDbRecord(testIp, 2);
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().item(recordWith2Requests).build());

        assertFalse(repository.canMakeSeatmapRequest(testIp), "Third request should be blocked");
        assertEquals(0, repository.getRemainingSeatmapRequests(testIp), "Should have 0 requests remaining");
    }

    @Test
    @DisplayName("Unknown IP handling should work correctly")
    void unknownIpHandling_ShouldWorkCorrectly() throws Exception {
        // Given - no existing record for "unknown" IP
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().build()); // Empty response

        // When
        boolean canMakeRequest = repository.canMakeSeatmapRequest("unknown");
        int remainingRequests = repository.getRemainingSeatmapRequests("unknown");

        // Then
        assertTrue(canMakeRequest, "Unknown IP should be allowed initially");
        assertEquals(2, remainingRequests, "Unknown IP should have 2 requests remaining");
    }

    /**
     * Helper method to create a DynamoDB record for testing
     */
    private Map<String, AttributeValue> createDynamoDbRecord(String ipAddress, int seatmapRequestsUsed) {
        Map<String, AttributeValue> record = new HashMap<>();
        record.put("ipAddress", AttributeValue.builder().s(ipAddress).build());
        record.put("seatmapRequestsUsed", AttributeValue.builder().n(String.valueOf(seatmapRequestsUsed)).build());
        record.put("firstAccess", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build());
        record.put("expiresAt", AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(6 * 30 * 24 * 60 * 60).getEpochSecond())).build());
        
        if (seatmapRequestsUsed > 0) {
            record.put("lastSeatmapRequest", AttributeValue.builder().n(String.valueOf(Instant.now().getEpochSecond())).build());
        }
        
        return record;
    }
}