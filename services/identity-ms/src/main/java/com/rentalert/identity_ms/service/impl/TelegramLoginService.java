package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.dto.auth.TelegramAuthRequest;
import com.rentalert.dto.profile.TelegramUser;
import com.rentalert.dto.user.UserSession;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.messaging.UserEventPublisher;
import com.rentalert.identity_ms.service.LoginService;
import com.rentalert.identity_ms.service.TelegramInitDataParser;
import com.rentalert.identity_ms.service.UserProfileService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramLoginService implements LoginService<TelegramAuthRequest> {

    private final UserProfileService userProfileService;
    private final JwtService jwtService;
    private final TelegramInitDataParser telegramInitDataParser;
    private final TokenIssuer tokenIssuer;
    private final TransactionTemplate transactionTemplate;
    private final UserEventPublisher userEventPublisher;

    @Override
    public LoginResponse auth(TelegramAuthRequest request) {
        TelegramUser telegramUser = telegramInitDataParser.verify(request.initData());

        LoginData loginData = transactionTemplate.execute(status -> updateProfileOnLogin(telegramUser));

        LoginResponse response = tokenIssuer.issue(loginData.session(), loginData.refreshToken());
        userProfileService.evictCachedProfile(loginData.session().getUserId());
        log.debug("User {} logged in", loginData.session().getUserId());
        return response;
    }

    private LoginData updateProfileOnLogin(TelegramUser telegramUser) {
        UserProfile profile = userProfileService.findOrRegister(telegramUser, telegramUser.getId().toString());

        profile.setUsername(telegramUser.getUsername());
        profile.setPhotoUrl(telegramUser.getPhotoUrl());
        profile.setLastLogin(Instant.now());
        userEventPublisher.publish(UserEvent.LOGIN_SUCCESS, profile.getId());

        UserSession session = tokenIssuer.toSession(profile);
        return new LoginData(session, resolveRefreshToken(profile, session));
    }

    private String resolveRefreshToken(UserProfile profile, UserSession session) {
        String existingRefreshToken = userProfileService.getDecryptedRefreshToken(profile);
        if (existingRefreshToken != null && isValid(existingRefreshToken)) {
            log.debug("Reusing existing refresh token for profile id: {}", profile.getId());
            return existingRefreshToken;
        }

        String newRefreshToken = jwtService.generateToken(session, JwtType.REFRESH);
        profile.getSettings().setRefreshTokenHash(userProfileService.encryptRefreshToken(newRefreshToken));
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

    private record LoginData(UserSession session, String refreshToken) {

    }
}
