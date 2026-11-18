package com.rentalert.dto.event.migration;

import java.util.UUID;

public record MigrationProgress(UUID migrationId, int step, String service, String table, long processed, long total) {

}
