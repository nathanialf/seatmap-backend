package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.User;
import com.seatmap.common.repository.DynamoDbRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

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
}