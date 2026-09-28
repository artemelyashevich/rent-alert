package com.rentalert.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelDto {

    private NotificationEngine engine;

    /** Включён ли канал у пользователя. */
    private boolean enabled;

    /** Является ли канал каналом по умолчанию у пользователя. */
    private boolean isDefault;

    /** Доступен ли канал вообще (глобальный флаг NotificationEngine.isEnabled). */
    private boolean available;
}