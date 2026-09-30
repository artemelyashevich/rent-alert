package com.rentalert.notification_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.dto.notification.NotificationEngine;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.notification_ms.domain.NotificationTypeSetting;
import com.rentalert.notification_ms.repository.NotificationTypeSettingRepository;
import com.rentalert.notification_ms.service.NotificationTypeSettings;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationTypeSettingsServiceImplTests {

    private final NotificationTypeSettingRepository repository = mock(NotificationTypeSettingRepository.class);
    private final NotificationTypeSettingsServiceImpl service = new NotificationTypeSettingsServiceImpl(repository);

    private static List<NotificationTypeSetting> allTypes(boolean enabled) {
        return Arrays.stream(NotificationType.values()).map(type -> row(type, enabled)).toList();
    }

    private static NotificationTypeSetting row(NotificationType type, boolean enabled) {
        NotificationTypeSetting row = new NotificationTypeSetting();
        row.setNotificationType(type);
        row.setCategory("CATEGORY");
        row.setEngines(List.of(NotificationEngine.TELEGRAM_BOT));
        row.setEnabled(enabled);
        return row;
    }

    @Test
    void servesSettingsOfEveryTypeAfterStartup() {
        when(repository.findAll()).thenReturn(allTypes(true));

        service.afterSingletonsInstantiated();

        assertThat(service.get(NotificationType.NEW_LISTING_MATCHED)).isEqualTo(new NotificationTypeSettings(
            NotificationType.NEW_LISTING_MATCHED, "CATEGORY", List.of(NotificationEngine.TELEGRAM_BOT), false, true));
    }

    @Test
    void refusesToStartWhenTypeHasNoSettingsRow() {
        when(repository.findAll()).thenReturn(List.of(row(NotificationType.USER_CREATED, true)));

        assertThatThrownBy(service::afterSingletonsInstantiated)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NEW_LISTING_MATCHED");
    }

    @Test
    void reloadAppliesEditedRows() {
        when(repository.findAll()).thenReturn(allTypes(true));
        service.afterSingletonsInstantiated();

        when(repository.findAll()).thenReturn(allTypes(false));
        service.reload();

        assertThat(service.get(NotificationType.NEW_LISTING_MATCHED).enabled()).isFalse();
    }
}
