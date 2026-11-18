package com.rentalert.dto.migration;

public record TableResult(String table, long total, long migrated, long skipped, long failed) {

}
