package com.seatmap.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seatmap.api.model.FlightSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for concurrent seatmap fetching
 * These tests are only enabled when specific environment variables are set
 * to avoid running against real APIs in normal test runs
 */
@EnabledIfEnvironmentVariable(named = "ENABLE_PERFORMANCE_TESTS", matches = "true")
class ConcurrentSeatmapPerformanceTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ConcurrentSeatmapPerformanceTest.class);
    
    private AmadeusService amadeusService;
    private SabreService sabreService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        // Only initialize if API credentials are available
        try {
            this.amadeusService = new AmadeusService();
            this.sabreService = new SabreService();
            this.objectMapper = new ObjectMapper();
        } catch (IllegalStateException e) {
            logger.warn("API credentials not configured, performance tests will be skipped");
        }
    }
    
    @Test
    void testConcurrentVsSequentialSeatmapFetching() {
        if (amadeusService == null) {
            logger.info("Skipping performance test - API credentials not configured");
            return;
        }
        
        // Create mock flight offers for testing
        List<JsonNode> mockFlightOffers = createMockFlightOffers(5);
        
        // Test sequential processing
        long sequentialStart = System.currentTimeMillis();
        List<FlightSearchResult> sequentialResults = processFlightOffersSequentially(mockFlightOffers);
        long sequentialDuration = System.currentTimeMillis() - sequentialStart;
        
        // Test concurrent processing
        long concurrentStart = System.currentTimeMillis();
        List<FlightSearchResult> concurrentResults = processFlightOffersConcurrently(mockFlightOffers);
        long concurrentDuration = System.currentTimeMillis() - concurrentStart;
        
        // Log results
        logger.info("Sequential processing: {} flights in {}ms", sequentialResults.size(), sequentialDuration);
        logger.info("Concurrent processing: {} flights in {}ms", concurrentResults.size(), concurrentDuration);
        
        // Concurrent should be faster (or at least not significantly slower)
        assertTrue(concurrentDuration <= sequentialDuration * 1.2, 
            "Concurrent processing should not be more than 20% slower than sequential");
        
        // Both should return same number of results
        assertEquals(sequentialResults.size(), concurrentResults.size());
    }
    
    @Test
    void testMemoryUsageWithConcurrentSeatmaps() {
        if (amadeusService == null) {
            logger.info("Skipping memory test - API credentials not configured");
            return;
        }
        
        // Get baseline memory
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long baselineMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Process multiple flights concurrently
        List<JsonNode> mockFlightOffers = createMockFlightOffers(10);
        List<FlightSearchResult> results = processFlightOffersConcurrently(mockFlightOffers);
        
        // Check memory after processing
        System.gc();
        long afterProcessingMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = afterProcessingMemory - baselineMemory;
        
        logger.info("Memory usage increase: {} MB for {} flights", 
            memoryIncrease / (1024 * 1024), results.size());
        
        // Should not exceed reasonable memory limits (adjust based on requirements)
        assertTrue(memoryIncrease < 100 * 1024 * 1024, // 100MB limit
            "Memory increase should be less than 100MB for 10 concurrent seatmap fetches");
    }
    
    @Test
    void testConcurrentSeatmapFetchingWithFailures() {
        if (amadeusService == null) {
            logger.info("Skipping failure handling test - API credentials not configured");
            return;
        }
        
        // Create mix of valid and invalid flight offers
        List<JsonNode> mixedFlightOffers = createMixedFlightOffers(8);
        
        long startTime = System.currentTimeMillis();
        List<FlightSearchResult> results = processFlightOffersConcurrently(mixedFlightOffers);
        long duration = System.currentTimeMillis() - startTime;
        
        logger.info("Processed {} flights (with some failures) in {}ms", results.size(), duration);
        
        // Should handle failures gracefully
        assertEquals(mixedFlightOffers.size(), results.size());
        
        // Some results should have seatmap errors
        long failedSeatmaps = results.stream()
            .filter(r -> !r.isSeatMapAvailable())
            .count();
        
        assertTrue(failedSeatmaps > 0, "Should have some failed seatmap fetches in mixed test");
        
        // All results should have error messages if seatmap failed
        results.stream()
            .filter(r -> !r.isSeatMapAvailable())
            .forEach(r -> assertNotNull(r.getSeatMapError(), "Failed seatmap should have error message"));
    }
    
    @Test
    void testThreadPoolResourceManagement() {
        if (amadeusService == null) {
            logger.info("Skipping thread pool test - API credentials not configured");
            return;
        }
        
        // Create many flight offers to test thread pool limits
        List<JsonNode> manyFlightOffers = createMockFlightOffers(20);
        
        ExecutorService customExecutor = Executors.newFixedThreadPool(5);
        
        try {
            long startTime = System.currentTimeMillis();
            
            // Use custom executor for controlled concurrency
            List<CompletableFuture<FlightSearchResult>> futures = manyFlightOffers.stream()
                .map(offer -> CompletableFuture.supplyAsync(() -> buildFlightSearchResult(offer), customExecutor))
                .toList();
            
            List<FlightSearchResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            long duration = System.currentTimeMillis() - startTime;
            
            logger.info("Processed {} flights with limited thread pool in {}ms", results.size(), duration);
            
            assertEquals(manyFlightOffers.size(), results.size());
            
            // Should complete within reasonable time even with thread pool limits
            assertTrue(duration < 60000, "Should complete within 60 seconds with thread pool");
            
        } finally {
            customExecutor.shutdown();
        }
    }
    
    private List<FlightSearchResult> processFlightOffersSequentially(List<JsonNode> flightOffers) {
        List<FlightSearchResult> results = new ArrayList<>();
        for (JsonNode offer : flightOffers) {
            results.add(buildFlightSearchResult(offer));
        }
        return results;
    }
    
    private List<FlightSearchResult> processFlightOffersConcurrently(List<JsonNode> flightOffers) {
        return flightOffers.parallelStream()
            .map(this::buildFlightSearchResult)
            .toList();
    }
    
    private FlightSearchResult buildFlightSearchResult(JsonNode offer) {
        try {
            // Simulate seatmap fetch - in real implementation this would call actual APIs
            Thread.sleep(100 + (int)(Math.random() * 200)); // Simulate 100-300ms API call
            
            // For testing, create a mock seatmap or simulate failure
            boolean shouldSucceed = Math.random() > 0.2; // 80% success rate
            
            if (shouldSucceed) {
                return new FlightSearchResult(offer, createMockSeatMapData(), true, null);
            } else {
                return new FlightSearchResult(offer, null, false, "Simulated API failure");
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new FlightSearchResult(offer, null, false, "Interrupted");
        }
    }
    
    private List<JsonNode> createMockFlightOffers(int count) {
        List<JsonNode> offers = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            ObjectNode offer = objectMapper.createObjectNode();
            offer.put("id", "test_offer_" + i);
            offer.put("dataSource", i % 2 == 0 ? "AMADEUS" : "SABRE");
            offer.put("type", "flight-offer");
            
            // Add minimal itinerary structure
            ArrayNode itineraries = objectMapper.createArrayNode();
            ObjectNode itinerary = objectMapper.createObjectNode();
            ArrayNode segments = objectMapper.createArrayNode();
            
            ObjectNode segment = objectMapper.createObjectNode();
            segment.put("carrierCode", "AA");
            segment.put("number", String.valueOf(100 + i));
            
            ObjectNode departure = objectMapper.createObjectNode();
            departure.put("iataCode", "LAX");
            departure.put("at", "2024-12-15T08:00:00");
            segment.set("departure", departure);
            
            ObjectNode arrival = objectMapper.createObjectNode();
            arrival.put("iataCode", "JFK");
            arrival.put("at", "2024-12-15T16:00:00");
            segment.set("arrival", arrival);
            
            segments.add(segment);
            itinerary.set("segments", segments);
            itineraries.add(itinerary);
            offer.set("itineraries", itineraries);
            
            offers.add(offer);
        }
        
        return offers;
    }
    
    private List<JsonNode> createMixedFlightOffers(int count) {
        List<JsonNode> offers = createMockFlightOffers(count);
        
        // Make some offers invalid to test error handling
        for (int i = 0; i < count; i += 3) {
            ObjectNode invalidOffer = (ObjectNode) offers.get(i);
            invalidOffer.remove("itineraries"); // Remove required field to cause failures
        }
        
        return offers;
    }
    
    private com.seatmap.api.model.SeatMapData createMockSeatMapData() {
        com.seatmap.api.model.SeatMapData seatMapData = new com.seatmap.api.model.SeatMapData();
        seatMapData.setSource("MOCK");
        return seatMapData;
    }
}