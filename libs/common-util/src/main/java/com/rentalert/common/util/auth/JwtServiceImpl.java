package com.rentalert.common.util.auth;

import com.rentalert.common.config.AuthProperties;
import com.rentalert.common.exception.AuthException;
import com.rentalert.dto.user.UserSession;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtServiceImpl implements JwtService {

    private final AuthProperties authProperties;
    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;

    private static final long MINIMUM_TOKEN_LIFETIME_SECONDS = 5 * 60;

    public JwtServiceImpl(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.accessSecretKey = toKey("app.auth.access-secret", authProperties.accessSecret());
        this.refreshSecretKey = toKey("app.auth.refresh-secret", authProperties.refreshSecret());
        log.info("JWT Service initialized with secret keys");
    }

    // HS256 needs at least 256 bits: fail at startup instead of on the first request
    private static SecretKey toKey(String property, String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(property + " must be set and at least 32 bytes long");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateToken(UserSession userProfile, JwtType tokenType) {
        long lifetime = getLifetime(tokenType);
        if (lifetime < MINIMUM_TOKEN_LIFETIME_SECONDS * 1000) {
            log.warn("Token lifetime {} is less than minimum {}ms",
                lifetime, MINIMUM_TOKEN_LIFETIME_SECONDS * 1000);
        }
        SecretKey secretKey = getKey(tokenType);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + lifetime);
        log.debug("Generating {} token for user {}, lifetime: {}s", tokenType, userProfile.getUsername(), lifetime / 1000);
        return Jwts.builder()
            .setSubject(userProfile.getUserId().toString())
            .addClaims(Map.of("role", userProfile.getRole()))
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    @Override
    public void validateToken(String token, JwtType tokenType) {
        getClaims(token, tokenType);
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, JwtType.ACCESS, Claims::getExpiration);
    }

    @Override
    public Long extractUserId(String token) {
        return extractUserId(token, JwtType.ACCESS);
    }

    @Override
    public Long extractUserId(String token, JwtType tokenType) {
        Claims claims = getClaims(token, tokenType);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthException("Token does not contain user ID");
        }
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new AuthException("Invalid user ID format in token: " + subject);
        }
    }

    private <T> T extractClaim(String token, JwtType tokenType, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaims(token, tokenType);
        T result = claimsResolver.apply(claims);
        if (result == null) {
            throw new AuthException("Claim not found in token");
        }
        return result;
    }

    private Claims getClaims(String token, JwtType tokenType) {
        if (token == null || token.isBlank()) {
            throw new AuthException("Token is empty");
        }
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getKey(tokenType))
                .build()
                .parseClaimsJws(token)
                .getBody();
            validateClaimsContent(claims);
            return claims;
        } catch (ExpiredJwtException e) {
            log.warn("Token has expired: {}", e.getMessage());
            throw new AuthException("Token expired");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT: {}", e.getMessage());
            throw new AuthException("Unsupported token format");
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT: {}", e.getMessage());
            throw new AuthException("Malformed token");
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            throw new AuthException("Invalid token signature");
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw new AuthException("Invalid token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid JWT argument: {}", e.getMessage());
            throw new AuthException("Invalid token format");
        } catch (Exception e) {
            log.error("Unexpected error during JWT parsing", e);
            throw new AuthException("Token validation error");
        }
    }

    private void validateClaimsContent(Claims claims) {
        Date expiration = claims.getExpiration();
        if (expiration != null && new Date().after(expiration)) {
            throw new AuthException("Token has expired");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new AuthException("Token does not contain user ID");
        }
    }

    private SecretKey getKey(JwtType type) {
        return JwtType.ACCESS.equals(type) ? accessSecretKey : refreshSecretKey;
    }

    private long getLifetime(JwtType type) {
        long seconds = JwtType.ACCESS.equals(type)
            ? authProperties.expirationSeconds()
            : authProperties.expirationSeconds() * 24;
        if (seconds <= 0) {
            log.warn("Invalid expiration seconds configuration: {}", seconds);
            seconds = JwtType.ACCESS.equals(type) ? 3600 : 3600 * 24;
        }
        return seconds * 1000;
    }
}