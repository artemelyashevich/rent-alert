package com.rentalert.common.service;

import lombok.NonNull;

import java.time.Duration;
import java.util.function.Supplier;

public interface LockService {

    void withLock(String lockName, Runnable action);

    <T> T withLock(String lockName, Supplier<T> action);

    boolean tryWithLock(@NonNull String lockName, @NonNull Duration leaseTime, @NonNull Runnable action);

    boolean isLocked(@NonNull String lockName);
}
