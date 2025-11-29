package com.seatmap.email.service;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.alert.service.AlertEvaluationService;
import com.seatmap.api.model.FlightSearchResult;
import com.seatmap.common.model.Bookmark;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private static final String FROM_EMAIL = "myseatmapapp@gmail.com";
    private static final String BASE_URL = System.getenv("BASE_URL");
    
    private final SesClient sesClient;
    
    public EmailService() {
        this.sesClient = SesClient.builder()
            .region(Region.US_WEST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .httpClient(UrlConnectionHttpClient.builder().build())
            .build();
    }
    
    /**
     * Send email verification email to user
     */
    public void sendVerificationEmail(String toEmail, String verificationToken) throws SeatmapException {
        logger.info("Sending verification email to: {}", toEmail);
        
        // Use frontend verification URL instead of API endpoint
        String environment = System.getenv("ENVIRONMENT");
        String frontendUrl = "dev".equals(environment) ? "https://dev.myseatmap.com" : "https://myseatmap.com";
        String verificationUrl = frontendUrl + "/verify-email?token=" + verificationToken;
        
        String subject = "Verify your Seatmap account";
        String htmlBody = buildVerificationEmailHtml(verificationUrl);
        String textBody = buildVerificationEmailText(verificationUrl);
        
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .source(FROM_EMAIL)
                .destination(Destination.builder()
                    .toAddresses(toEmail)
                    .build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).build())
                        .text(Content.builder().data(textBody).build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            logger.info("Verification email sent successfully. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", toEmail, e);
            throw new SeatmapException("EMAIL_SEND_ERROR", "Failed to send verification email", 500, e);
        }
    }
    
    /**
     * Send seat availability alert email
     */
    public void sendSeatAvailabilityAlert(String toEmail, String firstName, Bookmark bookmark, 
                                        AlertEvaluationService.AlertEvaluationResult alertResult) throws SeatmapException {
        logger.info("Sending seat availability alert to: {}", toEmail);
        
        // Extract flight details for email
        FlightDetails flightDetails = extractFlightDetails(bookmark, alertResult);
        
        String subject = buildAlertSubject(bookmark.getTitle(), bookmark.getItemType());
        String htmlBody = buildAlertEmailHtml(firstName, bookmark, alertResult, flightDetails);
        String textBody = buildAlertEmailText(firstName, bookmark, alertResult, flightDetails);
        
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .source(FROM_EMAIL)
                .destination(Destination.builder()
                    .toAddresses(toEmail)
                    .build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).build())
                        .text(Content.builder().data(textBody).build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            logger.info("Alert email sent successfully. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send alert email to: {}", toEmail, e);
            throw new SeatmapException("EMAIL_SEND_ERROR", "Failed to send alert email", 500, e);
        }
    }
    
    /**
     * Send welcome email after successful verification
     */
    public void sendWelcomeEmail(String toEmail, String firstName) throws SeatmapException {
        logger.info("Sending welcome email to: {}", toEmail);
        
        String subject = "Welcome to Seatmap!";
        String htmlBody = buildWelcomeEmailHtml(firstName);
        String textBody = buildWelcomeEmailText(firstName);
        
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                .source(FROM_EMAIL)
                .destination(Destination.builder()
                    .toAddresses(toEmail)
                    .build())
                .message(Message.builder()
                    .subject(Content.builder().data(subject).build())
                    .body(Body.builder()
                        .html(Content.builder().data(htmlBody).build())
                        .text(Content.builder().data(textBody).build())
                        .build())
                    .build())
                .build();
            
            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            logger.info("Welcome email sent successfully. MessageId: {}", response.messageId());
            
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", toEmail, e);
            throw new SeatmapException("EMAIL_SEND_ERROR", "Failed to send welcome email", 500, e);
        }
    }
    
    /**
     * Extract flight details for email templates
     */
    private FlightDetails extractFlightDetails(Bookmark bookmark, AlertEvaluationService.AlertEvaluationResult alertResult) {
        if (bookmark.getItemType() == Bookmark.ItemType.BOOKMARK) {
            // For individual flight bookmarks, extract details from triggering flight if available
            FlightSearchResult flight = alertResult.getTriggeringFlight();
            if (flight != null) {
                return extractFlightDetailsFromResult(flight);
            }
            // For bookmarks, try to extract from flightOfferData
            if (bookmark.getFlightOfferData() != null) {
                return extractFlightDetailsFromOfferData(bookmark.getFlightOfferData());
            }
            // Fallback to empty details
            return new FlightDetails("", "", "", "", "");
        } else {
            // For saved searches, use search criteria
            return new FlightDetails(
                "", // No specific flight number for saved searches
                bookmark.getOrigin() != null ? bookmark.getOrigin() : "",
                bookmark.getDestination() != null ? bookmark.getDestination() : "",
                bookmark.getDepartureDate() != null ? bookmark.getDepartureDate() : "",
                ""
            );
        }
    }
    
    /**
     * Extract flight details from FlightSearchResult
     */
    private FlightDetails extractFlightDetailsFromResult(FlightSearchResult flight) {
        try {
            if (flight.getItineraries() != null && flight.getItineraries().size() > 0) {
                var itinerary = flight.getItineraries().get(0);
                var segments = itinerary.get("segments");
                if (segments != null && segments.size() > 0) {
                    var firstSegment = segments.get(0);
                    var departure = firstSegment.get("departure");
                    var arrival = firstSegment.get("arrival");
                    var operating = firstSegment.get("operating");
                    
                    String carrierCode = operating != null ? operating.get("carrierCode").asText() : 
                                        firstSegment.get("carrierCode").asText();
                    String flightNumber = operating != null ? operating.get("number").asText() : 
                                         firstSegment.get("number").asText();
                    String origin = departure.get("iataCode").asText();
                    String destination = arrival.get("iataCode").asText();
                    String departureDate = departure.get("at").asText().substring(0, 10);
                    
                    return new FlightDetails(
                        carrierCode + flightNumber,
                        origin,
                        destination,
                        departureDate,
                        carrierCode
                    );
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting flight details: {}", e.getMessage());
        }
        return new FlightDetails("", "", "", "", "");
    }
    
    /**
     * Extract flight details from bookmark's flightOfferData
     */
    private FlightDetails extractFlightDetailsFromOfferData(String flightOfferData) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode flightData = mapper.readTree(flightOfferData);
            
            com.fasterxml.jackson.databind.JsonNode itineraries = flightData.get("itineraries");
            if (itineraries != null && itineraries.isArray() && itineraries.size() > 0) {
                com.fasterxml.jackson.databind.JsonNode firstItinerary = itineraries.get(0);
                com.fasterxml.jackson.databind.JsonNode segments = firstItinerary.get("segments");
                if (segments != null && segments.isArray() && segments.size() > 0) {
                    com.fasterxml.jackson.databind.JsonNode firstSegment = segments.get(0);
                    com.fasterxml.jackson.databind.JsonNode departure = firstSegment.get("departure");
                    com.fasterxml.jackson.databind.JsonNode arrival = firstSegment.get("arrival");
                    com.fasterxml.jackson.databind.JsonNode operating = firstSegment.get("operating");
                    
                    String carrierCode = null;
                    if (operating != null && operating.get("carrierCode") != null) {
                        carrierCode = operating.get("carrierCode").asText();
                    } else if (firstSegment.get("carrierCode") != null) {
                        carrierCode = firstSegment.get("carrierCode").asText();
                    }
                    
                    String flightNumber = null;
                    if (operating != null && operating.get("number") != null) {
                        flightNumber = operating.get("number").asText();
                    } else if (firstSegment.get("number") != null) {
                        flightNumber = firstSegment.get("number").asText();
                    }
                    
                    String origin = "";
                    if (departure != null && departure.get("iataCode") != null) {
                        origin = departure.get("iataCode").asText();
                    }
                    
                    String destination = "";
                    if (arrival != null && arrival.get("iataCode") != null) {
                        destination = arrival.get("iataCode").asText();
                    }
                    
                    String departureDate = "";
                    if (departure != null && departure.get("at") != null) {
                        departureDate = departure.get("at").asText().substring(0, 10);
                    }
                    
                    String fullFlightNumber = "";
                    if (carrierCode != null && flightNumber != null) {
                        fullFlightNumber = carrierCode + flightNumber;
                    }
                    
                    return new FlightDetails(
                        fullFlightNumber,
                        origin,
                        destination,
                        departureDate,
                        carrierCode != null ? carrierCode : ""
                    );
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting flight details from offer data: {}", e.getMessage());
        }
        return new FlightDetails("", "", "", "", "");
    }
    
    /**
     * Build alert email subject line using bookmark name
     */
    private String buildAlertSubject(String bookmarkName, Bookmark.ItemType itemType) {
        return bookmarkName;
    }
    
    /**
     * Build alert email HTML body
     */
    private String buildAlertEmailHtml(String firstName, Bookmark bookmark, 
                                     AlertEvaluationService.AlertEvaluationResult alertResult,
                                     FlightDetails flight) {
        String environment = System.getenv("ENVIRONMENT");
        String dashboardUrl = "dev".equals(environment) ? "https://dev.myseatmap.com/dashboard" : "https://myseatmap.com/dashboard";
        
        String thresholdUnit = bookmark.getItemType() == Bookmark.ItemType.BOOKMARK ? " seats" : "%";
        
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Seatmap Alert</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">✈️ Seat Availability Alert</h2>
                    
                    <p>Hello %s,</p>
                    
                    <p>Your seat availability alert has been triggered:</p>
                    
                    <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #2c3e50;">Bookmark Details</h3>
                        <p><strong>Name:</strong> %s</p>
                        %s
                        %s
                        %s
                    </div>
                    
                    <div style="background-color: #e3f2fd; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h4 style="margin-top: 0; color: #1976d2;">Alert Details</h4>
                        <p><strong>Alert:</strong> %s</p>
                        <p><strong>Your Threshold:</strong> %.1f%s</p>
                        <p><strong>Current Status:</strong> %.0f%s</p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                            View Dashboard
                        </a>
                    </div>
                    
                    <p style="color: #7f8c8d; font-size: 14px; margin-top: 30px;">
                        This alert was generated based on your bookmark settings. You can modify or disable alerts at any time.
                    </p>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="color: #95a5a6; font-size: 12px;">
                        © 2025 Seatmap. All rights reserved.<br>
                        Helping airline employees make better standby decisions.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(
                firstName != null ? firstName : "there",
                bookmark.getTitle(),
                flight.flightNumber.isEmpty() ? "" : String.format("<p><strong>Flight:</strong> %s</p>", flight.flightNumber),
                (!flight.origin.isEmpty() && !flight.destination.isEmpty()) ? String.format("<p><strong>Route:</strong> %s → %s</p>", flight.origin, flight.destination) : "",
                flight.departureDate.isEmpty() ? "" : String.format("<p><strong>Date:</strong> %s</p>", flight.departureDate),
                alertResult.getMessage(),
                alertResult.getThreshold(),
                thresholdUnit,
                alertResult.getCurrentValue(),
                bookmark.getItemType() == Bookmark.ItemType.BOOKMARK ? "" : "%",
                dashboardUrl
            );
    }
    
    /**
     * Build alert email text body
     */
    private String buildAlertEmailText(String firstName, Bookmark bookmark, 
                                     AlertEvaluationService.AlertEvaluationResult alertResult,
                                     FlightDetails flight) {
        String environment = System.getenv("ENVIRONMENT");
        String dashboardUrl = "dev".equals(environment) ? "https://dev.myseatmap.com/dashboard" : "https://myseatmap.com/dashboard";
        
        String thresholdUnit = bookmark.getItemType() == Bookmark.ItemType.BOOKMARK ? " seats" : "%";
        
        return """
            SEAT AVAILABILITY ALERT
            
            Hello %s,
            
            Your seat availability alert has been triggered:
            
            BOOKMARK DETAILS
            Name: %s
            %s
            %s
            %s
            
            ALERT DETAILS
            Alert: %s
            Your Threshold: %.1f%s
            Current Status: %.0f%s
            
            ACTIONS
            View Dashboard: %s
            
            This alert was generated based on your bookmark settings. You can modify or disable alerts at any time.
            
            © 2025 Seatmap. All rights reserved.
            Helping airline employees make better standby decisions.
            """.formatted(
                firstName != null ? firstName : "there",
                bookmark.getTitle(),
                flight.flightNumber.isEmpty() ? "" : String.format("Flight: %s", flight.flightNumber),
                (!flight.origin.isEmpty() && !flight.destination.isEmpty()) ? String.format("Route: %s → %s", flight.origin, flight.destination) : "",
                flight.departureDate.isEmpty() ? "" : String.format("Date: %s", flight.departureDate),
                alertResult.getMessage(),
                alertResult.getThreshold(),
                thresholdUnit,
                alertResult.getCurrentValue(),
                bookmark.getItemType() == Bookmark.ItemType.BOOKMARK ? "" : "%",
                dashboardUrl
            );
    }
    
    /**
     * Flight details for email templates
     */
    private static class FlightDetails {
        final String flightNumber;
        final String origin;
        final String destination;
        final String departureDate;
        final String carrierCode;
        
        FlightDetails(String flightNumber, String origin, String destination, String departureDate, String carrierCode) {
            this.flightNumber = flightNumber != null ? flightNumber : "";
            this.origin = origin != null ? origin : "";
            this.destination = destination != null ? destination : "";
            this.departureDate = departureDate != null ? departureDate : "";
            this.carrierCode = carrierCode != null ? carrierCode : "";
        }
    }

    private String buildVerificationEmailHtml(String verificationUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verify your Seatmap account</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Welcome to Seatmap!</h2>
                    
                    <p>Thank you for creating your Seatmap account. To get started, please verify your email address by clicking the button below:</p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #3498db; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>
                    
                    <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
                    <p style="word-break: break-all; background-color: #f8f9fa; padding: 10px; border-radius: 3px;">%s</p>
                    
                    <p style="color: #7f8c8d; font-size: 14px; margin-top: 30px;">
                        This verification link will expire in 1 hour. If you didn't create a Seatmap account, you can safely ignore this email.
                    </p>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="color: #95a5a6; font-size: 12px;">
                        © 2025 Seatmap. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(verificationUrl, verificationUrl);
    }
    
    private String buildVerificationEmailText(String verificationUrl) {
        return """
            Welcome to Seatmap!
            
            Thank you for creating your Seatmap account. To get started, please verify your email address by visiting the following link:
            
            %s
            
            This verification link will expire in 1 hour. If you didn't create a Seatmap account, you can safely ignore this email.
            
            © 2025 Seatmap. All rights reserved.
            """.formatted(verificationUrl);
    }
    
    private String buildWelcomeEmailHtml(String firstName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Welcome to Seatmap!</title>
            </head>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2c3e50;">Welcome to Seatmap, %s!</h2>
                    
                    <p>Your email has been successfully verified and your account is now active!</p>
                    
                    <p>With your Seatmap account, you can:</p>
                    <ul>
                        <li>Search for flights from multiple airlines</li>
                        <li>View detailed seat maps for your flights</li>
                        <li>Save your favorite flights and seat configurations</li>
                        <li>Get priority access to new features</li>
                    </ul>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #27ae60; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; font-weight: bold; display: inline-block;">
                            Start Exploring Flights
                        </a>
                    </div>
                    
                    <p>If you have any questions or need help getting started, feel free to contact our support team.</p>
                    
                    <p>Happy travels!</p>
                    <p>The Seatmap Team</p>
                    
                    <hr style="border: none; border-top: 1px solid #eee; margin: 30px 0;">
                    <p style="color: #95a5a6; font-size: 12px;">
                        © 2025 Seatmap. All rights reserved.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(firstName != null ? firstName : "there", BASE_URL != null ? BASE_URL : "https://seatmap.com");
    }
    
    private String buildWelcomeEmailText(String firstName) {
        return """
            Welcome to Seatmap, %s!
            
            Your email has been successfully verified and your account is now active!
            
            With your Seatmap account, you can:
            • Search for flights from multiple airlines
            • View detailed seat maps for your flights
            • Save your favorite flights and seat configurations
            • Get priority access to new features
            
            Visit %s to start exploring flights.
            
            If you have any questions or need help getting started, feel free to contact our support team.
            
            Happy travels!
            The Seatmap Team
            
            © 2025 Seatmap. All rights reserved.
            """.formatted(firstName != null ? firstName : "there", BASE_URL != null ? BASE_URL : "https://seatmap.com");
    }
}