package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRepository Tests")
class UserRepositoryTest {
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    private UserRepository repository;
    private final String tableName = "test-users-table";
    
    @BeforeEach
    void setUp() {
        repository = new UserRepository(mockDynamoDbClient, tableName);
    }
    
    @Test
    @DisplayName("Should find user by email when user exists")
    void shouldFindUserByEmailWhenExists() throws SeatmapException {
        // Given
        String email = "test@example.com";
        User expectedUser = createTestUser();
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(createUserAttributeMap()))
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByEmail(email);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedUser.getEmail(), result.get().getEmail());
        assertEquals(expectedUser.getUserId(), result.get().getUserId());
        
        // Verify DynamoDB query was called correctly
        verify(mockDynamoDbClient).query(argThat((QueryRequest request) -> 
            request.tableName().equals(tableName) &&
            request.indexName().equals("email-index") &&
            request.keyConditionExpression().equals("email = :email") &&
            request.expressionAttributeValues().containsKey(":email")
        ));
    }
    
    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() throws SeatmapException {
        // Given
        String email = "nonexistent@example.com";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByEmail(email);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should throw exception when DynamoDB fails during email lookup")
    void shouldThrowExceptionWhenDynamoDbFailsDuringEmailLookup() {
        // Given
        String email = "test@example.com";
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.findByEmail(email)
        );
        
        assertTrue(exception.getMessage().contains("Failed to find user by email"));
    }
    
    @Test
    @DisplayName("Should find user by OAuth ID when user exists")
    void shouldFindUserByOauthIdWhenExists() throws SeatmapException {
        // Given
        String oauthId = "oauth_123456789";
        User expectedUser = createTestUser();
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(createUserAttributeMap()))
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByOauthId(oauthId);
        
        // Then
        assertTrue(result.isPresent());
        assertEquals(expectedUser.getUserId(), result.get().getUserId());
        
        // Verify DynamoDB query was called correctly
        verify(mockDynamoDbClient).query(argThat((QueryRequest request) -> 
            request.tableName().equals(tableName) &&
            request.indexName().equals("oauth-id-index") &&
            request.keyConditionExpression().equals("oauthId = :oauthId") &&
            request.expressionAttributeValues().containsKey(":oauthId")
        ));
    }
    
    @Test
    @DisplayName("Should return empty when user not found by OAuth ID")
    void shouldReturnEmptyWhenUserNotFoundByOauthId() throws SeatmapException {
        // Given
        String oauthId = "nonexistent_oauth_id";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByOauthId(oauthId);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should throw exception when DynamoDB fails during OAuth ID lookup")
    void shouldThrowExceptionWhenDynamoDbFailsDuringOauthIdLookup() {
        // Given
        String oauthId = "oauth_123";
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.findByOauthId(oauthId)
        );
        
        assertTrue(exception.getMessage().contains("Failed to find user by OAuth ID"));
    }
    
    @Test
    @DisplayName("Should find user by verification token when user exists")
    void shouldFindUserByVerificationTokenWhenExists() throws SeatmapException {
        // Given
        String verificationToken = "verify_token_123";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(createUserAttributeMap()))
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByVerificationToken(verificationToken);
        
        // Then
        assertTrue(result.isPresent());
        
        // Verify DynamoDB query was called correctly
        verify(mockDynamoDbClient).query(argThat((QueryRequest request) -> 
            request.tableName().equals(tableName) &&
            request.indexName().equals("verification-token-index") &&
            request.keyConditionExpression().equals("verificationToken = :token") &&
            request.expressionAttributeValues().containsKey(":token")
        ));
    }
    
    @Test
    @DisplayName("Should return empty when user not found by verification token")
    void shouldReturnEmptyWhenUserNotFoundByVerificationToken() throws SeatmapException {
        // Given
        String verificationToken = "nonexistent_token";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<User> result = repository.findByVerificationToken(verificationToken);
        
        // Then
        assertFalse(result.isPresent());
    }
    
    @Test
    @DisplayName("Should throw exception when DynamoDB fails during verification token lookup")
    void shouldThrowExceptionWhenDynamoDbFailsDuringVerificationTokenLookup() {
        // Given
        String verificationToken = "verify_123";
        
        when(mockDynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        // When/Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            repository.findByVerificationToken(verificationToken)
        );
        
        assertTrue(exception.getMessage().contains("Failed to find user by verification token"));
    }
    
    @Test
    @DisplayName("Should check if email exists - returns true when exists")
    void shouldCheckEmailExistsReturnsTrueWhenExists() throws SeatmapException {
        // Given
        String email = "existing@example.com";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(createUserAttributeMap()))
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        boolean result = repository.emailExists(email);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should check if email exists - returns false when not exists")
    void shouldCheckEmailExistsReturnsFalseWhenNotExists() throws SeatmapException {
        // Given
        String email = "nonexistent@example.com";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        boolean result = repository.emailExists(email);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should check if OAuth ID exists - returns true when exists")
    void shouldCheckOauthIdExistsReturnsTrueWhenExists() throws SeatmapException {
        // Given
        String oauthId = "existing_oauth_id";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.singletonList(createUserAttributeMap()))
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        boolean result = repository.oauthIdExists(oauthId);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    @DisplayName("Should check if OAuth ID exists - returns false when not exists")
    void shouldCheckOauthIdExistsReturnsFalseWhenNotExists() throws SeatmapException {
        // Given
        String oauthId = "nonexistent_oauth_id";
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(Collections.emptyList())
            .build();
            
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        boolean result = repository.oauthIdExists(oauthId);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    @DisplayName("Should return correct entity class")
    void shouldReturnCorrectEntityClass() {
        assertEquals(User.class, repository.getEntityClass());
    }
    
    @Test
    @DisplayName("Should return correct hash key name")
    void shouldReturnCorrectHashKeyName() {
        assertEquals("userId", repository.getHashKeyName());
    }
    
    @Test
    @DisplayName("Should save user successfully and update timestamp")
    void shouldSaveUserSuccessfullyAndUpdateTimestamp() throws SeatmapException {
        // Given
        User user = createTestUser();
        Instant originalUpdatedAt = user.getUpdatedAt();
        
        PutItemResponse putItemResponse = PutItemResponse.builder().build();
        when(mockDynamoDbClient.putItem(any(PutItemRequest.class))).thenReturn(putItemResponse);
        
        // When
        assertDoesNotThrow(() -> repository.saveUser(user));
        
        // Then
        assertTrue(user.getUpdatedAt().isAfter(originalUpdatedAt));
        verify(mockDynamoDbClient).putItem(argThat((PutItemRequest request) -> 
            request.tableName().equals(tableName) &&
            request.item().containsKey("userId") &&
            request.item().containsKey("email")
        ));
    }
    
    @Test
    @DisplayName("Should include JsonIgnore fields in toAttributeValueMap")
    void shouldIncludeJsonIgnoreFieldsInToAttributeValueMap() throws SeatmapException {
        // Given
        User user = createTestUser();
        user.setPasswordHash("hashed-password");
        user.setVerificationToken("verification-token");
        user.setVerificationExpiresAt(Instant.now().plusSeconds(3600));
        
        // When
        Map<String, AttributeValue> result = repository.toAttributeValueMap(user);
        
        // Then
        assertTrue(result.containsKey("passwordHash"));
        assertTrue(result.containsKey("verificationToken"));
        assertTrue(result.containsKey("verificationExpiresAt"));
        assertEquals("hashed-password", result.get("passwordHash").s());
        assertEquals("verification-token", result.get("verificationToken").s());
        assertNotNull(result.get("verificationExpiresAt").n());
    }
    
    @Test
    @DisplayName("Should restore JsonIgnore fields in fromAttributeValueMap")
    void shouldRestoreJsonIgnoreFieldsInFromAttributeValueMap() throws SeatmapException {
        // Given
        Map<String, AttributeValue> attributeMap = Map.of(
            "userId", AttributeValue.builder().s("test-user-123").build(),
            "email", AttributeValue.builder().s("test@example.com").build(),
            "firstName", AttributeValue.builder().s("John").build(),
            "lastName", AttributeValue.builder().s("Doe").build(),
            "passwordHash", AttributeValue.builder().s("hashed-password").build(),
            "verificationToken", AttributeValue.builder().s("verification-token").build(),
            "verificationExpiresAt", AttributeValue.builder().n(String.valueOf(Instant.now().plusSeconds(3600).getEpochSecond())).build(),
            "emailVerified", AttributeValue.builder().bool(true).build(),
            "createdAt", AttributeValue.builder().s(Instant.now().toString()).build(),
            "updatedAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
        
        // When
        User result = repository.fromAttributeValueMap(attributeMap);
        
        // Then
        assertEquals("hashed-password", result.getPasswordHash());
        assertEquals("verification-token", result.getVerificationToken());
        assertNotNull(result.getVerificationExpiresAt());
    }
    
    @Test
    @DisplayName("Should handle null JsonIgnore fields in toAttributeValueMap")
    void shouldHandleNullJsonIgnoreFieldsInToAttributeValueMap() throws SeatmapException {
        // Given
        User user = createTestUser();
        user.setPasswordHash(null);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        
        // When
        Map<String, AttributeValue> result = repository.toAttributeValueMap(user);
        
        // Then - should not contain null fields
        assertFalse(result.containsKey("passwordHash"));
        assertFalse(result.containsKey("verificationToken"));
        assertFalse(result.containsKey("verificationExpiresAt"));
    }
    
    @Test
    @DisplayName("Should handle missing JsonIgnore fields in fromAttributeValueMap")
    void shouldHandleMissingJsonIgnoreFieldsInFromAttributeValueMap() throws SeatmapException {
        // Given - basic attribute map without JsonIgnore fields
        Map<String, AttributeValue> attributeMap = Map.of(
            "userId", AttributeValue.builder().s("test-user-123").build(),
            "email", AttributeValue.builder().s("test@example.com").build(),
            "firstName", AttributeValue.builder().s("John").build(),
            "lastName", AttributeValue.builder().s("Doe").build(),
            "emailVerified", AttributeValue.builder().bool(true).build(),
            "createdAt", AttributeValue.builder().s(Instant.now().toString()).build(),
            "updatedAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
        
        // When
        User result = repository.fromAttributeValueMap(attributeMap);
        
        // Then - should not fail and JsonIgnore fields should be null
        assertNull(result.getPasswordHash());
        assertNull(result.getVerificationToken());
        assertNull(result.getVerificationExpiresAt());
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUserId("test-user-123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setPasswordHash("hashed_password");
        user.setEmailVerified(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
    
    private Map<String, AttributeValue> createUserAttributeMap() {
        return Map.of(
            "userId", AttributeValue.builder().s("test-user-123").build(),
            "email", AttributeValue.builder().s("test@example.com").build(),
            "firstName", AttributeValue.builder().s("John").build(),
            "lastName", AttributeValue.builder().s("Doe").build(),
            "passwordHash", AttributeValue.builder().s("hashed_password").build(),
            "verified", AttributeValue.builder().bool(true).build(),
            "createdAt", AttributeValue.builder().s(Instant.now().toString()).build(),
            "updatedAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
    }
}