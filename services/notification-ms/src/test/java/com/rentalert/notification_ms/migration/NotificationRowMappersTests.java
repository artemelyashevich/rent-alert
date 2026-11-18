package com.rentalert.notification_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NotificationRowMappersTests {

    private static final List<String> REMINDER_LABELS = List.of("id", "notification_template_id", "status");

    @Test
    void remindersReplacesMonolithTemplateIdWithTargetId() throws SQLException {
        TemplateDictionary templates = new TemplateDictionary(
            Map.of(5L, new TemplateDictionary.TemplateKey("FAVORITE_REMINDER", "TELEGRAM_BOT")),
            Map.of(new TemplateDictionary.TemplateKey("FAVORITE_REMINDER", "TELEGRAM_BOT"), 9L));
        Map<String, Object> values = new HashMap<>();
        values.put("id", 1L);
        values.put("notification_template_id", 5L);
        values.put("status", "NEW");

        Object[] row = NotificationRowMappers.reminders(REMINDER_LABELS, templates).mapRow(resultSet(values), 1);

        assertThat(row).containsExactly(1L, 9L, "NEW");
    }

    @Test
    void remindersLeavesTemplateNullWhenTargetHasNoMatch() throws SQLException {
        TemplateDictionary templates = new TemplateDictionary(
            Map.of(5L, new TemplateDictionary.TemplateKey("FAVORITE_REMINDER", "TELEGRAM_BOT")),
            Map.of());
        Map<String, Object> values = new HashMap<>();
        values.put("id", 1L);
        values.put("notification_template_id", 5L);
        values.put("status", "NEW");

        Object[] row = NotificationRowMappers.reminders(REMINDER_LABELS, templates).mapRow(resultSet(values), 1);

        assertThat(row).containsExactly(1L, null, "NEW");
    }

    @Test
    void remindersWithoutTemplateStayNull() throws SQLException {
        TemplateDictionary templates = new TemplateDictionary(Map.of(), Map.of());
        Map<String, Object> values = new HashMap<>();
        values.put("id", 1L);
        values.put("notification_template_id", null);
        values.put("status", "NEW");

        Object[] row = NotificationRowMappers.reminders(REMINDER_LABELS, templates).mapRow(resultSet(values), 1);

        assertThat(row).containsExactly(1L, null, "NEW");
    }

    private static ResultSet resultSet(Map<String, Object> values) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getObject(anyString())).thenAnswer(invocation -> {
            String label = invocation.getArgument(0);
            return values.get(label);
        });
        return rs;
    }
}
