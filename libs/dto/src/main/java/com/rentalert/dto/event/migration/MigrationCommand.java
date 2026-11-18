package com.rentalert.dto.event.migration;

import com.rentalert.dto.migration.MigrationSettings;
import java.util.UUID;

public record MigrationCommand(UUID migrationId, int step, String service, MigrationSettings settings) {

}
