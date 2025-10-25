package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import com.seatmap.common.repository.DynamoDbRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UserRepository extends DynamoDbRepository<User> {
    
    public UserRepository(DynamoDbClient dynamoDbClient, String tableName) {
        super(dynamoDbClient, tableName);
    }
    
    @Override
    protected Class<User> getEntityClass() {
        return User.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "userId";
    }
    
    public Optional<User> findByEmail(String email) throws SeatmapException {
        try {
            // Query the email-index GSI
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":email", AttributeValue.builder().s(email).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("email-index")
                    .keyConditionExpression("email = :email")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            if (!response.items().isEmpty()) {
                return Optional.of(fromAttributeValueMap(response.items().get(0)));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find user by email: " + e.getMessage());
        }
    }
    
    public Optional<User> findByOauthId(String oauthId) throws SeatmapException {
        try {
            // Query the oauth-id-index GSI
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":oauthId", AttributeValue.builder().s(oauthId).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("oauth-id-index")
                    .keyConditionExpression("oauthId = :oauthId")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            if (!response.items().isEmpty()) {
                return Optional.of(fromAttributeValueMap(response.items().get(0)));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find user by OAuth ID: " + e.getMessage());
        }
    }
    
    public boolean emailExists(String email) throws SeatmapException {
        return findByEmail(email).isPresent();
    }
    
    public boolean oauthIdExists(String oauthId) throws SeatmapException {
        return findByOauthId(oauthId).isPresent();
    }
    
    public Optional<User> findByVerificationToken(String verificationToken) throws SeatmapException {
        try {
            // Query the verification-token-index GSI
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":token", AttributeValue.builder().s(verificationToken).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("verification-token-index")
                    .keyConditionExpression("verificationToken = :token")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            if (!response.items().isEmpty()) {
                return Optional.of(fromAttributeValueMap(response.items().get(0)));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find user by verification token: " + e.getMessage());
        }
    }
    
    public void saveUser(User user) throws SeatmapException {
        // Update timestamp before saving
        user.updateTimestamp();
        save(user);
    }
    
    @Override
    protected Map<String, AttributeValue> toAttributeValueMap(User user) throws SeatmapException {
        // Get the base attribute map from parent (handles @JsonIgnore by excluding fields)
        Map<String, AttributeValue> attributeMap = super.toAttributeValueMap(user);
        
        // Manually add the @JsonIgnore fields that are needed for database storage
        if (user.getPasswordHash() != null) {
            attributeMap.put("passwordHash", AttributeValue.builder().s(user.getPasswordHash()).build());
        }
        
        if (user.getVerificationToken() != null) {
            attributeMap.put("verificationToken", AttributeValue.builder().s(user.getVerificationToken()).build());
        }
        
        if (user.getVerificationExpiresAt() != null) {
            attributeMap.put("verificationExpiresAt", AttributeValue.builder().n(String.valueOf(user.getVerificationExpiresAt().getEpochSecond())).build());
        }
        
        return attributeMap;
    }
    
    @Override
    protected User fromAttributeValueMap(Map<String, AttributeValue> attributeMap) throws SeatmapException {
        // Get the base user object from parent
        User user = super.fromAttributeValueMap(attributeMap);
        
        // Manually set the @JsonIgnore fields from the database
        if (attributeMap.containsKey("passwordHash") && attributeMap.get("passwordHash").s() != null) {
            user.setPasswordHash(attributeMap.get("passwordHash").s());
        }
        
        if (attributeMap.containsKey("verificationToken") && attributeMap.get("verificationToken").s() != null) {
            user.setVerificationToken(attributeMap.get("verificationToken").s());
        }
        
        if (attributeMap.containsKey("verificationExpiresAt") && attributeMap.get("verificationExpiresAt").n() != null) {
            long epochSeconds = Long.parseLong(attributeMap.get("verificationExpiresAt").n());
            user.setVerificationExpiresAt(Instant.ofEpochSecond(epochSeconds));
        }
        
        return user;
    }
}