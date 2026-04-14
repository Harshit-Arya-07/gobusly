package com.busbooking.security;

import jakarta.annotation.PostConstruct;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Slf4j
public class JwtService {

    private static final String DEFAULT_BASE64_SECRET = "YXBwLWRldi1qd3Qtc2VjcmV0LWNoYW5nZS1pbi1wcm9kdWN0aW9u";
    private static final long DEFAULT_EXPIRATION_MS = 86_400_000L;

    @Value("${app.jwt.secret:${APP_JWT_SECRET:" + DEFAULT_BASE64_SECRET + "}}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:${APP_JWT_EXPIRATION_MS:" + DEFAULT_EXPIRATION_MS + "}}")
    private long jwtExpirationMs;

    @PostConstruct
    void initializeJwtConfig() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = DEFAULT_BASE64_SECRET;
            log.warn("JWT secret was missing; using development fallback. Set APP_JWT_SECRET in production.");
        }

        if (jwtExpirationMs <= 0) {
            jwtExpirationMs = DEFAULT_EXPIRATION_MS;
            log.warn("JWT expiration was invalid; using default {} ms", DEFAULT_EXPIRATION_MS);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
