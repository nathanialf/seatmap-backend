package com.seatmap.auth.service;

import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.repository.GuestAccessRepository;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import com.seatmap.common.model.User;
import com.seatmap.email.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationTest {
    
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
    private EmailService emailService;
    
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, sessionRepository, passwordService, jwtService, guestAccessRepository, emailService);
    }
    
    @Test
    @DisplayName("Should register user without JWT token and send verification email")
    void shouldRegisterUserWithoutJwtTokenAndSendVerificationEmail() throws SeatmapException {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "StrongPass123!", "John", "Doe");
        String hashedPassword = "hashed_password";
        
        when(passwordService.getPasswordValidationError(anyString())).thenReturn(null);
        when(userRepository.emailExists(anyString())).thenReturn(false);
        when(passwordService.hashPassword(anyString())).thenReturn(hashedPassword);
        
        // When
        AuthResponse response = authService.register(request);
        
        // Then
        assertNotNull(response);
        assertNull(response.getToken()); // No JWT token until email verified
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertTrue(response.isNewUser());
        assertTrue(response.isPending());
        assertTrue(response.getMessage().contains("verify your account"));
        
        // Verify user is saved as unverified
        verify(userRepository).saveUser(argThat(user -> 
            user.getEmail().equals("test@example.com") &&
            !user.getEmailVerified() &&
            user.getVerificationToken() != null &&
            user.getVerificationExpiresAt() != null &&
            user.getVerificationExpiresAt().isAfter(Instant.now())
        ));
        
        // Verify email is sent
        verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString());
        
        // No session created until verification
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should verify email successfully and return JWT token")
    void shouldVerifyEmailSuccessfullyAndReturnJwtToken() throws SeatmapException {
        // Given
        String verificationToken = "valid_verification_token";
        String jwtToken = "jwt_token";
        int expirationSeconds = 86400;
        
        User unverifiedUser = createUnverifiedUser();
        unverifiedUser.setVerificationToken(verificationToken);
        unverifiedUser.setVerificationExpiresAt(Instant.now().plusSeconds(1800)); // 30 minutes from now
        
        when(userRepository.findByVerificationToken(verificationToken)).thenReturn(Optional.of(unverifiedUser));
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.verifyEmail(verificationToken);
        
        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals(unverifiedUser.getEmail(), response.getEmail());
        assertEquals(unverifiedUser.getFirstName(), response.getFirstName());
        assertEquals(unverifiedUser.getLastName(), response.getLastName());
        assertEquals(expirationSeconds, response.getExpiresIn());
        assertTrue(response.getMessage().contains("verified successfully"));
        
        // Verify user is marked as verified
        verify(userRepository).saveUser(argThat(user -> 
            user.getEmailVerified() &&
            user.getVerificationToken() == null &&
            user.getVerificationExpiresAt() == null
        ));
        
        // Verify welcome email is sent
        verify(emailService).sendWelcomeEmail(unverifiedUser.getEmail(), unverifiedUser.getFirstName());
        
        // Verify session is created
        verify(sessionRepository).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid verification token")
    void shouldThrowExceptionForInvalidVerificationToken() throws SeatmapException {
        // Given
        String invalidToken = "invalid_token";
        
        when(userRepository.findByVerificationToken(invalidToken)).thenReturn(Optional.empty());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.verifyEmail(invalidToken);
        });
        
        assertEquals("Invalid or expired verification token", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
        
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for expired verification token")
    void shouldThrowExceptionForExpiredVerificationToken() throws SeatmapException {
        // Given
        String expiredToken = "expired_token";
        User user = createUnverifiedUser();
        user.setVerificationToken(expiredToken);
        user.setVerificationExpiresAt(Instant.now().minusSeconds(1800)); // 30 minutes ago
        
        when(userRepository.findByVerificationToken(expiredToken)).thenReturn(Optional.of(user));
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.verifyEmail(expiredToken);
        });
        
        assertEquals("Verification token has expired", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
        
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for already verified user")
    void shouldThrowExceptionForAlreadyVerifiedUser() throws SeatmapException {
        // Given
        String token = "valid_token";
        User verifiedUser = createVerifiedUser();
        verifiedUser.setVerificationToken(token);
        
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(verifiedUser));
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.verifyEmail(token);
        });
        
        assertEquals("Email is already verified", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
        
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should resend verification email successfully")
    void shouldResendVerificationEmailSuccessfully() throws SeatmapException {
        // Given
        String email = "test@example.com";
        User unverifiedUser = createUnverifiedUser();
        unverifiedUser.setEmail(email);
        
        when(userRepository.findByEmail(email.toLowerCase().trim())).thenReturn(Optional.of(unverifiedUser));
        
        // When
        AuthResponse response = authService.resendVerificationEmail(email);
        
        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(email, response.getEmail());
        assertTrue(response.isPending());
        assertTrue(response.getMessage().contains("Verification email has been resent"));
        
        // Verify new token is generated
        verify(userRepository).saveUser(argThat(user -> 
            user.getVerificationToken() != null &&
            user.getVerificationExpiresAt() != null &&
            user.getVerificationExpiresAt().isAfter(Instant.now())
        ));
        
        // Verify email is sent with new token
        verify(emailService).sendVerificationEmail(eq(email), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when resending to non-existent user")
    void shouldThrowExceptionWhenResendingToNonExistentUser() throws SeatmapException {
        // Given
        String email = "nonexistent@example.com";
        
        when(userRepository.findByEmail(email.toLowerCase().trim())).thenReturn(Optional.empty());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.resendVerificationEmail(email);
        });
        
        assertEquals("User not found", exception.getMessage());
        assertEquals(404, exception.getHttpStatus());
        
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should throw exception when resending to already verified user")
    void shouldThrowExceptionWhenResendingToAlreadyVerifiedUser() throws SeatmapException {
        // Given
        String email = "verified@example.com";
        User verifiedUser = createVerifiedUser();
        verifiedUser.setEmail(email);
        
        when(userRepository.findByEmail(email.toLowerCase().trim())).thenReturn(Optional.of(verifiedUser));
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.resendVerificationEmail(email);
        });
        
        assertEquals("Email is already verified", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
        
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should prevent login for unverified user")
    void shouldPreventLoginForUnverifiedUser() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("unverified@example.com", "StrongPass123!");
        User unverifiedUser = createUnverifiedUser();
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(unverifiedUser));
        when(passwordService.verifyPassword(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.login(request);
        });
        
        assertEquals("Please verify your email address before logging in", exception.getMessage());
        assertEquals(403, exception.getHttpStatus());
        
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should allow login for verified user")
    void shouldAllowLoginForVerifiedUser() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("verified@example.com", "StrongPass123!");
        User verifiedUser = createVerifiedUser();
        String jwtToken = "jwt_token";
        int expirationSeconds = 86400;
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(verifiedUser));
        when(passwordService.verifyPassword(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.login(request);
        
        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals(verifiedUser.getEmail(), response.getEmail());
        assertEquals(expirationSeconds, response.getExpiresIn());
        
        verify(sessionRepository).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should generate secure verification token")
    void shouldGenerateSecureVerificationToken() throws SeatmapException {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "StrongPass123!", "John", "Doe");
        
        when(passwordService.getPasswordValidationError(anyString())).thenReturn(null);
        when(userRepository.emailExists(anyString())).thenReturn(false);
        when(passwordService.hashPassword(anyString())).thenReturn("hashed_password");
        
        // When
        authService.register(request);
        
        // Then
        verify(userRepository).saveUser(argThat(user -> {
            String token = user.getVerificationToken();
            return token != null && 
                   token.length() == 64 && // 64 character hex string
                   token.matches("[a-f0-9]+"); // Only hex characters
        }));
    }
    
    private User createUnverifiedUser() {
        User user = new User();
        user.setUserId("test-user-id");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthProvider(User.AuthProvider.EMAIL);
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(false);
        return user;
    }
    
    private User createVerifiedUser() {
        User user = createUnverifiedUser();
        user.setEmailVerified(true);
        return user;
    }
}