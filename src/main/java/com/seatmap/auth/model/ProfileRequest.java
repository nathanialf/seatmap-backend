package com.seatmap.auth.model;

import jakarta.validation.constraints.Size;

public class ProfileRequest {
    
    @Size(max = 50, message = "First name must be 50 characters or less")
    private String firstName;
    
    @Size(max = 50, message = "Last name must be 50 characters or less")
    private String lastName;

    public ProfileRequest() {}

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

}