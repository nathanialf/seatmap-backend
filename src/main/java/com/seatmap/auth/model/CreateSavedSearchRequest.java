package com.seatmap.auth.model;

import com.seatmap.api.model.FlightSearchRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateSavedSearchRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    @NotNull(message = "Search request is required")
    @Valid
    private FlightSearchRequest searchRequest;
    
    public CreateSavedSearchRequest() {}
    
    public CreateSavedSearchRequest(String title, FlightSearchRequest searchRequest) {
        this.title = title;
        this.searchRequest = searchRequest;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public FlightSearchRequest getSearchRequest() {
        return searchRequest;
    }
    
    public void setSearchRequest(FlightSearchRequest searchRequest) {
        this.searchRequest = searchRequest;
    }
    
    @Override
    public String toString() {
        return "CreateSavedSearchRequest{" +
                "title='" + title + '\'' +
                ", searchRequest=" + searchRequest +
                '}';
    }
}