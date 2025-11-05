package com.seatmap.auth.service;

import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.auth.repository.UserUsageRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IpBasedGuestLimitingTest {
    
    @Mock
    private UserRepository mockUserRepository;
    
    @Mock
    private SessionRepository mockSessionRepository;
    
    @Mock
    private PasswordService mockPasswordService;
    
    @Mock
    private JwtService mockJwtService;
    
    @Mock
    private GuestAccessRepository mockGuestAccessRepository;
    
    @Mock
    private UserUsageRepository mockUserUsageRepository;
    
    @Mock
    private EmailService mockEmailService;
    
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        authService = new AuthService(
            mockUserRepository,
            mockSessionRepository, 
            mockPasswordService,
            mockJwtService,
            mockGuestAccessRepository,
            mockUserUsageRepository,
            mockEmailService
        );
        
        // Default mock setup
        lenient().when(mockJwtService.generateGuestToken(anyString(), anyInt())).thenReturn("guest-token");
        lenient().when(mockJwtService.getTokenExpirationSeconds()).thenReturn(86400);
    }
    
    @Test
    void createGuestSession_FirstTimeUser_Success() throws Exception {
        String clientIp = "192.168.1.100";
        
        // Mock: IP has remaining seatmap requests
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(2);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        assertNotNull(response);
        assertEquals("guest-token", response.getToken());
        assertNotNull(response.getUserId());
        assertTrue(response.getUserId().startsWith("guest_"));
        assertTrue(response.getMessage().contains("You have 2 seat map view"));
        assertTrue(response.getMessage().contains("remaining"));
        
        // Verify session was saved with IP
        verify(mockSessionRepository).saveSession(argThat(session -> 
            session.getIpAddress().equals(clientIp) &&
            session.getUserType() == com.seatmap.common.model.Session.UserType.GUEST
        ));
        
        // Should NOT record a seatmap request during token creation
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    void createGuestSession_OneRequestRemaining_Success() throws Exception {
        String clientIp = "192.168.1.101";
        
        // Mock: IP has 1 remaining seatmap request
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(1);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        assertNotNull(response);
        assertEquals("guest-token", response.getToken());
        assertTrue(response.getMessage().contains("You have 1 seat map view remaining"));
        
        verify(mockSessionRepository).saveSession(any());
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    void createGuestSession_ZeroRequestsRemaining_Success() throws Exception {
        String clientIp = "192.168.1.102";
        
        // Mock: IP has 0 remaining seatmap requests (but can still create token)
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(0);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        assertNotNull(response);
        assertEquals("guest-token", response.getToken());
        assertTrue(response.getMessage().contains("You have 0 seat map views remaining"));
        
        // Token creation should still work, limits are enforced at seatmap request level
        verify(mockSessionRepository).saveSession(any());
        verify(mockGuestAccessRepository, never()).recordSeatmapRequest(anyString());
    }
    
    @Test
    void createGuestSession_BackwardCompatibility_WorksWithoutIp() throws Exception {
        // Test backward compatibility method
        when(mockGuestAccessRepository.getRemainingSeatmapRequests("unknown")).thenReturn(2);
        
        @SuppressWarnings("deprecation")
        AuthResponse response = authService.createGuestSession();
        
        assertNotNull(response);
        assertEquals("guest-token", response.getToken());
        assertTrue(response.getMessage().contains("You have 2 seat map view"));
        
        verify(mockGuestAccessRepository).getRemainingSeatmapRequests("unknown");
        verify(mockSessionRepository).saveSession(any());
    }
    
    @Test
    void createGuestSession_SessionStorageIncludesIp() throws Exception {
        String clientIp = "10.0.0.1";
        
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(2);
        
        authService.createGuestSession(clientIp);
        
        // Verify session is saved with IP address
        verify(mockSessionRepository).saveSession(argThat(session -> 
            session.getIpAddress().equals(clientIp) &&
            session.getUserType() == com.seatmap.common.model.Session.UserType.GUEST
        ));
    }
    
    @Test
    void createGuestSession_MultipleIpsIndependent() throws Exception {
        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";
        
        // Both IPs have independent limits
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(ip1)).thenReturn(2);
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(ip2)).thenReturn(1);
        
        // Create session for IP1
        AuthResponse response1 = authService.createGuestSession(ip1);
        assertNotNull(response1);
        assertTrue(response1.getMessage().contains("You have 2 seat map view"));
        
        // Create session for IP2  
        AuthResponse response2 = authService.createGuestSession(ip2);
        assertNotNull(response2);
        assertTrue(response2.getMessage().contains("You have 1 seat map view"));
        
        // Verify both IPs were tracked separately
        verify(mockGuestAccessRepository).getRemainingSeatmapRequests(ip1);
        verify(mockGuestAccessRepository).getRemainingSeatmapRequests(ip2);
    }
    
    @Test
    void createGuestSession_UnknownIpHandling() throws Exception {
        String unknownIp = "unknown";
        
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(unknownIp)).thenReturn(2);
        
        AuthResponse response = authService.createGuestSession(unknownIp);
        
        assertNotNull(response);
        verify(mockGuestAccessRepository).getRemainingSeatmapRequests(unknownIp);
    }
    
    @Test
    void createGuestSession_RepositoryException_PropagatesError() throws Exception {
        String clientIp = "192.168.1.200";
        
        // Mock repository to throw exception
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.createGuestSession(clientIp);
        });
        
        assertEquals("Database connection failed", exception.getMessage());
        
        verify(mockGuestAccessRepository).getRemainingSeatmapRequests(clientIp);
        // Since the exception occurs before session creation, session should NOT be saved
        verify(mockSessionRepository, never()).saveSession(any());
    }
    
    @Test
    void createGuestSession_MessageFormatting_SingleView() throws Exception {
        String clientIp = "192.168.1.103";
        
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(1);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        // Should use singular form for 1 view
        assertTrue(response.getMessage().contains("You have 1 seat map view remaining"));
        assertFalse(response.getMessage().contains("views remaining"));
    }
    
    @Test
    void createGuestSession_MessageFormatting_MultipleViews() throws Exception {
        String clientIp = "192.168.1.104";
        
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(2);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        // Should use plural form for 2 views
        assertTrue(response.getMessage().contains("You have 2 seat map views remaining"));
        assertFalse(response.getMessage().contains("view remaining"));
    }
    
    @Test
    void createGuestSession_NoLimitEnforcementDuringTokenCreation() throws Exception {
        String clientIp = "192.168.1.105";
        
        // Even if IP has 0 remaining requests, token creation should still work
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(0);
        
        AuthResponse response = authService.createGuestSession(clientIp);
        
        // Should succeed - limits are enforced at seatmap request level, not token creation
        assertNotNull(response);
        assertEquals("guest-token", response.getToken());
        
        verify(mockSessionRepository).saveSession(any());
    }
    
    @Test
    void createGuestSession_JwtServiceParameters() throws Exception {
        String clientIp = "192.168.1.106";
        String expectedGuestId = "guest_";
        
        when(mockGuestAccessRepository.getRemainingSeatmapRequests(clientIp)).thenReturn(2);
        
        authService.createGuestSession(clientIp);
        
        // Verify JWT service called with guest token parameters
        verify(mockJwtService).generateGuestToken(argThat(guestId -> 
            guestId.startsWith(expectedGuestId)), eq(0));
        verify(mockJwtService).getTokenExpirationSeconds();
    }
}