package com.seatmap.auth.service;

import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import com.seatmap.common.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private SessionRepository sessionRepository;
    
    @Mock
    private PasswordService passwordService;
    
    @Mock
    private JwtService jwtService;
    
    private AuthService authService;
    
    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, sessionRepository, passwordService, jwtService);
    }
    
    @Test
    @DisplayName("Should register user successfully")
    void shouldRegisterUserSuccessfully() throws SeatmapException {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "StrongPass123!", "John", "Doe");
        String hashedPassword = "hashed_password";
        String jwtToken = "jwt_token";
        int expirationSeconds = 86400;
        
        when(passwordService.getPasswordValidationError(anyString())).thenReturn(null);
        when(userRepository.emailExists(anyString())).thenReturn(false);
        when(passwordService.hashPassword(anyString())).thenReturn(hashedPassword);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.register(request);
        
        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(User.AuthProvider.EMAIL, response.getAuthProvider());
        assertEquals(expirationSeconds, response.getExpiresIn());
        
        verify(userRepository).saveUser(any(User.class));
        verify(sessionRepository).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid password during registration")
    void shouldThrowExceptionForInvalidPasswordDuringRegistration() throws SeatmapException {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "weak", "John", "Doe");
        
        when(passwordService.getPasswordValidationError(anyString())).thenReturn("Password is too weak");
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.register(request);
        });
        
        assertEquals("Password is too weak", exception.getMessage());
        assertEquals(400, exception.getHttpStatus());
        
        verify(userRepository, never()).saveUser(any(User.class));
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for duplicate email during registration")
    void shouldThrowExceptionForDuplicateEmailDuringRegistration() throws SeatmapException {
        // Given
        RegisterRequest request = new RegisterRequest("test@example.com", "StrongPass123!", "John", "Doe");
        
        when(passwordService.getPasswordValidationError(anyString())).thenReturn(null);
        when(userRepository.emailExists(anyString())).thenReturn(true);
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.register(request);
        });
        
        assertEquals("Email address is already registered", exception.getMessage());
        assertEquals(409, exception.getHttpStatus());
        
        verify(userRepository, never()).saveUser(any(User.class));
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUserSuccessfully() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "StrongPass123!");
        User user = createTestUser();
        String jwtToken = "jwt_token";
        int expirationSeconds = 86400;
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwtToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.login(request);
        
        // Then
        assertNotNull(response);
        assertEquals(jwtToken, response.getToken());
        assertEquals(user.getEmail(), response.getEmail());
        assertEquals(user.getFirstName(), response.getFirstName());
        assertEquals(user.getLastName(), response.getLastName());
        assertEquals(expirationSeconds, response.getExpiresIn());
        
        verify(sessionRepository).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for non-existent user during login")
    void shouldThrowExceptionForNonExistentUserDuringLogin() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("nonexistent@example.com", "StrongPass123!");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.login(request);
        });
        
        assertEquals("Invalid email or password", exception.getMessage());
        assertEquals(401, exception.getHttpStatus());
        
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for invalid password during login")
    void shouldThrowExceptionForInvalidPasswordDuringLogin() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "WrongPass123!");
        User user = createTestUser();
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword(anyString(), anyString())).thenReturn(false);
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.login(request);
        });
        
        assertEquals("Invalid email or password", exception.getMessage());
        assertEquals(401, exception.getHttpStatus());
        
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should throw exception for suspended user during login")
    void shouldThrowExceptionForSuspendedUserDuringLogin() throws SeatmapException {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "StrongPass123!");
        User user = createTestUser();
        user.setStatus(User.UserStatus.SUSPENDED);
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordService.verifyPassword(anyString(), anyString())).thenReturn(true);
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.login(request);
        });
        
        assertEquals("Account is suspended", exception.getMessage());
        assertEquals(403, exception.getHttpStatus());
        
        verify(sessionRepository, never()).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should create guest session successfully")
    void shouldCreateGuestSessionSuccessfully() throws SeatmapException {
        // Given
        String guestToken = "guest_jwt_token";
        int expirationSeconds = 86400;
        
        when(jwtService.generateGuestToken(anyString(), eq(0))).thenReturn(guestToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.createGuestSession();
        
        // Then
        assertNotNull(response);
        assertEquals(guestToken, response.getToken());
        assertNotNull(response.getUserId());
        assertTrue(response.getUserId().startsWith("guest_"));
        assertEquals(expirationSeconds, response.getExpiresIn());
        assertNotNull(response.getGuestLimits());
        assertEquals(0, response.getGuestLimits().getFlightsViewed());
        assertEquals(2, response.getGuestLimits().getMaxFlights());
        assertEquals("Guest session created. You can view up to 2 seat maps.", response.getMessage());
        
        verify(sessionRepository).saveSession(any(Session.class));
    }
    
    @Test
    @DisplayName("Should validate user token successfully")
    void shouldValidateUserTokenSuccessfully() throws SeatmapException {
        // Given
        String token = "valid_jwt_token";
        String userId = "user_123";
        User user = createTestUser();
        
        when(jwtService.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtService.isGuestToken(token)).thenReturn(false);
        when(userRepository.findByKey(userId)).thenReturn(Optional.of(user));
        
        // When
        User result = authService.validateToken(token);
        
        // Then
        assertNotNull(result);
        assertEquals(user.getUserId(), result.getUserId());
        assertEquals(user.getEmail(), result.getEmail());
    }
    
    @Test
    @DisplayName("Should validate guest token successfully")
    void shouldValidateGuestTokenSuccessfully() throws SeatmapException {
        // Given
        String token = "valid_guest_jwt_token";
        String guestId = "guest_123";
        
        when(jwtService.getUserIdFromToken(token)).thenReturn(guestId);
        when(jwtService.isGuestToken(token)).thenReturn(true);
        
        // When
        User result = authService.validateToken(token);
        
        // Then
        assertNull(result); // Guest tokens return null to indicate guest user
    }
    
    @Test
    @DisplayName("Should throw exception for token of non-existent user")
    void shouldThrowExceptionForTokenOfNonExistentUser() throws SeatmapException {
        // Given
        String token = "valid_jwt_token";
        String userId = "non_existent_user";
        
        when(jwtService.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtService.isGuestToken(token)).thenReturn(false);
        when(userRepository.findByKey(userId)).thenReturn(Optional.empty());
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.validateToken(token);
        });
        
        assertEquals("User not found", exception.getMessage());
        assertEquals(401, exception.getHttpStatus());
    }
    
    @Test
    @DisplayName("Should throw exception for token of suspended user")
    void shouldThrowExceptionForTokenOfSuspendedUser() throws SeatmapException {
        // Given
        String token = "valid_jwt_token";
        String userId = "user_123";
        User user = createTestUser();
        user.setStatus(User.UserStatus.SUSPENDED);
        
        when(jwtService.getUserIdFromToken(token)).thenReturn(userId);
        when(jwtService.isGuestToken(token)).thenReturn(false);
        when(userRepository.findByKey(userId)).thenReturn(Optional.of(user));
        
        // When & Then
        SeatmapException exception = assertThrows(SeatmapException.class, () -> {
            authService.validateToken(token);
        });
        
        assertEquals("Account is suspended", exception.getMessage());
        assertEquals(403, exception.getHttpStatus());
    }
    
    @Test
    @DisplayName("Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws SeatmapException {
        // Given
        String oldToken = "old_jwt_token";
        String newToken = "new_jwt_token";
        String userId = "user_123";
        User user = createTestUser();
        int expirationSeconds = 86400;
        
        when(jwtService.getUserIdFromToken(oldToken)).thenReturn(userId);
        when(jwtService.isGuestToken(oldToken)).thenReturn(false);
        when(userRepository.findByKey(userId)).thenReturn(Optional.of(user));
        when(jwtService.refreshToken(oldToken)).thenReturn(newToken);
        when(jwtService.getTokenExpirationSeconds()).thenReturn(expirationSeconds);
        
        // When
        AuthResponse response = authService.refreshToken(oldToken);
        
        // Then
        assertNotNull(response);
        assertEquals(newToken, response.getToken());
        assertEquals(expirationSeconds, response.getExpiresIn());
    }
    
    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws SeatmapException {
        // Given
        String token = "valid_jwt_token";
        String userId = "user_123";
        
        when(jwtService.getUserIdFromToken(token)).thenReturn(userId);
        
        // When & Then
        assertDoesNotThrow(() -> {
            authService.logout(token);
        });
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUserId("test-user-id");
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed_password");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAuthProvider(User.AuthProvider.EMAIL);
        user.setStatus(User.UserStatus.ACTIVE);
        return user;
    }
}