package com.rentalert.dto.event.cleanup;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CleanupCommand(UUID sagaId, int step, String service, Instant referenceTime) {

}
