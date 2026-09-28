package com.rentalert.redis.service.impl;

import com.rentalert.common.util.auth.JwtService;
import com.rentalert.common.exception.AuthException;
import com.rentalert.common.exception.UserBlockedException;
import com.rentalert.common.security.AuthenticatedUser;
import com.rentalert.common.security.AuthenticationService;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.service.UserSessionService;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

@RequiredArgsConstructor
public class SessionAuthenticationService implements AuthenticationService {

    public static final String BLOCKED_ROLE = "BLOCKED";

    private final JwtService jwtService;
    private final UserSessionService userSessionService;

    @Override
    public UserDetails findUserByToken(String token) {
        Long userId = jwtService.extractUserId(token);
        UserSession session = userSessionService.getCurrentUser();
        if (!Objects.equals(userId, session.getUserId())) {
            throw new AuthException("Session does not belong to the token owner");
        }
        if (BLOCKED_ROLE.equals(session.getRole())) {
            throw new UserBlockedException();
        }
        return new AuthenticatedUser(session.getUserId(), session.getUsername(), session.getEmail(), session.getRole());
    }
}
