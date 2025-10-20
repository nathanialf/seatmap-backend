package com.seatmap.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);
    private static final int BCRYPT_STRENGTH = 12; // Cost factor as specified in HLD
    
    private final BCryptPasswordEncoder encoder;
    
    public PasswordService() {
        this.encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
    
    /**
     * Hash a plain text password using bcrypt with cost factor 12
     */
    public String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        
        logger.debug("Hashing password with bcrypt strength {}", BCRYPT_STRENGTH);
        return encoder.encode(plainPassword);
    }
    
    /**
     * Verify a plain text password against a bcrypt hash
     */
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        
        logger.debug("Verifying password against bcrypt hash");
        return encoder.matches(plainPassword, hashedPassword);
    }
    
    /**
     * Validate password meets security requirements
     * Must be at least 8 characters with uppercase, lowercase, number, and special character
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);
        
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
    
    /**
     * Get password validation error message
     */
    public String getPasswordValidationError(String password) {
        if (password == null) {
            return "Password is required";
        }
        
        if (password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        
        if (!password.chars().anyMatch(Character::isUpperCase)) {
            return "Password must contain at least one uppercase letter";
        }
        
        if (!password.chars().anyMatch(Character::isLowerCase)) {
            return "Password must contain at least one lowercase letter";
        }
        
        if (!password.chars().anyMatch(Character::isDigit)) {
            return "Password must contain at least one number";
        }
        
        if (!password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0)) {
            return "Password must contain at least one special character";
        }
        
        return null; // Password is valid
    }
}