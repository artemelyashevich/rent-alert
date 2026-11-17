package com.rentalert.roommate_ms.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoommateFeedCursorTests {

    private final Instant createdAt = Instant.parse("2026-10-01T10:15:30.123456789Z");
    private final RoommateFeedCursor cursor = new RoommateFeedCursor(70, createdAt, 15L, -42);

    @Test
    void encodedCursorDecodesBack() {
        assertThat(RoommateFeedCursor.decode(cursor.encode())).isEqualTo(cursor);
    }

    @Test
    void garbageIsBadRequest() {
        assertThatThrownBy(() -> RoommateFeedCursor.decode("not-a-cursor"))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_FEED_CURSOR_INVALID));
    }

    @Test
    void ordersByScoreThenNewestThenId() {
        assertThat(cursor.isBefore(69, createdAt.plusSeconds(60), 99L)).isTrue();
        assertThat(cursor.isBefore(71, createdAt.minusSeconds(60), 1L)).isFalse();
        assertThat(cursor.isBefore(70, createdAt.minusSeconds(1), 99L)).isTrue();
        assertThat(cursor.isBefore(70, createdAt, 14L)).isTrue();
        assertThat(cursor.isBefore(70, createdAt, 15L)).isFalse();
    }
}
