package com.rentalert.dto.event.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminEventGroup {
    JOB("Действия"),
    NOTIFICATION("Рассылка уведомлений"),
    SCHEDULER("Настройки планировщика"),
    STATISTICS("Статистика"),
    USER("Действия с пользователем"),
    CITY("Справочник городов"),
    ROOMMATE("Модерация «Соседей»");

    private final String name;
}
