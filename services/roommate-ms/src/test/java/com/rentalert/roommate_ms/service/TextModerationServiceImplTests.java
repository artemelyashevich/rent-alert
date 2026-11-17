package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.service.impl.TextModerationServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextModerationServiceImplTests {

    private final TextModerationService service = new TextModerationServiceImpl(List.of("казино", " ставки на спорт ", ""));

    @Test
    void emptyAndOrdinaryTextIsApproved() {
        assertThat(service.check(null).status()).isEqualTo(ModerationStatus.APPROVED_AUTO);
        assertThat(service.check("  ").status()).isEqualTo(ModerationStatus.APPROVED_AUTO);
        assertThat(service.check("Спокойный, работаю из дома, люблю готовить").status()).isEqualTo(ModerationStatus.APPROVED_AUTO);
    }

    @Test
    void bannedWordIsRejectedRegardlessOfCase() {
        assertThat(service.check("Зову в КАЗИНО").status()).isEqualTo(ModerationStatus.REJECTED_AUTO);
        assertThat(service.check("ставки на спорт каждый день").status()).isEqualTo(ModerationStatus.REJECTED_AUTO);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "звоните +375 29 123-45-67",
        "пишите на me@mail.by",
        "см. https://example.com",
        "мой ник @roommate_42",
        "пишите в телеграм"
    })
    void contactDetailsGoToManualModeration(String text) {
        assertThat(service.check(text).status()).isEqualTo(ModerationStatus.PENDING_MANUAL);
    }
}
