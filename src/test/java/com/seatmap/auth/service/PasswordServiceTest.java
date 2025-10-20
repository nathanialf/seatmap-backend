package com.seatmap.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {
    
    private PasswordService passwordService;
    
    @BeforeEach
    void setUp() {
        passwordService = new PasswordService();
    }
    
    @Test
    @DisplayName("Should hash password successfully")
    void shouldHashPassword() {
        String plainPassword = "TestPass123!";
        String hashedPassword = passwordService.hashPassword(plainPassword);
        
        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$12$")); // BCrypt with strength 12
    }
    
    @Test
    @DisplayName("Should throw exception for null password")
    void shouldThrowExceptionForNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.hashPassword(null);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for empty password")
    void shouldThrowExceptionForEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.hashPassword("");
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            passwordService.hashPassword("   ");
        });
    }
    
    @Test
    @DisplayName("Should verify correct password")
    void shouldVerifyCorrectPassword() {
        String plainPassword = "TestPass123!";
        String hashedPassword = passwordService.hashPassword(plainPassword);
        
        assertTrue(passwordService.verifyPassword(plainPassword, hashedPassword));
    }
    
    @Test
    @DisplayName("Should reject incorrect password")
    void shouldRejectIncorrectPassword() {
        String plainPassword = "TestPass123!";
        String wrongPassword = "WrongPass123!";
        String hashedPassword = passwordService.hashPassword(plainPassword);
        
        assertFalse(passwordService.verifyPassword(wrongPassword, hashedPassword));
    }
    
    @Test
    @DisplayName("Should return false for null inputs in verification")
    void shouldReturnFalseForNullInputsInVerification() {
        String hashedPassword = passwordService.hashPassword("TestPass123!");
        
        assertFalse(passwordService.verifyPassword(null, hashedPassword));
        assertFalse(passwordService.verifyPassword("TestPass123!", null));
        assertFalse(passwordService.verifyPassword(null, null));
    }
    
    @Test
    @DisplayName("Should validate strong password")
    void shouldValidateStrongPassword() {
        String validPassword = "StrongPass123!";
        
        assertTrue(passwordService.isValidPassword(validPassword));
        assertNull(passwordService.getPasswordValidationError(validPassword));
    }
    
    @Test
    @DisplayName("Should reject password without uppercase")
    void shouldRejectPasswordWithoutUppercase() {
        String invalidPassword = "weakpass123!";
        
        assertFalse(passwordService.isValidPassword(invalidPassword));
        assertEquals("Password must contain at least one uppercase letter", 
                    passwordService.getPasswordValidationError(invalidPassword));
    }
    
    @Test
    @DisplayName("Should reject password without lowercase")
    void shouldRejectPasswordWithoutLowercase() {
        String invalidPassword = "STRONGPASS123!";
        
        assertFalse(passwordService.isValidPassword(invalidPassword));
        assertEquals("Password must contain at least one lowercase letter", 
                    passwordService.getPasswordValidationError(invalidPassword));
    }
    
    @Test
    @DisplayName("Should reject password without number")
    void shouldRejectPasswordWithoutNumber() {
        String invalidPassword = "StrongPass!";
        
        assertFalse(passwordService.isValidPassword(invalidPassword));
        assertEquals("Password must contain at least one number", 
                    passwordService.getPasswordValidationError(invalidPassword));
    }
    
    @Test
    @DisplayName("Should reject password without special character")
    void shouldRejectPasswordWithoutSpecialCharacter() {
        String invalidPassword = "StrongPass123";
        
        assertFalse(passwordService.isValidPassword(invalidPassword));
        assertEquals("Password must contain at least one special character", 
                    passwordService.getPasswordValidationError(invalidPassword));
    }
    
    @Test
    @DisplayName("Should reject password that is too short")
    void shouldRejectPasswordThatIsTooShort() {
        String invalidPassword = "Test1!";
        
        assertFalse(passwordService.isValidPassword(invalidPassword));
        assertEquals("Password must be at least 8 characters long", 
                    passwordService.getPasswordValidationError(invalidPassword));
    }
    
    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        assertFalse(passwordService.isValidPassword(null));
        assertEquals("Password is required", 
                    passwordService.getPasswordValidationError(null));
    }
    
    @Test
    @DisplayName("Should accept various special characters")
    void shouldAcceptVariousSpecialCharacters() {
        String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "-", "=", "[", "]", "{", "}", "|", ";", ":", ",", ".", "<", ">", "?"};
        
        for (String specialChar : specialChars) {
            String password = "StrongPass123" + specialChar;
            assertTrue(passwordService.isValidPassword(password), 
                      "Password with special character '" + specialChar + "' should be valid");
        }
    }
    
    @Test
    @DisplayName("Should generate different hashes for same password")
    void shouldGenerateDifferentHashesForSamePassword() {
        String plainPassword = "TestPass123!";
        String hash1 = passwordService.hashPassword(plainPassword);
        String hash2 = passwordService.hashPassword(plainPassword);
        
        assertNotEquals(hash1, hash2); // BCrypt uses salt, so hashes should be different
        
        // But both should verify against the original password
        assertTrue(passwordService.verifyPassword(plainPassword, hash1));
        assertTrue(passwordService.verifyPassword(plainPassword, hash2));
    }
}