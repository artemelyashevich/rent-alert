package com.rentalert.common.util.auth;

import com.rentalert.dto.user.UserSession;
import java.util.Date;

public interface JwtService {

    String generateToken(UserSession userProfile, JwtType tokenType);

    void validateToken(String token, JwtType tokenType);

    Date extractExpiration(String token);

    Long extractUserId(String token);

    Long extractUserId(String token, JwtType tokenType);
}
