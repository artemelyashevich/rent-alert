package com.rentalert.kafka.migration;

public class MigrationPreflightException extends IllegalStateException {

    public MigrationPreflightException(String message) {
        super(message);
    }
}
