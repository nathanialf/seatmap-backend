package com.seatmap.auth.service;

import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.repository.UserUsageRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.GuestAccessHistory;
import com.seatmap.common.model.User;
import com.seatmap.common.model.UserUsageHistory;
import com.seatmap.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Guest Usage Transfer Tests")
class GuestUsageTransferTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SessionRepository sessionRepository;
    
    @Mock
    private PasswordService passwordService;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private GuestAccessRepository guestAccessRepository;
    
    @Mock
    private UserUsageRepository userUsageRepository;
    
    @Mock
    private EmailService emailService;
    
    private AuthService authService;
    
    @BeforeEach
    void setUp() throws SeatmapException {
        authService = new AuthService(
            userRepository,
            sessionRepository,
            passwordService,
            jwtService,
            guestAccessRepository,
            userUsageRepository,
            emailService
        );
        
        // Default mock setup
        lenient().when(passwordService.getPasswordValidationError(anyString())).thenReturn(null);
        lenient().when(passwordService.hashPassword(anyString())).thenReturn("hashed-password");
        lenient().when(userRepository.emailExists(anyString())).thenReturn(false);
        lenient().doNothing().when(userRepository).saveUser(any(User.class));
        lenient().doNothing().when(emailService).sendVerificationEmail(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should transfer guest usage when valid guest record exists")
    void shouldTransferGuestUsageWhenValidGuestRecordExists() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.100";
        int guestUsedRequests = 2;
        
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory guestHistory = new GuestAccessHistory(clientIp);
        guestHistory.setSeatmapRequestsUsed(guestUsedRequests);
        guestHistory.setExpiresAt(Instant.now().plusSeconds(3600)); // Not expired
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(guestHistory));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        assertEquals("newuser@example.com", response.getEmail());
        
        // Verify guest access was looked up
        verify(guestAccessRepository).findByIpAddress(clientIp);
        
        // Verify user usage record was created with transferred usage
        ArgumentCaptor<UserUsageHistory> usageCaptor = ArgumentCaptor.forClass(UserUsageHistory.class);
        verify(userUsageRepository).save(usageCaptor.capture());
        
        UserUsageHistory savedUsage = usageCaptor.getValue();
        assertEquals(guestUsedRequests, savedUsage.getSeatmapRequestsUsed());
        assertNotNull(savedUsage.getUserId());
        assertEquals(UserUsageHistory.getCurrentMonthYear(), savedUsage.getMonthYear());
    }
    
    @Test
    @DisplayName("Should not transfer usage when no guest record exists")
    void shouldNotTransferUsageWhenNoGuestRecordExists() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.101";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.empty());
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify guest access was looked up
        verify(guestAccessRepository).findByIpAddress(clientIp);
        
        // Verify no user usage record was created (since no guest usage to transfer)
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should not transfer usage when guest record is expired")
    void shouldNotTransferUsageWhenGuestRecordExpired() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.102";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory expiredGuestHistory = new GuestAccessHistory(clientIp);
        expiredGuestHistory.setSeatmapRequestsUsed(1);
        expiredGuestHistory.setExpiresAt(Instant.now().minusSeconds(3600)); // Expired
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(expiredGuestHistory));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify guest access was looked up
        verify(guestAccessRepository).findByIpAddress(clientIp);
        
        // Verify no user usage record was created (expired guest record)
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should not transfer usage when guest has zero seatmap requests")
    void shouldNotTransferUsageWhenGuestHasZeroRequests() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.103";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory guestHistory = new GuestAccessHistory(clientIp);
        guestHistory.setSeatmapRequestsUsed(0); // No usage
        guestHistory.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(guestHistory));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify guest access was looked up
        verify(guestAccessRepository).findByIpAddress(clientIp);
        
        // Verify no user usage record was created (zero usage to transfer)
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should handle null IP gracefully without lookup")
    void shouldHandleNullIpGracefully() throws SeatmapException {
        // Given
        String clientIp = null;
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify no guest access lookup was attempted
        verify(guestAccessRepository, never()).findByIpAddress(any());
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should handle empty IP gracefully without lookup")
    void shouldHandleEmptyIpGracefully() throws SeatmapException {
        // Given
        String clientIp = "";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify no guest access lookup was attempted
        verify(guestAccessRepository, never()).findByIpAddress(any());
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should handle unknown IP gracefully without lookup")
    void shouldHandleUnknownIpGracefully() throws SeatmapException {
        // Given
        String clientIp = "unknown";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify no guest access lookup was attempted
        verify(guestAccessRepository, never()).findByIpAddress(any());
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should continue registration even if guest transfer fails")
    void shouldContinueRegistrationEvenIfGuestTransferFails() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.104";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory guestHistory = new GuestAccessHistory(clientIp);
        guestHistory.setSeatmapRequestsUsed(1);
        guestHistory.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(guestHistory));
        doThrow(new RuntimeException("DynamoDB error")).when(userUsageRepository).save(any(UserUsageHistory.class));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess()); // Registration should still succeed
        assertEquals("newuser@example.com", response.getEmail());
        
        // Verify user was still saved and email was sent
        verify(userRepository).saveUser(any(User.class));
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should transfer maximum guest usage correctly")
    void shouldTransferMaximumGuestUsageCorrectly() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.105";
        int maxGuestUsage = 2; // Maximum allowed for guests
        
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory guestHistory = new GuestAccessHistory(clientIp);
        guestHistory.setSeatmapRequestsUsed(maxGuestUsage);
        guestHistory.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(guestHistory));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify user usage record was created with maximum guest usage
        ArgumentCaptor<UserUsageHistory> usageCaptor = ArgumentCaptor.forClass(UserUsageHistory.class);
        verify(userUsageRepository).save(usageCaptor.capture());
        
        UserUsageHistory savedUsage = usageCaptor.getValue();
        assertEquals(maxGuestUsage, savedUsage.getSeatmapRequestsUsed());
    }
    
    @Test
    @DisplayName("Should handle guest record with null usage gracefully")
    void shouldHandleGuestRecordWithNullUsageGracefully() throws SeatmapException {
        // Given
        String clientIp = "192.168.1.106";
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        GuestAccessHistory guestHistory = new GuestAccessHistory(clientIp);
        guestHistory.setSeatmapRequestsUsed(null); // Null usage
        guestHistory.setExpiresAt(Instant.now().plusSeconds(3600));
        
        when(guestAccessRepository.findByIpAddress(clientIp)).thenReturn(Optional.of(guestHistory));
        
        // When
        AuthResponse response = authService.register(registerRequest, clientIp);
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify no user usage record was created (null treated as zero)
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    @Test
    @DisplayName("Should use backward compatibility register method without IP")
    void shouldUseBackwardCompatibilityRegisterMethod() throws SeatmapException {
        // Given
        RegisterRequest registerRequest = createValidRegisterRequest();
        
        // When
        AuthResponse response = authService.register(registerRequest); // Without IP parameter
        
        // Then
        assertTrue(response.isSuccess());
        
        // Verify no guest access lookup was attempted (backward compatibility)
        verify(guestAccessRepository, never()).findByIpAddress(any());
        verify(userUsageRepository, never()).save(any(UserUsageHistory.class));
    }
    
    private RegisterRequest createValidRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("ValidPass123!");
        request.setFirstName("John");
        request.setLastName("Doe");
        return request;
    }
}