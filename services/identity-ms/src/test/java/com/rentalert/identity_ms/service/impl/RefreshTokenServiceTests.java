package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.config.AuthProperties;
import com.rentalert.common.exception.AuthException;
import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtServiceImpl;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.auth.RefreshTokenRequest;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshTokenServiceTests {

    private final JwtService jwt = new JwtServiceImpl(new AuthProperties(
        "test-access-secret-that-is-at-least-32-bytes", "test-refresh-secret-that-is-at-least-32-bytes", 3600));
    private final UserProfileService profiles = mock(UserProfileService.class);
    private final UserSessionService sessions = mock(UserSessionService.class);
    private final RefreshTokenService service = new RefreshTokenService(jwt, profiles, new TokenIssuer(jwt, sessions));

    private final UserProfile profile = new UserProfile();
    private String refreshToken;

    @BeforeEach
    void setUp() {
        profile.setId(5L);
        profile.setUsername("ann");
        refreshToken = jwt.generateToken(UserSession.builder().userId(5L).role("USER").build(), JwtType.REFRESH);
        when(profiles.getById(5L)).thenReturn(profile);
        when(profiles.getDecryptedRefreshToken(profile)).thenReturn(refreshToken);
        when(sessions.createSession(any())).thenAnswer(inv -> {
            UserSession session = inv.getArgument(0);
            session.setToken(jwt.generateToken(session, JwtType.ACCESS));
            return session;
        });
    }

    @Test
    void issuesAccessTokenForStoredRefreshToken() {
        LoginResponse response = service.auth(new RefreshTokenRequest(refreshToken));

        assertThat(jwt.extractUserId(response.accessToken())).isEqualTo(5L);
        assertThat(response.refreshToken()).isEqualTo(refreshToken);
        assertThat(response.expiresIn()).isBetween(3590L, 3600L);
    }

    @Test
    void rejectsRefreshTokenReplacedByNewerLogin() {
        when(profiles.getDecryptedRefreshToken(profile)).thenReturn("another-token");

        assertThatThrownBy(() -> service.auth(new RefreshTokenRequest(refreshToken))).isInstanceOf(AuthException.class);
        verify(sessions, never()).createSession(any());
    }

    @Test
    void rejectsAccessTokenPassedAsRefreshToken() {
        String accessToken = jwt.generateToken(UserSession.builder().userId(5L).role("USER").build(), JwtType.ACCESS);

        assertThatThrownBy(() -> service.auth(new RefreshTokenRequest(accessToken))).isInstanceOf(AuthException.class);
    }

    @Test
    void rejectsBlockedUser() {
        profile.getSettings().setRoleName(UserRole.BLOCKED);

        assertThatThrownBy(() -> service.auth(new RefreshTokenRequest(refreshToken))).isInstanceOf(UserBlockedException.class);
        verify(sessions, never()).createSession(any());
    }
}
