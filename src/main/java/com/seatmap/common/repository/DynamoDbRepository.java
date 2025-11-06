package com.seatmap.common.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.exception.SeatmapException;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public abstract class DynamoDbRepository<T> {
    protected final DynamoDbClient dynamoDbClient;
    protected final String tableName;
    protected final ObjectMapper objectMapper;
    
    public DynamoDbRepository(DynamoDbClient dynamoDbClient, String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    protected abstract Class<T> getEntityClass();
    
    protected abstract String getHashKeyName();
    
    protected String getRangeKeyName() {
        return null; // Override if table has range key
    }
    
    protected Map<String, AttributeValue> toAttributeValueMap(T entity) throws SeatmapException {
        try {
            String json = objectMapper.writeValueAsString(entity);
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return convertToAttributeValueMap(map);
        } catch (JsonProcessingException e) {
            throw SeatmapException.internalError("Failed to serialize entity: " + e.getMessage());
        }
    }
    
    protected T fromAttributeValueMap(Map<String, AttributeValue> attributeMap) throws SeatmapException {
        try {
            Map<String, Object> map = convertFromAttributeValueMap(attributeMap);
            String json = objectMapper.writeValueAsString(map);
            return objectMapper.readValue(json, getEntityClass());
        } catch (JsonProcessingException e) {
            throw SeatmapException.internalError("Failed to deserialize entity: " + e.getMessage());
        }
    }
    
    private Map<String, AttributeValue> convertToAttributeValueMap(Map<String, Object> map) {
        Map<String, AttributeValue> attributeMap = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() != null) {
                attributeMap.put(entry.getKey(), toAttributeValue(entry.getValue()));
            }
        }
        
        return attributeMap;
    }
    
    private AttributeValue toAttributeValue(Object value) {
        if (value == null) {
            return AttributeValue.builder().nul(true).build();
        } else if (value instanceof String) {
            return AttributeValue.builder().s((String) value).build();
        } else if (value instanceof Number) {
            return AttributeValue.builder().n(value.toString()).build();
        } else if (value instanceof Boolean) {
            return AttributeValue.builder().bool((Boolean) value).build();
        } else if (value instanceof List) {
            List<AttributeValue> list = new ArrayList<>();
            for (Object item : (List<?>) value) {
                list.add(toAttributeValue(item));
            }
            return AttributeValue.builder().l(list).build();
        } else if (value instanceof Map) {
            Map<String, AttributeValue> map = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                map.put(entry.getKey().toString(), toAttributeValue(entry.getValue()));
            }
            return AttributeValue.builder().m(map).build();
        } else {
            return AttributeValue.builder().s(value.toString()).build();
        }
    }
    
    private Map<String, Object> convertFromAttributeValueMap(Map<String, AttributeValue> attributeMap) {
        Map<String, Object> map = new HashMap<>();
        
        for (Map.Entry<String, AttributeValue> entry : attributeMap.entrySet()) {
            map.put(entry.getKey(), fromAttributeValue(entry.getValue()));
        }
        
        return map;
    }
    
    private Object fromAttributeValue(AttributeValue attributeValue) {
        if (attributeValue.s() != null) {
            return attributeValue.s();
        } else if (attributeValue.n() != null) {
            String numberStr = attributeValue.n();
            if (numberStr.contains(".")) {
                return Double.parseDouble(numberStr);
            } else {
                try {
                    return Integer.parseInt(numberStr);
                } catch (NumberFormatException e) {
                    return Long.parseLong(numberStr);
                }
            }
        } else if (attributeValue.bool() != null) {
            return attributeValue.bool();
        } else if (attributeValue.l() != null) {
            List<Object> list = new ArrayList<>();
            for (AttributeValue item : attributeValue.l()) {
                list.add(fromAttributeValue(item));
            }
            return list;
        } else if (attributeValue.m() != null) {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, AttributeValue> entry : attributeValue.m().entrySet()) {
                map.put(entry.getKey(), fromAttributeValue(entry.getValue()));
            }
            return map;
        } else {
            return null;
        }
    }
    
    public void save(T entity) throws SeatmapException {
        try {
            Map<String, AttributeValue> item = toAttributeValueMap(entity);
            
            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();
                    
            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to save entity: " + e.getMessage());
        }
    }
    
    public Optional<T> findByKey(String hashKey) throws SeatmapException {
        return findByKey(hashKey, null);
    }
    
    public Optional<T> findByKey(String hashKey, String rangeKey) throws SeatmapException {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(getHashKeyName(), AttributeValue.builder().s(hashKey).build());
            
            if (rangeKey != null && getRangeKeyName() != null) {
                key.put(getRangeKeyName(), AttributeValue.builder().s(rangeKey).build());
            }
            
            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();
                    
            GetItemResponse response = dynamoDbClient.getItem(request);
            
            if (response.hasItem()) {
                return Optional.of(fromAttributeValueMap(response.item()));
            } else {
                return Optional.empty();
            }
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to find entity: " + e.getMessage());
        }
    }
    
    public void delete(String hashKey) throws SeatmapException {
        delete(hashKey, null);
    }
    
    public void delete(String hashKey, String rangeKey) throws SeatmapException {
        try {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put(getHashKeyName(), AttributeValue.builder().s(hashKey).build());
            
            if (rangeKey != null && getRangeKeyName() != null) {
                key.put(getRangeKeyName(), AttributeValue.builder().s(rangeKey).build());
            }
            
            DeleteItemRequest request = DeleteItemRequest.builder()
                    .tableName(tableName)
                    .key(key)
                    .build();
                    
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            throw SeatmapException.internalError("Failed to delete entity: " + e.getMessage());
        }
    }
}