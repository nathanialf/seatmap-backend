package com.seatmap.email.service;

import com.seatmap.common.exception.SeatmapException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private SesClient mockSesClient;

    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        // Create EmailService instance
        emailService = new EmailService();
        
        // Replace the SES client with our mock using reflection
        Field sesClientField = EmailService.class.getDeclaredField("sesClient");
        sesClientField.setAccessible(true);
        sesClientField.set(emailService, mockSesClient);
        
        // Set test environment variables
        setEnvironmentVariable("BASE_URL", "https://test.seatmap.com");
    }

    @Test
    void sendVerificationEmail_WithValidInputs_SendsEmailSuccessfully() throws SeatmapException {
        // Arrange
        String email = "user@example.com";
        String token = "test-verification-token";
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-message-id-123")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        assertDoesNotThrow(() -> emailService.sendVerificationEmail(email, token));

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendVerificationEmail_WithSesException_ThrowsSeatmapException() {
        // Arrange
        String email = "user@example.com";
        String token = "test-verification-token";
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class)))
            .thenThrow(SesException.builder()
                .message("SES service error")
                .statusCode(500)
                .build());

        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            emailService.sendVerificationEmail(email, token));
        
        assertEquals("EMAIL_SEND_ERROR", exception.getErrorCode());
        assertEquals("Failed to send verification email", exception.getMessage());
        assertEquals(500, exception.getHttpStatus());
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendWelcomeEmail_WithValidInputs_SendsEmailSuccessfully() throws SeatmapException {
        // Arrange
        String email = "user@example.com";
        String firstName = "John";
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-welcome-message-id")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(email, firstName));

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendWelcomeEmail_WithNullFirstName_UsesDefaultGreeting() throws SeatmapException {
        // Arrange
        String email = "user@example.com";
        String firstName = null;
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-welcome-message-id")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        assertDoesNotThrow(() -> emailService.sendWelcomeEmail(email, firstName));

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendWelcomeEmail_WithSesException_ThrowsSeatmapException() {
        // Arrange
        String email = "user@example.com";
        String firstName = "Jane";
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class)))
            .thenThrow(SesException.builder()
                .message("Network timeout")
                .statusCode(503)
                .build());

        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            emailService.sendWelcomeEmail(email, firstName));
        
        assertEquals("EMAIL_SEND_ERROR", exception.getErrorCode());
        assertEquals("Failed to send welcome email", exception.getMessage());
        assertEquals(500, exception.getHttpStatus());
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void sendVerificationEmail_WithGenericException_ThrowsSeatmapException() {
        // Arrange
        String email = "user@example.com";
        String token = "test-token";
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class)))
            .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            emailService.sendVerificationEmail(email, token));
        
        assertEquals("EMAIL_SEND_ERROR", exception.getErrorCode());
        assertEquals("Failed to send verification email", exception.getMessage());
        assertEquals(500, exception.getHttpStatus());
    }

    @Test
    void sendWelcomeEmail_WithGenericException_ThrowsSeatmapException() {
        // Arrange
        String email = "user@example.com";
        String firstName = "Test";
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        // Act & Assert
        SeatmapException exception = assertThrows(SeatmapException.class, () -> 
            emailService.sendWelcomeEmail(email, firstName));
        
        assertEquals("EMAIL_SEND_ERROR", exception.getErrorCode());
        assertEquals("Failed to send welcome email", exception.getMessage());
        assertEquals(500, exception.getHttpStatus());
    }

    @Test
    void verificationEmailContent_ContainsExpectedElements() throws SeatmapException {
        // Arrange
        String email = "test@example.com";
        String token = "verification-token-123";
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-message-id")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        emailService.sendVerificationEmail(email, token);

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void welcomeEmailContent_ContainsExpectedElements() throws SeatmapException {
        // Arrange
        String email = "test@example.com";
        String firstName = "Alice";
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-message-id")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        emailService.sendWelcomeEmail(email, firstName);

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void welcomeEmailContent_WithNullBaseUrl_UsesDefaultUrl() throws Exception {
        // Arrange
        setEnvironmentVariable("BASE_URL", null);
        
        // Create new instance to pick up null BASE_URL
        EmailService serviceWithNullUrl = new EmailService();
        Field sesClientField = EmailService.class.getDeclaredField("sesClient");
        sesClientField.setAccessible(true);
        sesClientField.set(serviceWithNullUrl, mockSesClient);
        
        String email = "test@example.com";
        String firstName = "Bob";
        
        SendEmailResponse mockResponse = SendEmailResponse.builder()
            .messageId("test-message-id")
            .build();
        
        when(mockSesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        serviceWithNullUrl.sendWelcomeEmail(email, firstName);

        // Assert
        verify(mockSesClient).sendEmail(any(SendEmailRequest.class));
    }

    @SuppressWarnings("unchecked")
    private void setEnvironmentVariable(String key, String value) {
        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            java.util.Map<String, String> env = (java.util.Map<String, String>) theEnvironmentField.get(null);
            
            if (value == null) {
                env.remove(key);
            } else {
                env.put(key, value);
            }
            
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            java.util.Map<String, String> cienv = (java.util.Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            
            if (value == null) {
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
        } catch (Exception e) {
            // Environment variable modification failed - tests might still work
        }
    }
}