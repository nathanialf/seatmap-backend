package com.seatmap.auth.service;

import com.seatmap.auth.model.AuthResponse;
import com.seatmap.auth.model.LoginRequest;
import com.seatmap.auth.model.RegisterRequest;
import com.seatmap.auth.repository.SessionRepository;
import com.seatmap.auth.repository.UserRepository;
import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import com.seatmap.common.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    
    public AuthService(UserRepository userRepository, 
                      SessionRepository sessionRepository,
                      PasswordService passwordService,
                      JwtService jwtService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }
    
    /**
     * Register a new user with email and password
     */
    public AuthResponse register(RegisterRequest request) throws SeatmapException {
        logger.info("Processing registration for email: {}", request.getEmail());
        
        // Validate password
        String passwordError = passwordService.getPasswordValidationError(request.getPassword());
        if (passwordError != null) {
            throw SeatmapException.badRequest(passwordError);
        }
        
        // Check if email already exists
        if (userRepository.emailExists(request.getEmail())) {
            throw SeatmapException.conflict("Email address is already registered");
        }
        
        // Create new user
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordService.hashPassword(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setAuthProvider(User.AuthProvider.EMAIL);
        
        // Save user
        userRepository.saveUser(user);
        
        // Create session
        Session session = createUserSession(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        session.setJwtToken(token);
        sessionRepository.saveSession(session);
        
        logger.info("User registered successfully: {}", user.getUserId());
        
        return new AuthResponse(token, user, jwtService.getTokenExpirationSeconds());
    }
    
    /**
     * Login user with email and password
     */
    public AuthResponse login(LoginRequest request) throws SeatmapException {
        logger.info("Processing login for email: {}", request.getEmail());
        
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail().toLowerCase().trim());
        if (userOpt.isEmpty()) {
            throw SeatmapException.unauthorized("Invalid email or password");
        }
        
        User user = userOpt.get();
        
        // Verify password
        if (!passwordService.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Invalid password attempt for user: {}", user.getUserId());
            throw SeatmapException.unauthorized("Invalid email or password");
        }
        
        // Check if user is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw SeatmapException.forbidden("Account is suspended");
        }
        
        // Create session
        Session session = createUserSession(user);
        
        // Generate JWT token
        String token = jwtService.generateToken(user);
        session.setJwtToken(token);
        sessionRepository.saveSession(session);
        
        logger.info("User logged in successfully: {}", user.getUserId());
        
        return new AuthResponse(token, user, jwtService.getTokenExpirationSeconds());
    }
    
    /**
     * Create guest session
     */
    public AuthResponse createGuestSession() throws SeatmapException {
        logger.info("Creating guest session");
        
        String guestId = "guest_" + UUID.randomUUID().toString();
        
        // Create guest session
        Session session = new Session(UUID.randomUUID().toString(), guestId, Session.UserType.GUEST);
        
        // Generate JWT token for guest
        String token = jwtService.generateGuestToken(guestId, 0);
        session.setJwtToken(token);
        sessionRepository.saveSession(session);
        
        logger.info("Guest session created: {}", guestId);
        
        AuthResponse response = AuthResponse.forGuest(token, guestId, jwtService.getTokenExpirationSeconds());
        response.setMessage("Guest session created. You can view up to 2 seat maps.");
        
        return response;
    }
    
    /**
     * Validate JWT token and return user information
     */
    public User validateToken(String token) throws SeatmapException {
        logger.debug("Validating JWT token");
        
        // Validate JWT token
        String userId = jwtService.getUserIdFromToken(token);
        
        // Check if it's a guest token
        if (jwtService.isGuestToken(token)) {
            // For guest tokens, we don't need to fetch from database
            // The token contains all necessary information
            return null; // Indicates guest user
        }
        
        // Find user by ID
        Optional<User> userOpt = userRepository.findByKey(userId);
        if (userOpt.isEmpty()) {
            throw SeatmapException.unauthorized("User not found");
        }
        
        User user = userOpt.get();
        
        // Check if user is active
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw SeatmapException.forbidden("Account is suspended");
        }
        
        return user;
    }
    
    /**
     * Refresh JWT token
     */
    public AuthResponse refreshToken(String oldToken) throws SeatmapException {
        logger.debug("Refreshing JWT token");
        
        // Validate the old token first
        validateToken(oldToken);
        
        // Generate new token with same claims
        String newToken = jwtService.refreshToken(oldToken);
        
        logger.info("JWT token refreshed successfully");
        
        AuthResponse response = new AuthResponse();
        response.setToken(newToken);
        response.setExpiresIn(jwtService.getTokenExpirationSeconds());
        
        return response;
    }
    
    /**
     * Logout user (invalidate session)
     */
    public void logout(String token) throws SeatmapException {
        logger.debug("Processing logout");
        
        String userId = jwtService.getUserIdFromToken(token);
        
        // For now, we don't maintain a blacklist of tokens
        // JWT tokens will expire naturally after 24 hours
        // In the future, we could maintain a blacklist in Redis/DynamoDB
        
        logger.info("User logged out: {}", userId);
    }
    
    private Session createUserSession(User user) {
        Session session = new Session();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(user.getUserId());
        session.setUserType(Session.UserType.USER);
        return session;
    }
}