package com.rentalert.dto.cleanup;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public record CleanupResult(long deleted, long failed, Duration duration, List<String> errors) {

    public CleanupResult {
        errors = List.copyOf(errors);
    }

    public static CleanupResult empty() {
        return new CleanupResult(0, 0, Duration.ZERO, List.of());
    }

    public static CleanupResult sectionFailed(String section, Duration duration, Exception e) {
        return new CleanupResult(0, 0, duration, List.of(section + ": " + e.getMessage()));
    }

    public CleanupResult plus(CleanupResult other) {
        List<String> allErrors = new ArrayList<>(errors);
        allErrors.addAll(other.errors);
        return new CleanupResult(deleted + other.deleted, failed + other.failed, duration.plus(other.duration), allErrors);
    }

    public boolean hasFailures() {
        return failed > 0 || !errors.isEmpty();
    }
}
