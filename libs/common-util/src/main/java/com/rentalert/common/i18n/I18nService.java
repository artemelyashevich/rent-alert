package com.rentalert.common.i18n;

import com.rentalert.common.util.auth.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class I18nService {

    private final MessageSource messageSource;
    private final HttpServletRequest httpServletRequest;

    public String getMessage(String messageKey) {
        String language = HeaderUtil.getLanguage(httpServletRequest);
        return translate(messageKey, "", Locale.of(language));
    }

    public String getErrorMessage(String errorKey) {
        String language = HeaderUtil.getLanguage(httpServletRequest);
        return translate(errorKey, "Internal server error", Locale.of(language));
    }

    private String translate(String code, String defaultMsg, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, defaultMsg, locale);
    }

    private String translateError(String code, String defaultMsg, Locale locale, Object... args) {
        return messageSource.getMessage("ERROR_" + code, args, defaultMsg, locale);
    }
}
