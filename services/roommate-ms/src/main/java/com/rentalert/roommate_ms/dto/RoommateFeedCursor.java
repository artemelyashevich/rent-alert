package com.rentalert.roommate_ms.dto;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpStatus;

public record RoommateFeedCursor(int score, Instant createdAt, long id, int paramsHash) {

    public static RoommateFeedCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length != 5) {
                throw new IllegalArgumentException("expected 5 segments, got " + parts.length);
            }
            Instant createdAt = Instant.ofEpochSecond(Long.parseLong(parts[1]), Long.parseLong(parts[2]));
            return new RoommateFeedCursor(Integer.parseInt(parts[0]), createdAt, Long.parseLong(parts[3]), Integer.parseInt(parts[4]));
        } catch (RuntimeException e) {
            throw new AppException(InternalExceptionCode.EX400_ROOMMATE_FEED_CURSOR_INVALID, HttpStatus.BAD_REQUEST);
        }
    }

    public String encode() {
        String raw = score + ":" + createdAt.getEpochSecond() + ":" + createdAt.getNano() + ":" + id + ":" + paramsHash;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isBefore(int entryScore, Instant entryCreatedAt, long entryId) {
        if (entryScore != score) {
            return entryScore < score;
        }
        int byTime = entryCreatedAt.compareTo(createdAt);
        if (byTime != 0) {
            return byTime < 0;
        }
        return entryId < id;
    }
}
