package com.rentalert.notification_ms.domain;

import com.rentalert.dto.notification.NotificationEngine;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEnginesConverterTests {

    private final NotificationEnginesConverter converter = new NotificationEnginesConverter();

    @Test
    void roundTrips() {
        List<NotificationEngine> engines = List.of(NotificationEngine.TELEGRAM_BOT, NotificationEngine.EMAIL);

        assertThat(converter.convertToDatabaseColumn(engines)).isEqualTo("TELEGRAM_BOT,EMAIL");
        assertThat(converter.convertToEntityAttribute("TELEGRAM_BOT,EMAIL")).isEqualTo(engines);
    }

    @Test
    void toleratesSpacesAndEmptyValue() {
        assertThat(converter.convertToEntityAttribute(" TELEGRAM_BOT , EMAIL ,"))
            .containsExactly(NotificationEngine.TELEGRAM_BOT, NotificationEngine.EMAIL);
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }
}
