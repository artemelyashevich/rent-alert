package com.rentalert.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TextUtils {

    public static String cleanText(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String cleaned = input.replaceAll("[^a-zA-Zа-яА-Я0-9\\s]", "");
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    public static String deleteLastDots(String text) {
        return text.trim().replaceAll("\\.*$", "");
    }
}