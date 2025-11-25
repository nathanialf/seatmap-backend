package com.seatmap.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class FlightSearchResponse {
    private List<FlightSearchResult> data;
    private SearchMetadata meta;
    private Object dictionaries; // JsonNode for dictionaries from Amadeus
    
    // Default constructor
    public FlightSearchResponse() {}
    
    // Constructor
    public FlightSearchResponse(List<FlightSearchResult> data, SearchMetadata meta) {
        this.data = data;
        this.meta = meta;
    }
    
    // Constructor with dictionaries
    public FlightSearchResponse(List<FlightSearchResult> data, SearchMetadata meta, Object dictionaries) {
        this.data = data;
        this.meta = meta;
        this.dictionaries = dictionaries;
    }
    
    // Getters and setters
    public List<FlightSearchResult> getData() { return data; }
    public void setData(List<FlightSearchResult> data) { this.data = data; }
    
    public SearchMetadata getMeta() { return meta; }
    public void setMeta(SearchMetadata meta) { this.meta = meta; }
    
    public Object getDictionaries() { return dictionaries; }
    public void setDictionaries(Object dictionaries) { this.dictionaries = dictionaries; }
    
    // Inner class for metadata
    public static class SearchMetadata {
        private int count;
        private String sources;
        private String searchParams;
        private PaginationInfo pagination;
        
        // Default constructor
        public SearchMetadata() {}
        
        // Constructor
        public SearchMetadata(int count, String sources) {
            this.count = count;
            this.sources = sources;
        }
        
        // Constructor with search params
        public SearchMetadata(int count, String sources, String searchParams) {
            this.count = count;
            this.sources = sources;
            this.searchParams = searchParams;
        }
        
        // Constructor with pagination
        public SearchMetadata(int count, String sources, String searchParams, PaginationInfo pagination) {
            this.count = count;
            this.sources = sources;
            this.searchParams = searchParams;
            this.pagination = pagination;
        }
        
        // Getters and setters
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public String getSources() { return sources; }
        public void setSources(String sources) { this.sources = sources; }
        
        public String getSearchParams() { return searchParams; }
        public void setSearchParams(String searchParams) { this.searchParams = searchParams; }
        
        public PaginationInfo getPagination() { return pagination; }
        public void setPagination(PaginationInfo pagination) { this.pagination = pagination; }
    }
    
    // Inner class for pagination metadata
    public static class PaginationInfo {
        private int offset;
        private int limit;
        private int total;
        private boolean hasNext;
        private boolean hasPrevious;
        
        // Default constructor
        public PaginationInfo() {}
        
        // Constructor
        public PaginationInfo(int offset, int limit, int total, boolean hasNext, boolean hasPrevious) {
            this.offset = offset;
            this.limit = limit;
            this.total = total;
            this.hasNext = hasNext;
            this.hasPrevious = hasPrevious;
        }
        
        // Getters and setters
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        
        public boolean isHasNext() { return hasNext; }
        public void setHasNext(boolean hasNext) { this.hasNext = hasNext; }
        
        public boolean isHasPrevious() { return hasPrevious; }
        public void setHasPrevious(boolean hasPrevious) { this.hasPrevious = hasPrevious; }
    }
}