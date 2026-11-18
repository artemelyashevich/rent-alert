package com.rentalert.notification_ms.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateDictionaryTests {

    private static final TemplateDictionary.TemplateKey FAVORITE =
        new TemplateDictionary.TemplateKey("FAVORITE_REMINDER", "TELEGRAM_BOT");
    private static final TemplateDictionary.TemplateKey EMAIL =
        new TemplateDictionary.TemplateKey("EMAIL_ACTIVATION", "EMAIL");

    @Test
    void mapsMonolithTemplateToTargetTemplateByTypeAndEngine() {
        TemplateDictionary dictionary = new TemplateDictionary(Map.of(5L, FAVORITE), Map.of(FAVORITE, 14L));

        assertThat(dictionary.targetId(5L)).isEqualTo(14L);
    }

    @Test
    void returnsNullWhenTargetHasNoTemplateWithSameTypeAndEngine() {
        TemplateDictionary dictionary = new TemplateDictionary(Map.of(5L, FAVORITE), Map.of(EMAIL, 6L));

        assertThat(dictionary.targetId(5L)).isNull();
    }

    @Test
    void returnsNullForUnknownMonolithTemplate() {
        TemplateDictionary dictionary = new TemplateDictionary(Map.of(5L, FAVORITE), Map.of(FAVORITE, 14L));

        assertThat(dictionary.targetId(99L)).isNull();
    }

    @Test
    void returnsNullForMissingMonolithTemplateId() {
        TemplateDictionary dictionary = new TemplateDictionary(Map.of(5L, FAVORITE), Map.of(FAVORITE, 14L));

        assertThat(dictionary.targetId(null)).isNull();
    }
}
