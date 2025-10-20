package com.seatmap.auth.service;

import com.seatmap.common.exception.SeatmapException;
import com.seatmap.common.model.Session;
import com.seatmap.common.model.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final int TOKEN_EXPIRATION_SECONDS = 24 * 60 * 60; // 24 hours
    
    private final SecretKey secretKey;

    public JwtService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET environment variable must be set and at least 32 characters");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("role", "user");
        claims.put("provider", user.getAuthProvider().name().toLowerCase());
        
        return createToken(claims, user.getUserId());
    }

    public String generateGuestToken(String sessionId, int flightsViewed) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "guest");
        claims.put("provider", "guest");
        
        Map<String, Object> guestLimits = new HashMap<>();
        guestLimits.put("flightsViewed", flightsViewed);
        guestLimits.put("maxFlights", 2);
        claims.put("guestLimits", guestLimits);
        
        return createToken(claims, sessionId);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(TOKEN_EXPIRATION_SECONDS);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiration))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims validateToken(String token) throws SeatmapException {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token has expired: {}", e.getMessage());
            throw SeatmapException.unauthorized("Token has expired");
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token: {}", e.getMessage());
            throw SeatmapException.unauthorized("Invalid token format");
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token: {}", e.getMessage());
            throw SeatmapException.unauthorized("Invalid token format");
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature: {}", e.getMessage());
            throw SeatmapException.unauthorized("Invalid token signature");
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw SeatmapException.unauthorized("Invalid token");
        }
    }

    public String getUserIdFromToken(String token) throws SeatmapException {
        Claims claims = validateToken(token);
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) throws SeatmapException {
        Claims claims = validateToken(token);
        return claims.get("role", String.class);
    }

    public boolean isGuestToken(String token) throws SeatmapException {
        String role = getRoleFromToken(token);
        return "guest".equals(role);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (SeatmapException e) {
            return true;
        }
    }

    public int getTokenExpirationSeconds() {
        return TOKEN_EXPIRATION_SECONDS;
    }

    public String refreshToken(String oldToken) throws SeatmapException {
        Claims claims = validateToken(oldToken);
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);

        Map<String, Object> newClaims = new HashMap<>();
        newClaims.put("role", role);
        newClaims.put("provider", claims.get("provider"));

        if ("guest".equals(role)) {
            newClaims.put("guestLimits", claims.get("guestLimits"));
        } else {
            newClaims.put("email", claims.get("email"));
        }

        return createToken(newClaims, subject);
    }
}