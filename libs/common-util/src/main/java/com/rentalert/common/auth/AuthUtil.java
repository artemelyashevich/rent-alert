package com.rentalert.common.auth;

import com.rentalert.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

@UtilityClass
public class AuthUtil {

    public static String getToken(HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION))
                .orElseThrow(() -> new AuthException("Absent session token in request header"))
                .replace("Bearer ", "");
    }
}