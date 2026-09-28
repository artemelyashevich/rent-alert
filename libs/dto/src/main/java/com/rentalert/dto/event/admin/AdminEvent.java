package com.rentalert.dto.event.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminEvent {
    AUDIT_LOG_CLEAN_UP(AdminEventGroup.STATISTICS, "Очистка журнала аудита"),
    JOB_CLEAN_UP(AdminEventGroup.JOB, "Очистка базы данных"),
    JOB_CACHE_SYNC(AdminEventGroup.JOB, "Синхронизация кэшей"),
    JOB_CLEANUP_CACHE_ALL(AdminEventGroup.JOB, "Очистка кэша"),
    JOB_SYNC_CURRENCY_RATES(AdminEventGroup.JOB, "Синхронизация курсов валют"),
    NOTIFICATION_SEND(AdminEventGroup.NOTIFICATION, "Отправка уведомлений"),
    NOTIFICATION_DELETE(AdminEventGroup.NOTIFICATION, "Удаление по id = '%s'"),
    SCHEDULER_SETTINGS_CREATE(AdminEventGroup.SCHEDULER, "Создание настроек планировщика для провайдера = '%s'"),
    SCHEDULER_SETTINGS_UPDATE(AdminEventGroup.SCHEDULER, "Обновление настроек планировщика для провайдера = '%s'"),
    SCHEDULER_SETTINGS_DELETE(AdminEventGroup.SCHEDULER, "Удаление настроек планировщика для провайдера = '%s'"),
    USER_PROFILES_CHANGE_ROLE(AdminEventGroup.USER, "Изменение роли пользователя. Id пользователя = '%s'. Новая роль = '%s'"),
    USER_PROFILES_DELETE(AdminEventGroup.USER, "Удаление профиля пользователя по id = '%s'"),
    CITY_CREATE(AdminEventGroup.CITY, "Создание города '%s' для провайдера = '%s'"),
    CITY_UPDATE(AdminEventGroup.CITY, "Обновление города по id = '%s'"),
    CITY_TOGGLE_ACTIVE(AdminEventGroup.CITY, "Изменение активности города id = '%s' на '%s'"),
    CITY_DELETE(AdminEventGroup.CITY, "Удаление города по id = '%s'"),
    ROOMMATE_MODERATION_APPROVE(AdminEventGroup.ROOMMATE, "Одобрение задачи модерации id = '%s'"),
    ROOMMATE_MODERATION_REJECT(AdminEventGroup.ROOMMATE, "Отклонение задачи модерации id = '%s'");

    private final AdminEventGroup group;
    private final String description;
}
