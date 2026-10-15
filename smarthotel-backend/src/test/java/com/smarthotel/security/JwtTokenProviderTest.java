package com.smarthotel.security;

import com.smarthotel.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    @Test
    void passwordChangeInvalidatesPreviousToken() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-at-least-thirty-two-bytes-long");
        properties.setExpiration(86_400_000L);
        JwtTokenProvider provider = new JwtTokenProvider(properties);

        String token = provider.generateToken(3L, "customer01", "CUSTOMER", "old-bcrypt-hash");
        Claims claims = provider.parseToken(token);

        assertTrue(provider.matchesCredentialVersion(claims, "old-bcrypt-hash"));
        assertFalse(provider.matchesCredentialVersion(claims, "new-bcrypt-hash"));
    }
}
