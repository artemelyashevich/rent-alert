package com.rentalert.common.util;

import com.rentalert.dto.cleanup.CleanupResult;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class BatchDeleter {

    public static final int BATCH_SIZE = 500;

    public static <T> CleanupResult delete(String section, Supplier<List<T>> keys, ToIntFunction<List<T>> deleteBatch) {
        long startedAt = System.nanoTime();
        List<T> toDelete;
        try {
            toDelete = keys.get();
        } catch (RuntimeException e) {
            log.error("[CLEANUP] {}: failed to select rows to delete", section, e);
            return CleanupResult.sectionFailed(section, elapsed(startedAt), e);
        }
        long deleted = 0;
        long failed = 0;
        for (int from = 0; from < toDelete.size(); from += BATCH_SIZE) {
            List<T> batch = toDelete.subList(from, Math.min(from + BATCH_SIZE, toDelete.size()));
            try {
                deleted += deleteBatch.applyAsInt(batch);
            } catch (RuntimeException e) {
                failed += batch.size();
                log.error("[CLEANUP] {}: failed to delete a batch of {} rows", section, batch.size(), e);
            }
        }
        CleanupResult result = new CleanupResult(deleted, failed, elapsed(startedAt), List.of());
        log.info("[CLEANUP] {}: deleted={} failed={} time={}ms", section, deleted, failed, result.duration().toMillis());
        return result;
    }

    private static Duration elapsed(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
