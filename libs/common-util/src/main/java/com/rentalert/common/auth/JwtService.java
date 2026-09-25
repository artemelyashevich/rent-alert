package com.rentalert.common.auth;

import java.util.Date;

public interface JwtService {

    String generateToken(Long userId, JwtType tokenType);

    void validateToken(String token, JwtType tokenType);

    Date extractExpiration(String token);

    Long extractUserId(String token);
}
