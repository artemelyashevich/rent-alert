package com.rentalert.dto.migration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MigrationRunResponse(UUID id, MigrationStatus status, MigrationSettings settings, Instant createdAt,
                                   Instant startedAt, Instant finishedAt, Long requestedBy, List<MigrationStepResponse> steps) {

}
