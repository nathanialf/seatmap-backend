package com.seatmap.api.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.TierDefinition;
import com.seatmap.common.repository.TierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TierHandlerTest {
    
    private TierHandler handler;
    
    @Mock
    private TierRepository mockTierRepository;
    
    @Mock
    private Context mockContext;
    
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() throws Exception {
        handler = new TierHandler();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Use reflection to inject the mock repository
        Field repositoryField = TierHandler.class.getDeclaredField("tierRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(handler, mockTierRepository);
    }
    
    private TierDefinition createTestTier(String tierName, boolean publiclyAccessible, boolean active) {
        TierDefinition tier = new TierDefinition();
        tier.setTierId("tier_" + tierName.toLowerCase());
        tier.setTierName(tierName);
        tier.setDisplayName(tierName + " Plan");
        tier.setDescription("Test " + tierName + " tier");
        tier.setMaxBookmarks(tierName.equals("FREE") ? 0 : 50);
        tier.setMaxSeatmapCalls(tierName.equals("FREE") ? 10 : 1000);
        tier.setPriceUsd(tierName.equals("FREE") ? BigDecimal.ZERO : new BigDecimal("9.99"));
        tier.setBillingType(tierName.equals("FREE") ? "free" : "monthly");
        tier.setCanDowngrade(true);
        tier.setPubliclyAccessible(publiclyAccessible);
        tier.setRegion("US");
        tier.setActive(active);
        tier.setCreatedAt(Instant.now());
        tier.setUpdatedAt(Instant.now());
        return tier;
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
    void handleGetAllTiers_WhenTiersExist_ShouldReturnPublicTiers() throws Exception {
        // Arrange
        List<TierDefinition> allTiers = Arrays.asList(
            createTestTier("FREE", true, true),
            createTestTier("PRO", true, true),
            createTestTier("DEV", false, true), // Not publicly accessible
            createTestTier("BUSINESS", true, true)
        );
        
        when(mockTierRepository.findAllActive()).thenReturn(allTiers);
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("\"total\":3")); // Only public tiers
        assertFalse(response.getBody().contains("DEV")); // Dev tier should be filtered out
        assertTrue(response.getBody().contains("FREE"));
        assertTrue(response.getBody().contains("PRO"));
        assertTrue(response.getBody().contains("BUSINESS"));
        
        verify(mockTierRepository).findAllActive();
    }
    
    @Test
    void handleGetAllTiers_WhenNoTiers_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(mockTierRepository.findAllActive()).thenReturn(new ArrayList<>());
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("\"total\":0"));
        
        verify(mockTierRepository).findAllActive();
    }
    
    @Test
    void handleGetTierByName_WhenTierExists_ShouldReturnTier() throws Exception {
        // Arrange
        TierDefinition tier = createTestTier("PRO", true, true);
        when(mockTierRepository.findByTierName("PRO")).thenReturn(Optional.of(tier));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/PRO");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("\"tierName\":\"PRO\""));
        assertTrue(response.getBody().contains("\"displayName\":\"PRO Plan\""));
        
        verify(mockTierRepository).findByTierName("PRO");
    }
    
    @Test
    void handleGetTierByName_WhenTierNotFound_ShouldReturn404() throws Exception {
        // Arrange
        when(mockTierRepository.findByTierName("NONEXISTENT")).thenReturn(Optional.empty());
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/NONEXISTENT");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Tier not found\""));
        
        verify(mockTierRepository).findByTierName("NONEXISTENT");
    }
    
    @Test
    void handleGetTierByName_WhenTierNotActive_ShouldReturn404() throws Exception {
        // Arrange
        TierDefinition inactiveTier = createTestTier("INACTIVE", true, false);
        when(mockTierRepository.findByTierName("INACTIVE")).thenReturn(Optional.of(inactiveTier));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/INACTIVE");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Tier not found\""));
        
        verify(mockTierRepository).findByTierName("INACTIVE");
    }
    
    @Test
    void handleGetTierByName_WhenTierNotPublic_ShouldReturn404() throws Exception {
        // Arrange
        TierDefinition privateTier = createTestTier("PRIVATE", false, true);
        when(mockTierRepository.findByTierName("PRIVATE")).thenReturn(Optional.of(privateTier));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/PRIVATE");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Tier not found\""));
        
        verify(mockTierRepository).findByTierName("PRIVATE");
    }
    
    @Test
    void handleGetTierByName_WhenEmptyTierName_ShouldReturn400() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Tier name is required\""));
        
        verify(mockTierRepository, never()).findByTierName(anyString());
    }
    
    @Test
    void handleGetTierByName_CaseInsensitive_ShouldWork() throws Exception {
        // Arrange
        TierDefinition tier = createTestTier("PRO", true, true);
        when(mockTierRepository.findByTierName("PRO")).thenReturn(Optional.of(tier));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers/pro");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        
        verify(mockTierRepository).findByTierName("PRO"); // Should convert to uppercase
    }
    
    @Test
    void handleRequest_WhenUnsupportedHttpMethod_ShouldReturn404() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequest("POST", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Endpoint not found\""));
    }
    
    @Test
    void handleRequest_WhenUnsupportedPath_ShouldReturn404() throws Exception {
        // Arrange
        APIGatewayProxyRequestEvent event = createRequest("GET", "/invalid-path");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Endpoint not found\""));
    }
    
    @Test
    void handleRequest_WhenRepositoryThrowsException_ShouldReturn500() throws Exception {
        // Arrange
        when(mockTierRepository.findAllActive()).thenThrow(new SeatmapException("DATABASE_ERROR", "Database error", 500));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Database error\""));
        
        verify(mockTierRepository).findAllActive();
    }
    
    @Test
    void handleRequest_WhenUnexpectedExceptionOccurs_ShouldReturn500() throws Exception {
        // Arrange
        when(mockTierRepository.findAllActive()).thenThrow(new RuntimeException("Unexpected error"));
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(500, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":false"));
        assertTrue(response.getBody().contains("\"message\":\"Internal server error\""));
        
        verify(mockTierRepository).findAllActive();
    }
    
    @Test
    void handleGetAllTiers_WithRegionFilter_ShouldReturnRegionSpecificTiers() throws Exception {
        // Arrange
        List<TierDefinition> usTiers = Arrays.asList(
            createTestTier("FREE", true, true),
            createTestTier("PRO", true, true)
        );
        
        when(mockTierRepository.findByRegion("US")).thenReturn(usTiers);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "US");
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("\"total\":2"));
        assertTrue(response.getBody().contains("\"region\":\"US\""));
        assertTrue(response.getBody().contains("FREE"));
        assertTrue(response.getBody().contains("PRO"));
        
        verify(mockTierRepository).findByRegion("US");
        verify(mockTierRepository, never()).findAllActive();
    }
    
    @Test
    void handleGetAllTiers_WithRegionFilter_CaseInsensitive() throws Exception {
        // Arrange
        List<TierDefinition> euTiers = Arrays.asList(
            createTestTier("PRO", true, true)
        );
        
        when(mockTierRepository.findByRegion("EU")).thenReturn(euTiers);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "eu");  // lowercase
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"region\":\"EU\""));  // Should be uppercase in response
        
        verify(mockTierRepository).findByRegion("EU");  // Should convert to uppercase
    }
    
    @Test
    void handleGetAllTiers_WithRegionFilter_FiltersInactiveTiers() throws Exception {
        // Arrange
        List<TierDefinition> regionTiers = Arrays.asList(
            createTestTier("FREE", true, true),     // Active and public
            createTestTier("PRO", true, false),    // Inactive
            createTestTier("DEV", false, true)     // Not public
        );
        
        when(mockTierRepository.findByRegion("US")).thenReturn(regionTiers);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "US");
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":1")); // Only FREE should be returned
        assertTrue(response.getBody().contains("FREE"));
        assertFalse(response.getBody().contains("PRO"));  // Inactive tier filtered out
        assertFalse(response.getBody().contains("DEV"));  // Non-public tier filtered out
        
        verify(mockTierRepository).findByRegion("US");
    }
    
    @Test
    void handleGetAllTiers_WithEmptyRegionFilter_ShouldReturnAllTiers() throws Exception {
        // Arrange
        List<TierDefinition> allTiers = Arrays.asList(
            createTestTier("FREE", true, true),
            createTestTier("PRO", true, true)
        );
        
        when(mockTierRepository.findAllActive()).thenReturn(allTiers);
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "  ");  // Empty/whitespace
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"total\":2"));
        
        // Parse JSON to properly check for absence of region field in data object
        try {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode dataNode = responseJson.get("data");
            assertFalse(dataNode.has("region"), "Data object should not have region field when region parameter is empty");
        } catch (Exception e) {
            fail("Failed to parse response JSON: " + e.getMessage());
        }
        
        verify(mockTierRepository).findAllActive();
        verify(mockTierRepository, never()).findByRegion(anyString());
    }
    
    @Test
    void handleGetAllTiers_WithRegionFilter_NoTiersFound_ShouldReturnEmptyList() throws Exception {
        // Arrange
        when(mockTierRepository.findByRegion("NONEXISTENT")).thenReturn(new ArrayList<>());
        
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("region", "NONEXISTENT");
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers", queryParams);
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getBody().contains("\"success\":true"));
        assertTrue(response.getBody().contains("\"total\":0"));
        assertTrue(response.getBody().contains("\"region\":\"NONEXISTENT\""));
        
        verify(mockTierRepository).findByRegion("NONEXISTENT");
    }
    
    @Test
    void responsesIncludeCorsHeaders() throws Exception {
        // Arrange
        when(mockTierRepository.findAllActive()).thenReturn(new ArrayList<>());
        
        APIGatewayProxyRequestEvent event = createRequest("GET", "/tiers");
        
        // Act
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, mockContext);
        
        // Assert
        Map<String, String> headers = response.getHeaders();
        assertNotNull(headers);
        assertEquals("*", headers.get("Access-Control-Allow-Origin"));
        assertEquals("Content-Type,X-Amz-Date,Authorization,X-Api-Key", headers.get("Access-Control-Allow-Headers"));
        assertEquals("GET,OPTIONS", headers.get("Access-Control-Allow-Methods"));
        assertEquals("application/json", headers.get("Content-Type"));
    }
}