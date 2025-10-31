package com.seatmap.common.repository;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.TierDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TierRepositoryTest {
    
    @Mock
    private DynamoDbClient dynamoDbClient;
    
    private TierRepository tierRepository;
    private TierDefinition testTier;
    
    @BeforeEach
    void setUp() {
        tierRepository = new TierRepository(dynamoDbClient, "test-table");
        
        testTier = new TierDefinition();
        testTier.setTierId("free-us-2025");
        testTier.setTierName("FREE");
        testTier.setDisplayName("Free Tier");
        testTier.setDescription("Basic free tier");
        testTier.setMaxBookmarks(5);
        testTier.setMaxSeatmapCalls(10);
        testTier.setPriceUsd(BigDecimal.ZERO);
        testTier.setBillingType("free");
        testTier.setRegion("US");
        testTier.setActive(true);
    }
    
    @Test
    void getEntityClass_ShouldReturnTierDefinitionClass() {
        assertEquals(TierDefinition.class, tierRepository.getEntityClass());
    }
    
    @Test
    void getHashKeyName_ShouldReturnTierId() {
        assertEquals("tierId", tierRepository.getHashKeyName());
    }
    
    @Test
    void findByTierName_WhenTierExists_ShouldReturnTier() throws SeatmapException {
        // Given
        Map<String, AttributeValue> item = createMockAttributeMap();
        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item))
            .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<TierDefinition> result = tierRepository.findByTierName("FREE");
        
        // Then
        assertTrue(result.isPresent());
        assertEquals("FREE", result.get().getTierName());
        
        verify(dynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void findByTierName_WhenTierNotExists_ShouldReturnEmpty() throws SeatmapException {
        // Given
        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of())
            .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        Optional<TierDefinition> result = tierRepository.findByTierName("NONEXISTENT");
        
        // Then
        assertFalse(result.isPresent());
        
        verify(dynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void findByTierName_WhenDynamoDbException_ShouldThrowSeatmapException() {
        // Given
        when(dynamoDbClient.query(any(QueryRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> tierRepository.findByTierName("FREE"));
        
        assertTrue(exception.getMessage().contains("Failed to find tier by name"));
        assertTrue(exception.getMessage().contains("DynamoDB error"));
    }
    
    @Test
    void findByRegion_WhenTiersExist_ShouldReturnActiveTiers() throws SeatmapException {
        // Given
        Map<String, AttributeValue> item1 = createMockAttributeMap();
        Map<String, AttributeValue> item2 = createMockAttributeMap();
        item2.put("tierName", AttributeValue.builder().s("PRO").build());
        
        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of(item1, item2))
            .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        List<TierDefinition> result = tierRepository.findByRegion("US");
        
        // Then
        assertEquals(2, result.size());
        verify(dynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void findByRegion_WhenNoTiers_ShouldReturnEmptyList() throws SeatmapException {
        // Given
        QueryResponse queryResponse = QueryResponse.builder()
            .items(List.of())
            .build();
        
        when(dynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        // When
        List<TierDefinition> result = tierRepository.findByRegion("EU");
        
        // Then
        assertTrue(result.isEmpty());
    }
    
    @Test
    void findAllActive_WhenTiersExist_ShouldReturnActiveTiers() throws SeatmapException {
        // Given
        Map<String, AttributeValue> item = createMockAttributeMap();
        ScanResponse scanResponse = ScanResponse.builder()
            .items(List.of(item))
            .build();
        
        when(dynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);
        
        // When
        List<TierDefinition> result = tierRepository.findAllActive();
        
        // Then
        assertEquals(1, result.size());
        verify(dynamoDbClient).scan(any(ScanRequest.class));
    }
    
    @Test
    void findAllActive_WhenDynamoDbException_ShouldThrowSeatmapException() {
        // Given
        when(dynamoDbClient.scan(any(ScanRequest.class)))
            .thenThrow(DynamoDbException.builder().message("Scan failed").build());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, 
            () -> tierRepository.findAllActive());
        
        assertTrue(exception.getMessage().contains("Failed to find active tiers"));
    }
    
    @Test
    void saveTier_ShouldUpdateTimestampAndCallSave() throws SeatmapException {
        // Given
        TierRepository spyRepository = spy(tierRepository);
        doNothing().when(spyRepository).save(any(TierDefinition.class));
        
        // When
        spyRepository.saveTier(testTier);
        
        // Then
        assertNotNull(testTier.getUpdatedAt());
        verify(spyRepository).save(testTier);
    }
    
    private Map<String, AttributeValue> createMockAttributeMap() {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("tierId", AttributeValue.builder().s("free-us-2025").build());
        item.put("tierName", AttributeValue.builder().s("FREE").build());
        item.put("displayName", AttributeValue.builder().s("Free Tier").build());
        item.put("description", AttributeValue.builder().s("Basic free tier").build());
        item.put("maxBookmarks", AttributeValue.builder().n("5").build());
        item.put("maxSeatmapCalls", AttributeValue.builder().n("10").build());
        item.put("priceUsd", AttributeValue.builder().n("0.00").build());
        item.put("billingType", AttributeValue.builder().s("free").build());
        item.put("canDowngrade", AttributeValue.builder().bool(false).build());
        item.put("region", AttributeValue.builder().s("US").build());
        item.put("active", AttributeValue.builder().bool(true).build());
        item.put("createdAt", AttributeValue.builder().n("1700000000").build());
        item.put("updatedAt", AttributeValue.builder().n("1700000000").build());
        return item;
    }
}