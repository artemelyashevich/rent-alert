package com.rentalert.dto.migration;

import java.util.List;

public record MigrationStartRequest(List<String> services, MigrationSettings settings) {

}
