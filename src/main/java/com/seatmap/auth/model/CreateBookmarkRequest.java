package com.seatmap.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBookmarkRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData;
    
    @NotBlank(message = "Source is required")
    private String source;

    public CreateBookmarkRequest() {}

    public CreateBookmarkRequest(String title, String flightOfferData, String source) {
        this.title = title;
        this.flightOfferData = flightOfferData;
        this.source = source;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFlightOfferData() {
        return flightOfferData;
    }

    public void setFlightOfferData(String flightOfferData) {
        this.flightOfferData = flightOfferData;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}