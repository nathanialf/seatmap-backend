package com.seatmap.api.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FlightSearchResponseTest {

    @Test
    void defaultConstructor_CreatesEmptyResponse() {
        FlightSearchResponse response = new FlightSearchResponse();
        
        assertNull(response.getData());
        assertNull(response.getMeta());
        assertNull(response.getDictionaries());
    }

    @Test
    void constructorWithDataAndMeta_SetsFields() {
        // Arrange
        List<FlightSearchResult> data = Arrays.asList(
            new FlightSearchResult(),
            new FlightSearchResult()
        );
        FlightSearchResponse.SearchMetadata meta = new FlightSearchResponse.SearchMetadata(2, "AMADEUS,SABRE");

        // Act
        FlightSearchResponse response = new FlightSearchResponse(data, meta);

        // Assert
        assertEquals(data, response.getData());
        assertEquals(meta, response.getMeta());
        assertNull(response.getDictionaries());
    }

    @Test
    void constructorWithAllFields_SetsAllFields() {
        // Arrange
        List<FlightSearchResult> data = Arrays.asList(new FlightSearchResult());
        FlightSearchResponse.SearchMetadata meta = new FlightSearchResponse.SearchMetadata(1, "AMADEUS");
        Object dictionaries = "test-dictionaries";

        // Act
        FlightSearchResponse response = new FlightSearchResponse(data, meta, dictionaries);

        // Assert
        assertEquals(data, response.getData());
        assertEquals(meta, response.getMeta());
        assertEquals(dictionaries, response.getDictionaries());
    }

    @Test
    void settersAndGetters_WorkCorrectly() {
        // Arrange
        FlightSearchResponse response = new FlightSearchResponse();
        List<FlightSearchResult> data = Arrays.asList(new FlightSearchResult());
        FlightSearchResponse.SearchMetadata meta = new FlightSearchResponse.SearchMetadata(1, "SABRE");
        Object dictionaries = "test-dict";

        // Act
        response.setData(data);
        response.setMeta(meta);
        response.setDictionaries(dictionaries);

        // Assert
        assertEquals(data, response.getData());
        assertEquals(meta, response.getMeta());
        assertEquals(dictionaries, response.getDictionaries());
    }

    @Test
    void searchMetadata_DefaultConstructor_CreatesEmptyObject() {
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata();

        assertEquals(0, metadata.getCount());
        assertNull(metadata.getSources());
        assertNull(metadata.getSearchParams());
    }

    @Test
    void searchMetadata_ConstructorWithCountAndSources_SetsFields() {
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata(5, "AMADEUS");

        assertEquals(5, metadata.getCount());
        assertEquals("AMADEUS", metadata.getSources());
        assertNull(metadata.getSearchParams());
    }

    @Test
    void searchMetadata_ConstructorWithAllFields_SetsAllFields() {
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata(3, "SABRE", "LAX-SFO-2025-01-15");

        assertEquals(3, metadata.getCount());
        assertEquals("SABRE", metadata.getSources());
        assertEquals("LAX-SFO-2025-01-15", metadata.getSearchParams());
    }

    @Test
    void searchMetadata_SettersAndGetters_WorkCorrectly() {
        // Arrange
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata();

        // Act
        metadata.setCount(10);
        metadata.setSources("AMADEUS,SABRE");
        metadata.setSearchParams("SFO-LAX-2025-02-01");

        // Assert
        assertEquals(10, metadata.getCount());
        assertEquals("AMADEUS,SABRE", metadata.getSources());
        assertEquals("SFO-LAX-2025-02-01", metadata.getSearchParams());
    }

    @Test
    void searchMetadata_HandlesNullValues() {
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata();
        
        // Test setting null values
        metadata.setSources(null);
        metadata.setSearchParams(null);
        
        assertNull(metadata.getSources());
        assertNull(metadata.getSearchParams());
        assertEquals(0, metadata.getCount()); // int default
    }

    @Test
    void response_HandlesNullValues() {
        FlightSearchResponse response = new FlightSearchResponse();
        
        // Test setting null values
        response.setData(null);
        response.setMeta(null);
        response.setDictionaries(null);
        
        assertNull(response.getData());
        assertNull(response.getMeta());
        assertNull(response.getDictionaries());
    }
}