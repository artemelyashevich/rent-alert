package com.rentalert.notification_ms.domain;

import com.rentalert.dto.notification.NotificationEngine;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores channels as "TELEGRAM_BOT,EMAIL": readable and editable by hand in the table.
 */
@Converter
public class NotificationEnginesConverter implements AttributeConverter<List<NotificationEngine>, String> {

    @Override
    public String convertToDatabaseColumn(List<NotificationEngine> engines) {
        return engines == null ? "" : engines.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    @Override
    public List<NotificationEngine> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(name -> !name.isEmpty())
            .map(NotificationEngine::valueOf)
            .toList();
    }
}
