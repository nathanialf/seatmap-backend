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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;
    private final GuestAccessRepository guestAccessRepository;
    private final EmailService emailService;
    
    public AuthService(UserRepository userRepository, 
                      SessionRepository sessionRepository,
                      PasswordService passwordService,
                      JwtService jwtService,
                      GuestAccessRepository guestAccessRepository,
                      EmailService emailService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
        this.guestAccessRepository = guestAccessRepository;
        this.emailService = emailService;
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
        
        // Create new user (unverified)
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordService.hashPassword(request.getPassword()));
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setAuthProvider(User.AuthProvider.EMAIL);
        
        // Set email verification fields
        user.setEmailVerified(false);
        user.setVerificationToken(generateVerificationToken());
        user.setVerificationExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour
        
        // Save unverified user
        userRepository.saveUser(user);
        
        // Send verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        
        logger.info("User registered (unverified): {}", user.getEmail());
        
        // Return response without JWT token (user must verify email first)
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("Registration successful. Please check your email to verify your account.");
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setNewUser(true);
        response.setPending(true); // Indicates email verification required
        
        return response;
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
        
        // Check if email is verified
        if (!user.getEmailVerified()) {
            throw SeatmapException.forbidden("Please verify your email address before logging in");
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
     * Create guest session (no IP limiting - limits applied at seatmap request level)
     */
    public AuthResponse createGuestSession(String clientIp) throws SeatmapException {
        logger.info("Creating guest session for IP: {}", clientIp);
        
        String guestId = "guest_" + UUID.randomUUID().toString();
        
        // Create guest session (no rate limiting at token creation)
        Session session = new Session(UUID.randomUUID().toString(), guestId, Session.UserType.GUEST);
        session.setIpAddress(clientIp);
        
        // Get actual seatmap usage for this IP to include in JWT claims
        int remainingRequests = guestAccessRepository.getRemainingSeatmapRequests(clientIp);
        int usedRequests = 2 - remainingRequests; // Calculate actual usage
        
        // Generate JWT token for guest with accurate usage data
        String token = jwtService.generateGuestToken(guestId, usedRequests);
        session.setJwtToken(token);
        sessionRepository.saveSession(session);
        
        logger.info("Guest session created: {} for IP: {} (used: {}/2)", guestId, clientIp, usedRequests);
        
        AuthResponse response = AuthResponse.forGuest(token, guestId, jwtService.getTokenExpirationSeconds());
        response.setMessage(String.format("Guest session created. You have %d seat map view%s remaining.", 
            remainingRequests, remainingRequests == 1 ? "" : "s"));
        
        return response;
    }
    
    /**
     * Create guest session (backward compatibility - gets IP from context)
     */
    @Deprecated
    public AuthResponse createGuestSession() throws SeatmapException {
        // For backward compatibility, create session without IP tracking
        logger.warn("Creating guest session without IP tracking - this should be updated");
        return createGuestSession("unknown");
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
     * Resend email verification token
     */
    public AuthResponse resendVerificationEmail(String email) throws SeatmapException {
        logger.info("Processing resend verification for email: {}", email);
        
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            throw SeatmapException.notFound("User not found");
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (user.getEmailVerified()) {
            throw SeatmapException.badRequest("Email is already verified");
        }
        
        // Generate new verification token
        user.setVerificationToken(generateVerificationToken());
        user.setVerificationExpiresAt(Instant.now().plusSeconds(3600)); // 1 hour
        user.updateTimestamp();
        
        // Save updated user
        userRepository.saveUser(user);
        
        // Send new verification email
        emailService.sendVerificationEmail(user.getEmail(), user.getVerificationToken());
        
        logger.info("Verification email resent for user: {}", user.getEmail());
        
        AuthResponse response = new AuthResponse();
        response.setSuccess(true);
        response.setMessage("Verification email has been resent. Please check your email.");
        response.setEmail(user.getEmail());
        response.setPending(true);
        
        return response;
    }
    
    /**
     * Verify email address with verification token
     */
    public AuthResponse verifyEmail(String verificationToken) throws SeatmapException {
        logger.info("Processing email verification for token: {}", verificationToken.substring(0, 8) + "...");
        
        // Find user by verification token
        Optional<User> userOpt = userRepository.findByVerificationToken(verificationToken);
        if (userOpt.isEmpty()) {
            throw SeatmapException.badRequest("Invalid or expired verification token");
        }
        
        User user = userOpt.get();
        
        // Check if already verified
        if (user.getEmailVerified()) {
            throw SeatmapException.badRequest("Email is already verified");
        }
        
        // Check if token is expired
        if (user.getVerificationExpiresAt().isBefore(Instant.now())) {
            throw SeatmapException.badRequest("Verification token has expired");
        }
        
        // Verify the user
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationExpiresAt(null);
        user.updateTimestamp();
        
        // Save verified user
        userRepository.saveUser(user);
        
        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        
        // Create session and JWT token for verified user
        Session session = createUserSession(user);
        String token = jwtService.generateToken(user);
        session.setJwtToken(token);
        sessionRepository.saveSession(session);
        
        logger.info("Email verified successfully for user: {}", user.getEmail());
        
        AuthResponse response = new AuthResponse(token, user, jwtService.getTokenExpirationSeconds());
        response.setMessage("Email verified successfully! Welcome to Seatmap.");
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
    
    private String generateVerificationToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        StringBuilder token = new StringBuilder();
        for (byte b : bytes) {
            token.append(String.format("%02x", b));
        }
        return token.toString();
    }
}