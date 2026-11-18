package com.rentalert.dto.migration;

import java.time.Instant;
import java.util.List;

public record MigrationStepResponse(int step, String service, MigrationStepStatus status, List<TableResult> tables,
                                    long processed, long total, String error, Instant startedAt, Instant finishedAt) {

}
