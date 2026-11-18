package com.rentalert.kafka.migration;

import java.time.Duration;
import java.util.UUID;

public interface MigrationControl {

    boolean isCancelled(UUID migrationId);

    boolean tryWithLock(String lockName, Duration leaseTime, Runnable action);
}
