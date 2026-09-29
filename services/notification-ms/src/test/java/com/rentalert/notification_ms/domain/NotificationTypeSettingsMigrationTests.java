package com.rentalert.notification_ms.domain;

import com.rentalert.dto.notification.NotificationType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// The service refuses to start without a row per type; this catches a forgotten seed row at build time
class NotificationTypeSettingsMigrationTests {

    @Test
    void seedHasRowForEveryNotificationType() throws IOException {
        String migration;
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V005__notification_type_settings.sql")) {
            assertThat(in).isNotNull();
            migration = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(Arrays.stream(NotificationType.values()).map(Enum::name))
            .allSatisfy(type -> assertThat(migration).contains("('" + type + "',"));
    }
}
