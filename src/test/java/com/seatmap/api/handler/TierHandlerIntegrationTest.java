package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.model.TierDefinition;
import com.seatmap.common.repository.TierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TierHandlerIntegrationTest {
    
    private TierHandler handler;
    private TierRepository tierRepository;
    
    @Mock
    private DynamoDbClient mockDynamoDbClient;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws Exception {
        handler = new TierHandler();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Create repository with mocked DynamoDB client
        tierRepository = new TierRepository(mockDynamoDbClient, "test-tiers-table");
        
        // Use reflection to inject the repository
        Field repositoryField = TierHandler.class.getDeclaredField("tierRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(handler, tierRepository);
    }
    
    private Map<String, AttributeValue> createTierAttributeMap(String tierName, boolean publiclyAccessible, boolean active) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("tierId", AttributeValue.builder().s("tier_" + tierName.toLowerCase()).build());
        item.put("tierName", AttributeValue.builder().s(tierName).build());
        item.put("displayName", AttributeValue.builder().s(tierName + " Plan").build());
        item.put("description", AttributeValue.builder().s("Test " + tierName + " tier").build());
        item.put("maxBookmarks", AttributeValue.builder().n(tierName.equals("FREE") ? "0" : "50").build());
        item.put("maxSeatmapCalls", AttributeValue.builder().n(tierName.equals("FREE") ? "10" : "1000").build());
        item.put("priceUsd", AttributeValue.builder().n(tierName.equals("FREE") ? "0" : "9.99").build());
        item.put("billingType", AttributeValue.builder().s(tierName.equals("FREE") ? "free" : "monthly").build());
        item.put("canDowngrade", AttributeValue.builder().bool(true).build());
        item.put("publiclyAccessible", AttributeValue.builder().bool(publiclyAccessible).build());
        item.put("region", AttributeValue.builder().s("US").build());
        item.put("active", AttributeValue.builder().bool(active).build());
        item.put("createdAt", AttributeValue.builder().s(Instant.now().toString()).build());
        item.put("updatedAt", AttributeValue.builder().s(Instant.now().toString()).build());
        return item;
    }
    
    private APIGatewayProxyRequestEvent createRequest(String httpMethod, String path) {
        return createRequest(httpMethod, path, null);
    }
    
    private APIGatewayProxyRequestEvent createRequest(String httpMethod, String path, Map<String, String> queryParams) {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent();
        event.setHttpMethod(httpMethod);
        event.setPath(path);
        event.setHeaders(new HashMap<>());
        event.setQueryStringParameters(queryParams);
        return event;
    }
    
    @Test
    void integrationTest_GetAllTiers_WithMockedDynamoDB() throws Exception {
        // Arrange
        ScanResponse scanResponse = ScanResponse.builder()
            .items(
                createTierAttributeMap("FREE", true, true),
                createTierAttributeMap("PRO", true, true),
                createTierAttributeMap("DEV", false, true), // Not publicly accessible
                createTierAttributeMap("BUSINESS", true, true)
            )
            .build();
        
        when(mockDynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertTrue(responseJson.get("success").asBoolean());
        assertEquals(3, responseJson.get("data").get("total").asInt()); // Only public tiers
        
        JsonNode tiers = responseJson.get("data").get("tiers");
        assertEquals(3, tiers.size());
        
        // Verify tier names (should not include DEV tier)
        boolean foundFree = false, foundPro = false, foundBusiness = false, foundDev = false;
        for (JsonNode tier : tiers) {
            String tierName = tier.get("tierName").asText();
            switch (tierName) {
                case "FREE": foundFree = true; break;
                case "PRO": foundPro = true; break;
                case "BUSINESS": foundBusiness = true; break;
                case "DEV": foundDev = true; break;
            }
        }
        
        assertTrue(foundFree, "Should include FREE tier");
        assertTrue(foundPro, "Should include PRO tier");
        assertTrue(foundBusiness, "Should include BUSINESS tier");
        assertFalse(foundDev, "Should not include DEV tier (not publicly accessible)");
        
        verify(mockDynamoDbClient).scan(any(ScanRequest.class));
    }
    
    @Test
    void integrationTest_GetTierByName_WithMockedDynamoDB() throws Exception {
        // Arrange
        QueryResponse queryResponse = QueryResponse.builder()
            .items(createTierAttributeMap("PRO", true, true))
            .build();
        
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/PRO");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertTrue(responseJson.get("success").asBoolean());
        
        JsonNode tierData = responseJson.get("data");
        assertEquals("PRO", tierData.get("tierName").asText());
        assertEquals("PRO Plan", tierData.get("displayName").asText());
        assertEquals("Test PRO tier", tierData.get("description").asText());
        assertEquals(50, tierData.get("maxBookmarks").asInt());
        assertEquals(1000, tierData.get("maxSeatmapCalls").asInt());
        assertEquals(9.99, tierData.get("priceUsd").asDouble(), 0.01);
        assertEquals("monthly", tierData.get("billingType").asText());
        assertTrue(tierData.get("canDowngrade").asBoolean());
        assertTrue(tierData.get("publiclyAccessible").asBoolean());
        assertEquals("US", tierData.get("region").asText());
        assertTrue(tierData.get("active").asBoolean());
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void integrationTest_GetTierByName_TierNotFound() throws Exception {
        // Arrange
        QueryResponse emptyResponse = QueryResponse.builder()
            .items() // Empty list
            .build();
        
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(emptyResponse);
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/NONEXISTENT");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertFalse(responseJson.get("success").asBoolean());
        assertEquals("Tier not found", responseJson.get("message").asText());
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void integrationTest_DynamoDBException_ShouldReturn500() throws Exception {
        // Arrange
        when(mockDynamoDbClient.scan(any(ScanRequest.class)))
            .thenThrow(DynamoDbException.builder().message("DynamoDB error").build());
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertFalse(responseJson.get("success").asBoolean());
        assertTrue(responseJson.get("message").asText().contains("Failed to find active tiers"));
        
        verify(mockDynamoDbClient).scan(any(ScanRequest.class));
    }
    
    @Test
    void integrationTest_GetTiersByRegion_WithMockedDynamoDB() throws Exception {
        // Arrange
        QueryResponse queryResponse = QueryResponse.builder()
            .items(
                createTierAttributeMap("FREE", true, true),
                createTierAttributeMap("PRO", true, true),
                createTierAttributeMap("DEV", false, true) // Not publicly accessible
            )
            .build();
        
        when(mockDynamoDbClient.query(any(QueryRequest.class))).thenReturn(queryResponse);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "US");
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        assertTrue(responseJson.get("success").asBoolean());
        assertEquals("US", responseJson.get("data").get("region").asText());
        assertEquals(2, responseJson.get("data").get("total").asInt()); // Only public tiers
        
        JsonNode tiers = responseJson.get("data").get("tiers");
        assertEquals(2, tiers.size());
        
        // Verify tier names (should not include DEV tier)
        boolean foundFree = false, foundPro = false, foundDev = false;
        for (JsonNode tier : tiers) {
            String tierName = tier.get("tierName").asText();
            switch (tierName) {
                case "FREE": foundFree = true; break;
                case "PRO": foundPro = true; break;
                case "DEV": foundDev = true; break;
            }
        }
        
        assertTrue(foundFree, "Should include FREE tier");
        assertTrue(foundPro, "Should include PRO tier");
        assertFalse(foundDev, "Should not include DEV tier (not publicly accessible)");
        
        verify(mockDynamoDbClient).query(any(QueryRequest.class));
    }
    
    @Test
    void integrationTest_ResponseSerialization_ShouldIncludeAllFields() throws Exception {
        // Arrange
        ScanResponse scanResponse = ScanResponse.builder()
            .items(createTierAttributeMap("PRO", true, true))
            .build();
        
        when(mockDynamoDbClient.scan(any(ScanRequest.class))).thenReturn(scanResponse);
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        
        JsonNode responseJson = objectMapper.readTree(response.getBody());
        JsonNode tier = responseJson.get("data").get("tiers").get(0);
        
        // Verify all expected fields are present
        assertNotNull(tier.get("tierId"));
        assertNotNull(tier.get("tierName"));
        assertNotNull(tier.get("displayName"));
        assertNotNull(tier.get("description"));
        assertNotNull(tier.get("maxBookmarks"));
        assertNotNull(tier.get("maxSeatmapCalls"));
        assertNotNull(tier.get("priceUsd"));
        assertNotNull(tier.get("billingType"));
        assertNotNull(tier.get("canDowngrade"));
        assertNotNull(tier.get("publiclyAccessible"));
        assertNotNull(tier.get("region"));
        assertNotNull(tier.get("active"));
        assertNotNull(tier.get("createdAt"));
        assertNotNull(tier.get("updatedAt"));
        
        verify(mockDynamoDbClient).scan(any(ScanRequest.class));
    }
}