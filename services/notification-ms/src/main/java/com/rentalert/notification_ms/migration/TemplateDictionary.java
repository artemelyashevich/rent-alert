package com.rentalert.notification_ms.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

final class TemplateDictionary {

    private static final String MONOLITH_SQL = "SELECT id, notification_type, engine FROM public.notification_templates";
    private static final String TARGET_SQL = "SELECT id, notification_type, engine FROM notification_templates";

    private final Map<Long, TemplateKey> monolithTemplates;
    private final Map<TemplateKey, Long> targetTemplates;

    TemplateDictionary(Map<Long, TemplateKey> monolithTemplates, Map<TemplateKey, Long> targetTemplates) {
        this.monolithTemplates = monolithTemplates;
        this.targetTemplates = targetTemplates;
    }

    static TemplateDictionary load(JdbcTemplate monolith, JdbcTemplate target) {
        Map<Long, TemplateKey> monolithTemplates = new HashMap<>();
        monolith.query(MONOLITH_SQL, (RowCallbackHandler) rs -> monolithTemplates.put(rs.getLong("id"), key(rs)));
        Map<TemplateKey, Long> targetTemplates = new HashMap<>();
        target.query(TARGET_SQL, (RowCallbackHandler) rs -> targetTemplates.put(key(rs), rs.getLong("id")));
        return new TemplateDictionary(monolithTemplates, targetTemplates);
    }

    Long targetId(Long monolithTemplateId) {
        if (monolithTemplateId == null) {
            return null;
        }
        TemplateKey key = monolithTemplates.get(monolithTemplateId);
        return key == null ? null : targetTemplates.get(key);
    }

    private static TemplateKey key(ResultSet rs) throws SQLException {
        return new TemplateKey(rs.getString("notification_type"), rs.getString("engine"));
    }

    record TemplateKey(String notificationType, String engine) {
    }
}
