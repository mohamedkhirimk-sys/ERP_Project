package com.erp.identity.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(
            "my-test-secret-key-that-is-at-least-32-characters-long-for-hs384",
            3600000
        );
    }

    @Test
    void generateToken_shouldReturnValidJwt() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void validateToken_shouldReturnCorrectClaims() {
        String token = jwtUtil.generateToken("testuser", "ADMIN");
        Claims claims = jwtUtil.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals("ADMIN", claims.get("role"));
    }

    @Test
    void validateToken_shouldThrowOnInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.validateToken("invalid.token.here"));
    }
}
