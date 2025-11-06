package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionRepository Tests")
class SessionRepositoryTest {
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    private SessionRepository repository;
    private final String tableName = "test-sessions-table";
    
    @BeforeEach
    void setUp() {
        repository = new SessionRepository(mockDynamoDbClient, tableName);
    }
    
    @Test
    @DisplayName("Should find session by session ID and user ID when session exists")
    void shouldFindSessionBySessionIdAndUserIdWhenExists() throws SeatmapException {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        Session expectedSession = createTestSession();
        
        GetItemResponse getItemResponse = GetItemResponse.builder()
            .item(createSessionAttributeMap())
            .build();
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        
        // When
        Optional<Session> result = repository.findBySessionId(sessionId, userId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedSession.getSessionId(), result.get().getSessionId());
        assertEquals(expectedSession.getUserId(), result.get().getUserId());
        
        // Verify DynamoDB getItem was called correctly
        verify(mockDynamoDbClient).getItem(argThat((GetItemRequest request) -> 
            request.tableName().equals(tableName) &&
            request.key().containsKey("sessionId") &&
            request.key().containsKey("userId")
        ));
    }
    
    @Test
    @DisplayName("Should return empty when session not found")
    void shouldReturnEmptyWhenSessionNotFound() throws SeatmapException {
        // Given
        String sessionId = "nonexistent-session";
        String userId = "user-456";
        
        // DynamoDB returns a response without an item when not found
        GetItemResponse getItemResponse = GetItemResponse.builder()
            .build(); // No item means not found
            
        when(mockDynamoDbClient.getItem(any(GetItemRequest.class))).thenReturn(getItemResponse);
        
        // When
        Optional<Session> result = repository.findBySessionId(sessionId, userId);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should save session successfully")
    void shouldSaveSessionSuccessfully() throws SeatmapException {
        // Given
        Session session = createTestSession();
        
        PutItemResponse putItemResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(putItemResponse);
        
        // When
        assertDoesNotThrow(() -> repository.saveSession(session));
        
        // Then
        verify(mockDynamoDbClient).putItem(argThat((PutItemRequest request) -> 
            request.tableName().equals(tableName) &&
            request.item().containsKey("sessionId") &&
            request.item().containsKey("userId")
        ));
    }
    
    @Test
    @DisplayName("Should delete session successfully")
    void shouldDeleteSessionSuccessfully() throws SeatmapException {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        
        DeleteItemResponse deleteItemResponse = DeleteItemResponse.builder().build();
        when(mockDynamoDbClient.deleteItem(any(DeleteItemRequest.class))).thenReturn(deleteItemResponse);
        
        // When
        assertDoesNotThrow(() -> repository.deleteSession(sessionId, userId));
        
        // Then
        verify(mockDynamoDbClient).deleteItem(argThat((DeleteItemRequest request) -> 
            request.tableName().equals(tableName) &&
            request.key().containsKey("sessionId") &&
            request.key().containsKey("userId")
        ));
    }
    
    @Test
    @DisplayName("Should increment guest flights viewed successfully")
    void shouldIncrementGuestFlightsViewedSuccessfully() throws SeatmapException {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        
        UpdateItemResponse updateItemResponse = UpdateItemResponse.builder().build();
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class))).thenReturn(updateItemResponse);
        
        // When
        assertDoesNotThrow(() -> repository.incrementGuestFlightsViewed(sessionId, userId));
        
        // Then
        verify(mockDynamoDbClient).updateItem(argThat((UpdateItemRequest request) -> {
            boolean tableNameCorrect = tableName.equals(request.tableName());
            boolean hasSessionIdKey = request.key().containsKey("sessionId");
            boolean hasUserIdKey = request.key().containsKey("userId");
            boolean hasUpdateExpression = request.updateExpression() != null && 
                request.updateExpression().equals("SET #guestFlights = if_not_exists(#guestFlights, :zero) + :inc");
            boolean hasExpressionAttributeNames = request.expressionAttributeNames() != null &&
                request.expressionAttributeNames().containsKey("#guestFlights");
            boolean hasExpressionAttributeValues = request.expressionAttributeValues() != null &&
                request.expressionAttributeValues().containsKey(":inc") &&
                request.expressionAttributeValues().containsKey(":zero");
                
            return tableNameCorrect && hasSessionIdKey && hasUserIdKey && 
                   hasUpdateExpression && hasExpressionAttributeNames && hasExpressionAttributeValues;
        }));
    }
    
    @Test
    @DisplayName("Should throw exception when DynamoDB fails during increment")
    void shouldThrowExceptionWhenDynamoDbFailsDuringIncrement() {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.incrementGuestFlightsViewed(sessionId, userId)
        );
        
        assertTrue(exception.getMessage().contains("Failed to increment guest flights viewed"));
    }
    
    @Test
    @DisplayName("Should return correct entity class")
    void shouldReturnCorrectEntityClass() {
        assertEquals(Session.class, repository.getEntityClass());
    }
    
    @Test
    @DisplayName("Should return correct hash key name")
    void shouldReturnCorrectHashKeyName() {
        assertEquals("sessionId", repository.getHashKeyName());
    }
    
    @Test
    @DisplayName("Should return correct range key name")
    void shouldReturnCorrectRangeKeyName() {
        assertEquals("userId", repository.getRangeKeyName());
    }
    
    @Test
    @DisplayName("Should handle conditional check failures during increment")
    void shouldHandleConditionalCheckFailuresDuringIncrement() {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class)))
            .thenThrow(ConditionalCheckFailedException.builder()
                .message("Conditional check failed")
                .build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.incrementGuestFlightsViewed(sessionId, userId)
        );
        
        assertTrue(exception.getMessage().contains("Failed to increment guest flights viewed"));
    }
    
    @Test
    @DisplayName("Should handle resource not found exceptions during increment")
    void shouldHandleResourceNotFoundExceptionsDuringIncrement() {
        // Given
        String sessionId = "session-123";
        String userId = "user-456";
        
        when(mockDynamoDbClient.updateItem(any(UpdateItemRequest.class)))
            .thenThrow(ResourceNotFoundException.builder()
                .message("Resource not found")
                .build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.incrementGuestFlightsViewed(sessionId, userId)
        );
        
        assertTrue(exception.getMessage().contains("Failed to increment guest flights viewed"));
    }
    
    private Session createTestSession() {
        Session session = new Session();
        session.setSessionId("session-123");
        session.setUserId("user-456");
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        session.setGuestFlightsViewed(0);
        session.setCreatedAt(Instant.now());
        return session;
    }
    
    private Map<String, AttributeValue> createSessionAttributeMap() {
        return Map.of(
            "sessionId", AttributeValue.builder().s("session-123").build(),
            "userId", AttributeValue.builder().s("user-456").build(),
            "expiresAt", AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(3600).getEpochSecond())).build(),
            "guestFlightsViewed", AttributeValue.builder().n("0").build(),
            "createdAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
    }
}