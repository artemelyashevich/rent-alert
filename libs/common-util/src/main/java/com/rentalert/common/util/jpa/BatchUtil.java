package com.rentalert.common.util.jpa;

import java.util.List;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class BatchUtil {

    private static final int BATCH_SIZE = 250;

    public static <T> void deleteInBatches(List<T> keys, Consumer<List<T>> deleteFn) {
        for (int i = 0; i < keys.size(); i += BATCH_SIZE) {
            List<T> batch = keys.subList(i, Math.min(i + BATCH_SIZE, keys.size()));
            deleteFn.accept(batch);
        }
    }

    public static <T> void deleteInBatchesByJob(List<T> keys, Consumer<List<T>> deleteFn, String jobName) {
        for (int i = 0; i < keys.size(); i += BATCH_SIZE) {
            List<T> batch = keys.subList(i, Math.min(i + BATCH_SIZE, keys.size()));
            deleteFn.accept(batch);
            log.debug("[{}] Processed batch [{}/{}]", jobName, Math.min(i + BATCH_SIZE, keys.size()), keys.size());
        }
    }
}
