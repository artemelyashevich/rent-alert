package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.profile.UserRole;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserSettings;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class AdminLoginServiceTests {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final UserProfileService userProfileService = mock(UserProfileService.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final TokenIssuer tokenIssuer = mock(TokenIssuer.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final UserEventPublisher userEventPublisher = mock(UserEventPublisher.class);
    private final AdminLoginService service = new AdminLoginService(
        repository, userProfileService, ENCODER, jwtService, tokenIssuer, transactionTemplate, userEventPublisher);

    @BeforeEach
    void runCallbackInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
            invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
    }

    @Test
    void adminWithValidPasswordGetsTokens() {
        UserProfile profile = profile(UserRole.ADMIN, ENCODER.encode("secret"));
        UserSession session = UserSession.builder().userId(5L).build();
        LoginResponse response = LoginResponse.builder().accessToken("access").refreshToken("refresh").build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(profile));
        when(tokenIssuer.toSession(profile)).thenReturn(session);
        when(jwtService.generateToken(session, JwtType.REFRESH)).thenReturn("refresh");
        when(userProfileService.encryptRefreshToken("refresh")).thenReturn("refresh-enc");
        when(tokenIssuer.issue(session, "refresh")).thenReturn(response);

        assertThat(service.auth(new AdminLoginRequest("admin", "secret"))).isSameAs(response);
        assertThat(profile.getSettings().getRefreshTokenHash()).isEqualTo("refresh-enc");
        assertThat(profile.getLastLogin()).isNotNull();
        verify(userProfileService).evictCachedProfile(5L);
        verify(userEventPublisher).publish(UserEvent.LOGIN_SUCCESS, 5L);
    }

    @Test
    void wrongPasswordIsRejected() {
        when(repository.findByUsername("admin")).thenReturn(Optional.of(profile(UserRole.ADMIN, ENCODER.encode("secret"))));

        assertThatThrownBy(() -> service.auth(new AdminLoginRequest("admin", "wrong"))).isInstanceOf(AuthException.class);
        verify(tokenIssuer, never()).issue(any(), any());
        verify(userEventPublisher, never()).publish(any(), any());
    }

    @Test
    void nonAdminIsRejected() {
        when(repository.findByUsername("user")).thenReturn(Optional.of(profile(UserRole.USER, ENCODER.encode("secret"))));

        assertThatThrownBy(() -> service.auth(new AdminLoginRequest("user", "secret"))).isInstanceOf(AuthException.class);
    }

    @Test
    void adminWithoutPasswordIsRejected() {
        when(repository.findByUsername("admin")).thenReturn(Optional.of(profile(UserRole.ADMIN, null)));

        assertThatThrownBy(() -> service.auth(new AdminLoginRequest("admin", "secret"))).isInstanceOf(AuthException.class);
    }

    @Test
    void unknownUsernameIsRejected() {
        when(repository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.auth(new AdminLoginRequest("ghost", "secret"))).isInstanceOf(AuthException.class);
        verify(jwtService, never()).generateToken(any(UserSession.class), eq(JwtType.REFRESH));
    }

    private UserProfile profile(UserRole role, String passwordHash) {
        UserProfile profile = new UserProfile();
        profile.setId(5L);
        profile.setSettings(UserSettings.builder().roleName(role).passwordHash(passwordHash).build());
        return profile;
    }
}
