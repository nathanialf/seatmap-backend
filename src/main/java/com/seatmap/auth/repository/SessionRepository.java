package com.seatmap.auth.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import com.seatmap.common.repository.DynamoDbRepository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SessionRepository extends DynamoDbRepository<Session> {
    
    public SessionRepository(DynamoDbClient dynamoDbClient, String tableName) {
        super(dynamoDbClient, tableName);
    }
    
    @Override
    protected Class<Session> getEntityClass() {
        return Session.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "sessionId";
    }
    
    @Override
    protected String getRangeKeyName() {
        return "userId";
    }
    
    public Optional<Session> findBySessionId(String sessionId, String userId) throws SeatmapException {
        return findByKey(sessionId, userId);
    }
    
    public void saveSession(Session session) throws SeatmapException {
        save(session);
    }
    
    public void deleteSession(String sessionId, String userId) throws SeatmapException {
        delete(sessionId, userId);
    }
    
    public void incrementGuestFlightsViewed(String sessionId, String userId) throws SeatmapException {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("sessionId", AttributeValue.builder().s(sessionId).build());
            key.put("userId", AttributeValue.builder().s(userId).build());
            
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":inc", AttributeValue.builder().n("1").build());
            expressionAttributeValues.put(":zero", AttributeValue.builder().n("0").build());
            
            Map<String, String> expressionAttributeNames = new HashMap<>();
            expressionAttributeNames.put("#guestFlights", "guestFlightsViewed");
            
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .updateExpression("SET #guestFlights = if_not_exists(#guestFlights, :zero) + :inc")
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();
                    
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to increment guest flights viewed: " + e.getMessage());
        }
    }
    
    public boolean isSessionValid(String sessionId, String userId) throws SeatmapException {
        Optional<Session> sessionOpt = findBySessionId(sessionId, userId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        return !session.isExpired();
    }
    
    @Override
    protected Map<String, AttributeValue> toAttributeValueMap(Session session) throws SeatmapException {
        Map<String, AttributeValue> item = super.toAttributeValueMap(session);
        
        // Add TTL attribute for DynamoDB automatic expiration
        if (session.getExpiresAt() != null) {
            long ttl = session.getExpiresAt().getEpochSecond();
            item.put("expiresAt", AttributeValue.builder().n(String.valueOf(ttl)).build());
        }
        
        return item;
    }
}