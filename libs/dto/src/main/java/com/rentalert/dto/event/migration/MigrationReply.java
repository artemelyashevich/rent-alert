package com.rentalert.dto.event.migration;

import com.rentalert.dto.migration.TableResult;
import java.util.List;
import java.util.UUID;

public record MigrationReply(UUID migrationId, int step, String service, boolean success, List<TableResult> tables, String error) {

}
