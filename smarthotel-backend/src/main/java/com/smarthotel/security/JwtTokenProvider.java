package com.smarthotel.security;

import com.smarthotel.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Base64;
import java.security.MessageDigest;
import javax.crypto.Mac;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecret()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(
            Long userId,
            String username,
            String roleCode,
            String passwordHash) {

        Date now = new Date();

        Date expireTime = new Date(
                now.getTime() + jwtProperties.getExpiration()
        );

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", roleCode)
                .claim("credentialVersion", credentialVersion(passwordHash))
                .setIssuedAt(now)
                .setExpiration(expireTime)
                .signWith(
                        getSigningKey(),
                        SignatureAlgorithm.HS256
                )
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean matchesCredentialVersion(Claims claims, String passwordHash) {
        String presented = claims.get("credentialVersion", String.class);
        return presented != null && MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.US_ASCII),
                credentialVersion(passwordHash).getBytes(StandardCharsets.US_ASCII));
    }

    private String credentialVersion(String passwordHash) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(getSigningKey());
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(passwordHash.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("无法生成凭据版本", e);
        }
    }
}
