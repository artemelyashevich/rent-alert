package com.rentalert.kafka.migration;

import com.rentalert.dto.event.migration.MigrationProgress;
import com.rentalert.dto.migration.MigrationSettings;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrationContext {

    private static final long PROGRESS_INTERVAL_NANOS = Duration.ofSeconds(2).toNanos();

    private final UUID migrationId;
    private final int step;
    private final String service;
    private final MigrationSettings settings;
    private final MigrationControl control;
    private final Consumer<MigrationProgress> progressSink;
    private final Map<String, Long> lastProgressNanos = new HashMap<>();

    MigrationContext(UUID migrationId, int step, String service, MigrationSettings settings,
        MigrationControl control, Consumer<MigrationProgress> progressSink) {
        this.migrationId = migrationId;
        this.step = step;
        this.service = service;
        this.settings = settings;
        this.control = control;
        this.progressSink = progressSink;
    }

    public UUID migrationId() {
        return migrationId;
    }

    public int step() {
        return step;
    }

    public MigrationSettings settings() {
        return settings;
    }

    public boolean cancelled() {
        try {
            return control.isCancelled(migrationId);
        } catch (RuntimeException e) {
            log.warn("[MIGRATION] {}: cancellation flag is unavailable, continuing: {}", migrationId, e.getMessage());
            return false;
        }
    }

    public void progress(String table, long processed, long total) {
        long now = System.nanoTime();
        Long last = lastProgressNanos.get(table);
        if (last == null || now - last >= PROGRESS_INTERVAL_NANOS) {
            send(table, processed, total, now);
        }
    }

    public void finishTable(String table, long processed, long total) {
        send(table, processed, total, System.nanoTime());
    }

    private void send(String table, long processed, long total, long now) {
        lastProgressNanos.put(table, now);
        progressSink.accept(new MigrationProgress(migrationId, step, service, table, processed, total));
    }
}
