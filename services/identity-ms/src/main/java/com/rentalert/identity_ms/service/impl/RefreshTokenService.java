package com.rentalert.identity_ms.service.impl;

import com.rentalert.common.exception.AuthException;
import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.util.auth.JwtType;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.auth.RefreshTokenRequest;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.service.LoginService;
import com.rentalert.identity_ms.service.UserProfileService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements LoginService<RefreshTokenRequest> {

    private final JwtService jwtService;
    private final UserProfileService userProfileService;
    private final TokenIssuer tokenIssuer;

    @Override
    public LoginResponse auth(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        Long userId = jwtService.extractUserId(refreshToken, JwtType.REFRESH);
        UserProfile profile = userProfileService.getById(userId);

        // Only the token stored at the last login is accepted, so an older leaked one can't be replayed
        String storedToken = userProfileService.getDecryptedRefreshToken(profile);
        if (storedToken == null || !MessageDigest.isEqual(
            storedToken.getBytes(StandardCharsets.UTF_8), refreshToken.getBytes(StandardCharsets.UTF_8))) {
            throw new AuthException("Refresh token has been revoked");
        }

        LoginResponse response = tokenIssuer.issue(tokenIssuer.toSession(profile), refreshToken);
        log.debug("Access token refreshed for user {}", userId);
        return response;
    }
}
