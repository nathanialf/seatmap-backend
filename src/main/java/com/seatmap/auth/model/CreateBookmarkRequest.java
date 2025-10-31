package com.seatmap.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateBookmarkRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must be 100 characters or less")
    private String title;
    
    @NotBlank(message = "Flight offer data is required")
    private String flightOfferData;

    public CreateBookmarkRequest() {}

    public CreateBookmarkRequest(String title, String flightOfferData) {
        this.title = title;
        this.flightOfferData = flightOfferData;
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
}