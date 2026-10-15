package com.example.serviceApp.Service;

import com.example.serviceApp.Entity.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs
    ) {
        byte[] decodedSecret = Base64.getDecoder().decode(secret);

        if (decodedSecret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        }

        this.secretKey = Keys.hmacShaKeyFor(decodedSecret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("role", userDetails.getRole().name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
