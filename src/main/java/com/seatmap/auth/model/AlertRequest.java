package com.seatmap.auth.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AlertRequest {
    
    @NotNull(message = "Alert threshold is required")
    @Positive(message = "Alert threshold must be positive")
    private Double alertThreshold;

    public AlertRequest() {}

    public AlertRequest(Double alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    public Double getAlertThreshold() {
        return alertThreshold;
    }

    public void setAlertThreshold(Double alertThreshold) {
        this.alertThreshold = alertThreshold;
    }

    @Override
    public String toString() {
        return "AlertRequest{" +
                "alertThreshold=" + alertThreshold +
                '}';
    }
}