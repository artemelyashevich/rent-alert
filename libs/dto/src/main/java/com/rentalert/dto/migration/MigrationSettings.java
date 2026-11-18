package com.rentalert.dto.migration;

import java.time.Instant;
import java.util.List;

public record MigrationSettings(int batchSize, Long limitPerTable, Instant since, boolean dryRun, List<String> tables) {

    public static final int DEFAULT_BATCH_SIZE = 1000;
    public static final int MAX_BATCH_SIZE = 50_000;
}
