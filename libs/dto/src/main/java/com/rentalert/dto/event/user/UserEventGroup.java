package com.rentalert.dto.event.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserEventGroup {

    AUTH("Аутентификация"),
    PROFILE("Профиль"),
    FILTERS("Фильтры"),
    FAVORITES("Избранное"),
    LISTINGS("Объявления"),
    NOTIFICATIONS("Уведомления"),
    FEEDBACK("Отзывы"),
    BOT("Телеграм-бот"),
    ROOMMATE("Соседи");

    private final String name;
}
