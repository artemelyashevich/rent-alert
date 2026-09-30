package com.rentalert.dto.event.cleanup;

import com.rentalert.dto.cleanup.CleanupResult;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CleanupReply(UUID sagaId, int step, String service, long deleted, long failed, long durationMs, List<String> errors) {

    public static CleanupReply of(CleanupCommand command, CleanupResult result) {
        return new CleanupReply(command.sagaId(), command.step(), command.service(), result.deleted(), result.failed(), result.duration().toMillis(), result.errors());
    }
}
