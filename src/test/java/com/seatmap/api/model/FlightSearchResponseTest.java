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

    // PaginationInfo Tests
    @Test
    void paginationInfo_DefaultConstructor_CreatesObjectWithDefaults() {
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo();
        
        assertEquals(0, pagination.getOffset());
        assertEquals(0, pagination.getLimit());
        assertEquals(0, pagination.getTotal());
        assertFalse(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_ConstructorWithAllFields_SetsAllFields() {
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(20, 10, 100, true, true);
        
        assertEquals(20, pagination.getOffset());
        assertEquals(10, pagination.getLimit());
        assertEquals(100, pagination.getTotal());
        assertTrue(pagination.isHasNext());
        assertTrue(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_SettersAndGetters_WorkCorrectly() {
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo();
        
        pagination.setOffset(30);
        pagination.setLimit(15);
        pagination.setTotal(75);
        pagination.setHasNext(true);
        pagination.setHasPrevious(false);
        
        assertEquals(30, pagination.getOffset());
        assertEquals(15, pagination.getLimit());
        assertEquals(75, pagination.getTotal());
        assertTrue(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_FirstPage_HasCorrectFlags() {
        // First page should have hasNext=true (if there are more), hasPrevious=false
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(0, 10, 50, true, false);
        
        assertEquals(0, pagination.getOffset());
        assertTrue(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_MiddlePage_HasCorrectFlags() {
        // Middle page should have both hasNext=true and hasPrevious=true
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(20, 10, 50, true, true);
        
        assertEquals(20, pagination.getOffset());
        assertTrue(pagination.isHasNext());
        assertTrue(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_LastPage_HasCorrectFlags() {
        // Last page should have hasNext=false, hasPrevious=true
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(40, 10, 50, false, true);
        
        assertEquals(40, pagination.getOffset());
        assertFalse(pagination.isHasNext());
        assertTrue(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_OnlyPage_HasCorrectFlags() {
        // Only page (single page result) should have hasNext=false, hasPrevious=false
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(0, 10, 5, false, false);
        
        assertEquals(0, pagination.getOffset());
        assertFalse(pagination.isHasNext());
        assertFalse(pagination.isHasPrevious());
    }

    @Test
    void paginationInfo_UnknownTotal_WorksCorrectly() {
        // When total is unknown (-1), pagination should still work based on result count
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(20, 10, -1, true, true);
        
        assertEquals(20, pagination.getOffset());
        assertEquals(10, pagination.getLimit());
        assertEquals(-1, pagination.getTotal());
        assertTrue(pagination.isHasNext());
        assertTrue(pagination.isHasPrevious());
    }

    @Test
    void searchMetadata_WithPagination_WorksCorrectly() {
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(10, 10, 50, true, true);
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata(10, "AMADEUS");
        metadata.setPagination(pagination);
        
        assertEquals(pagination, metadata.getPagination());
        assertEquals(10, metadata.getPagination().getOffset());
        assertEquals(10, metadata.getPagination().getLimit());
    }

    @Test
    void searchMetadata_PaginationCanBeNull() {
        FlightSearchResponse.SearchMetadata metadata = new FlightSearchResponse.SearchMetadata();
        metadata.setPagination(null);
        
        assertNull(metadata.getPagination());
    }

    @Test
    void paginationInfo_BoundaryValues_WorkCorrectly() {
        // Test with boundary values for pagination
        FlightSearchResponse.PaginationInfo pagination = new FlightSearchResponse.PaginationInfo(100, 20, 120, false, true);
        
        assertEquals(100, pagination.getOffset()); // Max offset
        assertEquals(20, pagination.getLimit());   // Max results per page
        assertFalse(pagination.isHasNext());       // Last page
        assertTrue(pagination.isHasPrevious());    // Has previous pages
    }
}