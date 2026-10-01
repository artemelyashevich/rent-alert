package com.rentalert.dto.event.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserEvent {

    LISTING_RECEIVED(UserEventGroup.LISTINGS, "Получено новое объявление"),

    FAVORITE_ADDED(UserEventGroup.FAVORITES, "Добавление объявления id='%s' в избранное"),
    FAVORITE_REMOVED(UserEventGroup.FAVORITES, "Удаление объявления id='%s' из избранного"),

    FILTER_UPDATED(UserEventGroup.FILTERS, "Обновление фильтра"),
    FEEDBACK_CREATED(UserEventGroup.FEEDBACK, "Создание отзыва"),

    LOGIN_SUCCESS(UserEventGroup.AUTH, "Успешный вход в систему"),
    LOGIN_FAILED(UserEventGroup.AUTH, "Неудачная попытка входа"),
    PROFILE_UPDATED(UserEventGroup.PROFILE, "Обновление профиля"),
    USER_DELETED(UserEventGroup.PROFILE, "Удаление профиля администратором"),
    USER_REGISTERED(UserEventGroup.PROFILE, "Регистрация пользователя"),
    FILTER_CREATED(UserEventGroup.FILTERS, "Создание фильтра"),
    FILTER_DELETED(UserEventGroup.FILTERS, "Удаление фильтра"),
    FILTER_ACTIVATED(UserEventGroup.FILTERS, "Активация фильтра"),
    HIDDEN_LISTING(UserEventGroup.LISTINGS, "Скрытие объявления id='%s'"),
    NOTIFICATION_SETTINGS_UPDATED(UserEventGroup.NOTIFICATIONS, "Обновление настроек уведомлений"),
    BOT_COMMAND(UserEventGroup.BOT, "Команда боту: '%s'"),
    TELEGRAM_CALLBACK(UserEventGroup.BOT, "Нажатие кнопки в боте"),
    CALLBACK_FAVORITE(UserEventGroup.BOT, "Добавление в избранное из бота (объявление id='%s')"),
    CALLBACK_UNFAVORITE(UserEventGroup.BOT, "Удаление из избранного из бота (объявление id='%s')"),
    CALLBACK_HIDE(UserEventGroup.BOT, "Скрытие объявления из бота (id='%s')"),
    BOT_BLOCKED(UserEventGroup.BOT, "Пользователь заблокировал бота"),
    BOT_UNBLOCKED(UserEventGroup.BOT, "Пользователь разблокировал бота"),

    ROOMMATE_CONTACT_REVEALED(UserEventGroup.ROOMMATE, "Раскрыт контакт по матчу id='%s' (запросил участник id='%s')");

    private final UserEventGroup group;
    private final String description;
}
