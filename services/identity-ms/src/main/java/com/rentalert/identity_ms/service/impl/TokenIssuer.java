package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.redis.service.UserSessionService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Issues an access token for a user: shared by login and refresh.
 */
@Component
@RequiredArgsConstructor
public class TokenIssuer {

    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    public UserSession toSession(UserProfile profile) {
        UserSettings settings = profile.getSettings();
        if (settings.getRoleName() == UserRole.BLOCKED) {
            throw new UserBlockedException();
        }
        return UserSession.builder()
            .userId(profile.getId())
            .username(profile.getUsername())
            .email(settings.getEmail())
            .role(settings.getRoleName().toString())
            .build();
    }

    /**
     * Stores the session in Redis, so call it only after the profile changes are committed.
     */
    public LoginResponse issue(UserSession session, String refreshToken) {
        String accessToken = userSessionService.createSession(session).getToken();
        Instant expiresAt = jwtService.extractExpiration(accessToken).toInstant();
        return LoginResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(Duration.between(Instant.now(), expiresAt).toSeconds())
            .build();
    }
}
