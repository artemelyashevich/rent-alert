package com.rentalert.common.util;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class MaskUtil {

    private static final String MASK = "**********";

    private static final Pattern URL_PATTERN = Pattern.compile(
        "(?i)\\b((?:https?|ftp)://[^\\s]+|www\\.[^\\s]+)"
    );

    public static String maskLinks(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        return URL_PATTERN.matcher(input).replaceAll(MASK);
    }
}
