package com.rentalert.common.util.auth;

import com.rentalert.common.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;

@UtilityClass
public class HeaderUtil {

    public static final Locale DEFAULT_LOCALE = Locale.of("ru", "RU");

    public static String getToken(HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION))
            .orElseThrow(() -> new AuthException("Absent session token in request header"))
            .replace("Bearer ", "");
    }

    public static String getLanguage(HttpServletRequest httpServletRequest) {
        return Optional.ofNullable(httpServletRequest.getHeader(HttpHeaders.ACCEPT_LANGUAGE))
            .orElseGet(DEFAULT_LOCALE::getLanguage);
    }
}