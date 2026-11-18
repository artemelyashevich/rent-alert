package com.rentalert.notification_ms.migration;

import com.rentalert.kafka.migration.ColumnRowMapper;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;

final class NotificationRowMappers {

    private static final String TEMPLATE_COLUMN = "notification_template_id";

    private NotificationRowMappers() {
    }

    static RowMapper<Object[]> reminders(List<String> labels, TemplateDictionary templates) {
        int templateIndex = labels.indexOf(TEMPLATE_COLUMN);
        RowMapper<Object[]> plain = ColumnRowMapper.of(labels, Set.of());
        return (rs, rowNum) -> {
            Object[] row = plain.mapRow(rs, rowNum);
            row[templateIndex] = templates.targetId(toLong(row[templateIndex]));
            return row;
        };
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
