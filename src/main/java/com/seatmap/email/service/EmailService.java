package com.seatmap.email.service;

import com.seatmap.common.exception.SeatmapException;
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
        
        String verificationUrl = BASE_URL + "/auth/verify?token=" + verificationToken;
        
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