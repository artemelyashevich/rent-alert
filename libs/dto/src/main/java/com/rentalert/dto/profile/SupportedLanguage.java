package com.rentalert.dto.profile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SupportedLanguage {
    RU("Русский"),
    EN("English"),
    BE("Беларуская мова");

    @Getter
    private final String name;
}
