package com.seatmap.common.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.TierDefinition;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TierRepository extends DynamoDbRepository<TierDefinition> {
    
    public TierRepository(DynamoDbClient dynamoDbClient, String tableName) {
        super(dynamoDbClient, tableName);
    }
    
    @Override
    protected Class<TierDefinition> getEntityClass() {
        return TierDefinition.class;
    }
    
    @Override
    protected String getHashKeyName() {
        return "tierId";
    }
    
    public Optional<TierDefinition> findByTierName(String tierName) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":tierName", AttributeValue.builder().s(tierName).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("tier-name-index")
                    .keyConditionExpression("tierName = :tierName")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            if (!response.items().isEmpty()) {
                return Optional.of(fromAttributeValueMap(response.items().get(0)));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find tier by name: " + e.getMessage());
        }
    }
    
    public List<TierDefinition> findByRegion(String region) throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":region", AttributeValue.builder().s(region).build());
            expressionAttributeValues.put(":active", AttributeValue.builder().bool(true).build());
            
            QueryRequest request = QueryRequest.builder()
                    .tableName(tableName)
                    .indexName("region-index")
                    .keyConditionExpression("region = :region")
                    .filterExpression("active = :active")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            QueryResponse response = dynamoDbClient.query(request);
            
            return response.items().stream()
                    .map(item -> {
                        try {
                            return fromAttributeValueMap(item);
                        } catch (SeatmapException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find tiers by region: " + e.getMessage());
        }
    }
    
    public List<TierDefinition> findAllActive() throws SeatmapException {
        try {
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            expressionAttributeValues.put(":active", AttributeValue.builder().bool(true).build());
            
            ScanRequest request = ScanRequest.builder()
                    .tableName(tableName)
                    .filterExpression("active = :active")
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
                    
            ScanResponse response = dynamoDbClient.scan(request);
            
            return response.items().stream()
                    .map(item -> {
                        try {
                            return fromAttributeValueMap(item);
                        } catch (SeatmapException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find active tiers: " + e.getMessage());
        }
    }
    
    public void saveTier(TierDefinition tier) throws SeatmapException {
        tier.updateTimestamp();
        save(tier);
    }
}