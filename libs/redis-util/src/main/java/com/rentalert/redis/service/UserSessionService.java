package com.rentalert.redis.service;

import com.rentalert.dto.user.UserSession;

/**
 * Sessions of authenticated users, shared by all services through Redis. A session is keyed by the access token taken from the request's Authorization header.
 */
public interface UserSessionService {

    UserSession getCurrentUser();

    /**
     * Stores {@code session} (userId, username, email, role) under {@code token}, replacing the user's previous session.
     */
    UserSession createSession(UserSession session);

    UserSession createSession(String token, UserSession session);

    /**
     * Re-creates the current request's session with fresh user data.
     */
    void updateSession(UserSession session);

    void invalidateSession(Long userId);
}
