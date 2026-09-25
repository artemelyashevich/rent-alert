package com.rentalert.identity_ms.domain;

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
