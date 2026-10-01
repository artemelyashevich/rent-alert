package com.rentalert.identity_ms.service.impl;

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
import com.rentalert.identity_ms.service.LoginService;
import com.rentalert.identity_ms.service.UserProfileService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLoginService implements LoginService<AdminLoginRequest> {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Неверный логин или пароль";

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenIssuer tokenIssuer;
    private final TransactionTemplate transactionTemplate;
    private final UserEventPublisher userEventPublisher;

    @Override
    public LoginResponse auth(AdminLoginRequest request) {
        LoginData loginData = transactionTemplate.execute(status -> updateProfileOnLogin(request));

        LoginResponse response = tokenIssuer.issue(loginData.session(), loginData.refreshToken());
        userProfileService.evictCachedProfile(loginData.session().getUserId());
        log.debug("Admin {} logged in", loginData.session().getUserId());
        return response;
    }

    private LoginData updateProfileOnLogin(AdminLoginRequest request) {
        UserProfile profile = userProfileRepository.findByUsername(request.username())
            .orElseThrow(() -> {
                log.warn("Admin login failed: unknown username");
                return new AuthException(INVALID_CREDENTIALS_MESSAGE);
            });

        UserSettings settings = profile.getSettings();
        if (settings.getRoleName() != UserRole.ADMIN
            || settings.getPasswordHash() == null
            || !passwordEncoder.matches(request.password(), settings.getPasswordHash())) {
            log.warn("Admin login failed: invalid credentials for profile id {}", profile.getId());
            throw new AuthException(INVALID_CREDENTIALS_MESSAGE);
        }

        UserSession session = tokenIssuer.toSession(profile);
        String refreshToken = jwtService.generateToken(session, JwtType.REFRESH);
        settings.setRefreshTokenHash(userProfileService.encryptRefreshToken(refreshToken));
        profile.setLastLogin(Instant.now());
        userEventPublisher.publish(UserEvent.LOGIN_SUCCESS, profile.getId());
        return new LoginData(session, refreshToken);
    }

    private record LoginData(UserSession session, String refreshToken) {

    }
}
