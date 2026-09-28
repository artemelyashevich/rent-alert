package com.rentalert.identity_ms.service.impl;

import java.time.Instant;
import java.util.Objects;

import com.rentalert.common.auth.JwtService;
import com.rentalert.common.auth.JwtType;
import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.auth.TelegramAuthRequest;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.domain.UserRole;
import com.rentalert.identity_ms.dto.profile.TelegramUser;
import com.rentalert.identity_ms.service.LoginService;
import com.rentalert.identity_ms.service.TelegramInitDataParser;
import com.rentalert.identity_ms.service.UserProfileService;
import com.rentalert.redis.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLoginService implements LoginService<TelegramAuthRequest> {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;
    private final TelegramInitDataParser telegramInitDataParser;
    private final AuthenticationService authenticationService;
    private final UserSessionService userSessionService;

    @Override
    @Transactional
    public LoginResponse auth(TelegramAuthRequest request) {
        TelegramUser telegramUser;
        try {
            telegramUser = telegramInitDataParser.verify(request.initData());
        } catch (Exception e) {
            log.warn("Login failed: invalid Telegram init data", e);
            throw e;
        }

        String telegramId = telegramUser.getId().toString();

        UserProfile profile = authenticationService.findOrRegister(telegramUser, telegramId);

        if (profile.getSettings().getRoleName() == UserRole.BLOCKED) {
            throw new UserBlockedException();
        }

        String accessToken = jwtService.generateToken(profile, JwtType.ACCESS);
        String refreshToken = resolveRefreshToken(profile);

        profile.getSettings().setRefreshTokenHash(userProfileService.encryptRefreshToken(refreshToken));
        profile.setLastLogin(Instant.now());

        syncProfileFromTelegram(profile, telegramUser);

        userProfileService.save(profile);

        log.debug("User {} saved, awaiting commit before session creation", profile.getId());

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                userSessionService.createSession(accessToken, profile);
                log.debug("User {} logged in", profile.getId());
            }
        });

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtService.extractExpiration(accessToken).getTime())
                .build();
    }

    private String resolveRefreshToken(UserProfile profile) {
        String existingRefreshToken = userProfileService.getDecryptedRefreshToken(profile);

        if (existingRefreshToken != null && isValid(existingRefreshToken)) {
            log.debug("Reusing existing refresh token for profile id: {}", profile.getId());
            return existingRefreshToken;
        }

        String newRefreshToken = jwtService.generateToken(profile, JwtType.REFRESH);
        log.debug("Issued new refresh token for profile id: {}", profile.getId());
        return newRefreshToken;
    }

    private boolean isValid(String refreshToken) {
        try {
            jwtService.validateToken(refreshToken, JwtType.REFRESH);
            return true;
        } catch (AuthException e) {
            return false;
        }
    }

    private void syncProfileFromTelegram(UserProfile profile, TelegramUser telegramUser) {
        if (!Objects.equals(profile.getUsername(), telegramUser.getUsername())) {
            log.debug("Username changed for profile id: {}", profile.getId());
            profile.setUsername(telegramUser.getUsername());
        }

        if (!Objects.equals(profile.getPhotoUrl(), telegramUser.getPhotoUrl())) {
            log.debug("Photo url changed for profile id: {}", profile.getId());
            profile.setPhotoUrl(telegramUser.getPhotoUrl());
        }
    }
}