package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.common.service.LockService;
import com.rentalert.dto.GlobalConstants;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.domain.ScheduleTask;
import com.rentalert.processing_ms.repository.ListingRepository;
import com.rentalert.processing_ms.repository.ScheduleTaskRepository;
import com.rentalert.processing_ms.service.CityService;
import com.rentalert.processing_ms.service.ListingService;
import com.rentalert.processing_ms.service.ScheduledTaskRecordService;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import com.rentalert.redis.dto.CacheType;
import com.rentalert.redis.service.CacheService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.Tag;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public abstract class AbstractScheduleParser<T> extends AbstractSchedule {

    @Autowired
    protected SchedulerSettingsService schedulerSettingsService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private LockService lockService;
    @Autowired
    private ScheduleTaskRepository scheduleTaskRepository;
    @Autowired
    private ScheduledTaskRecordService taskRecordService;
    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ListingService listingService;

    @Autowired
    private CityService cityService;

    /** Базовая задержка между запросами по разным городам, мс. */
    @Value("${app.parsers.city-request-delay-ms:1000}")
    private long cityRequestDelayMs;

    /** Верхняя граница случайного джиттера, добавляемого к задержке между городами, мс. */
    @Value("${app.parsers.city-request-jitter-ms:500}")
    private long cityRequestJitterMs;

    @Override
    @PostConstruct
    public void init() {
        super.init();
        scheduleTaskRepository.findByProvider(getSchedulerProvider())
            .orElseGet(() -> scheduleTaskRepository.save(
                ScheduleTask.builder()
                    .attempt(1L)
                    .provider(getSchedulerProvider())
                    .build()
            ));
        warmupProcessedCache();
    }

    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT5M")
    public void refreshProcessedCache() {
        if (!isActive()) {
            return;
        }
        warmupProcessedCache();
    }

    public void schedule() {
        if (!isActive()) {
            return;
        }

        if (isMaintenanceInProgress()) {
            log.debug("Maintenance job in progress, skipping scheduler iteration. scheduler={}", getSchedulerProvider());
            return;
        }

        try {
            log.info("Scheduler started: {}", getSchedulerName());

            if (isMaintenanceInProgress()) {
                log.info("Maintenance job started mid-iteration, stopping further submissions. scheduler={}", getSchedulerProvider());
                return;
            }

            List<City> cities = getActiveCities();
            if (cities.isEmpty()) {
                log.warn("No active cities configured, skipping iteration. scheduler={}", getSchedulerProvider());
                return;
            }

            int totalFetched = 0;
            for (int i = 0; i < cities.size(); i++) {
                if (!isActive() || isMaintenanceInProgress()) {
                    return;
                }

                if (i > 0 && !sleepBetweenCities()) {
                    return;
                }

                City city = cities.get(i);
                List<T> tasks;
                try {
                    tasks = findTasks(city);
                } catch (CallNotPermittedException e) {
                    log.warn("Circuit breaker is open, skipping iteration. scheduler={}", getSchedulerName());
                    getMetricService().increment(
                        "scheduler.circuit.open",
                        getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                    );
                    return;
                } catch (Exception e) {
                    if (e instanceof UpstreamHttpException upstream && upstream.isRateLimited()) {
                        log.warn("Source rate limit hit, stopping iteration. scheduler={}, city={}, retryAfterSeconds={}",
                            getSchedulerName(), city.getName(), upstream.getRetryAfterSeconds());
                        getMetricService().increment(
                            "scheduler.city.rate_limited",
                            getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                        );
                        return;
                    }
                    log.error("Failed to fetch tasks for city. scheduler={}, city={}", getSchedulerProvider(), city.getName(), e);
                    getMetricService().increment(
                        "scheduler.city.fetch.error",
                        getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                    );
                    continue;
                }

                if (tasks == null || tasks.isEmpty()) {
                    continue;
                }
                totalFetched += tasks.size();
                submitTasks(tasks, city);
            }

            if (totalFetched > 0) {
                getMetricService().increment(
                    "scheduler.tasks.fetched",
                    totalFetched,
                    getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                );
            }

            collectExecutorMetrics();

        } catch (Exception e) {
            log.error("Scheduler iteration failed. scheduler={}", getSchedulerProvider(), e);
            getMetricService().increment(
                "scheduler.iteration.error",
                getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
            );
        }
    }

    protected List<City> getActiveCities() {
        return cityService.findActiveByProvider(getSchedulerProvider());
    }

    private void submitTasks(List<T> tasks, City city) {
        for (T task : tasks) {
            if (!isActive()) {
                return;
            }
            String key = getTaskKey(task);

            if (isProcessed(key)) {
                continue;
            }

            boolean acquired = cacheService.putIfAbsent(CacheType.SCHEDULER_IN_FLIGHT, key, Boolean.TRUE);
            if (!acquired) {
                log.debug("Task already in-flight. scheduler={}, key={}", getSchedulerProvider(), key);
                getMetricService().increment(
                    "scheduler.task.duplicate",
                    getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                );
                continue;
            }

            try {
                taskRecordService.markPending(getSchedulerProvider(), key);
            } catch (Exception e) {
                log.warn("Failed to persist PENDING status. scheduler={}, key={}", getSchedulerProvider(), key, e);
            }

            try {
                submitTask(() -> executeSafely(task, key, city));
            } catch (Exception e) {
                cacheService.evict(CacheType.SCHEDULER_IN_FLIGHT, key);
                log.error("Task rejected. scheduler={}, key={}", getSchedulerProvider(), key, e);
                getMetricService().increment(
                    "scheduler.task.rejected",
                    getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name())
                );
                // Очередь переполнена — остальные задачи пачки тоже будут отклонены.
                return;
            }
        }
    }

    private boolean sleepBetweenCities() {
        long jitter = cityRequestJitterMs > 0
            ? ThreadLocalRandom.current().nextLong(cityRequestJitterMs + 1)
            : 0;
        long pauseMs = Math.max(0, cityRequestDelayMs) + jitter;
        if (pauseMs == 0) {
            return true;
        }
        try {
            Thread.sleep(pauseMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while pacing city requests. scheduler={}", getSchedulerProvider());
            return false;
        }
    }

    protected void warmupProcessedCache() {
        boolean ran = lockService.tryWithLock(
            "scheduled:warmup:" + getSchedulerProvider(),
            Duration.ofMinutes(5),
            this::doWarmupProcessedCache
        );
        if (!ran) {
            log.debug("Processed cache warm-up skipped — another instance owns the lock. scheduler={}",
                getSchedulerProvider());
        }
    }

    private void doWarmupProcessedCache() {
        try {
            List<String> successKeys = taskRecordService.getSuccessKeysForWarmUp(getSchedulerProvider(), getProcessedTtl());
            if (successKeys.isEmpty()) {
                log.info("No processed keys to warm up, keeping existing cache. scheduler={}", getSchedulerProvider());
                return;
            }
            cacheService.evict(getProcessedCacheType());
            cacheService.addToSetBatch(getProcessedCacheType(), successKeys, getProcessedTtl());
            log.info("Processed cache refreshed. scheduler={}, keys={}", getSchedulerProvider(), successKeys.size());
        } catch (Exception e) {
            log.warn("Cache warm-up failed. scheduler={}", getSchedulerProvider(), e);
        }
    }

    private void executeSafely(T task, String key, City city) {
        MDC.put(GlobalConstants.TRACE_ID, UUID.randomUUID().toString());
        acquireRateLimit();

        final String schedulerName = getSchedulerProvider().name();

        LongTaskTimer.Sample longTask = getMetricService().startLongTask(
            "scheduler.task.active",
            getMetricService().tag(GlobalConstants.SCHEDULER, schedulerName)
        );
        try {
            getMetricService().recordTime(
                "scheduler.task.execution",
                () -> {
                    ListingOutcome outcome = execute(task, city);
                    markProcessed(key);
                    if (outcome == ListingOutcome.CREATED) {
                        afterSuccessExecute(task, city);
                        log.debug("New listing processed. scheduler={}, key={}", schedulerName, key);
                    } else {
                        log.debug("Listing already exists, marked processed without notification. scheduler={}, key={}", schedulerName, key);
                    }
                },
                getMetricService().tag(GlobalConstants.SCHEDULER, schedulerName)
            );
        } catch (Exception e) {
            getMetricService().increment(
                "scheduler.task.error",
                getMetricService().tag(GlobalConstants.SCHEDULER, schedulerName)
            );
            try {
                taskRecordService.markError(getSchedulerProvider(), key, e);
            } catch (Exception dbEx) {
                log.warn("Failed to persist ERROR status. scheduler={}, key={}", schedulerName, key, dbEx);
            }
            onError(task, e);
        } finally {
            getMetricService().stopLongTask(longTask);
            cacheService.evict(CacheType.SCHEDULER_IN_FLIGHT, key);
            MDC.remove(GlobalConstants.TRACE_ID);
        }
    }

    protected boolean isProcessed(String key) {
        return cacheService.isMember(getProcessedCacheType(), key);
    }

    protected void markProcessed(String key) {
        cacheService.addToSet(getProcessedCacheType(), key, getProcessedTtl());
        try {
            taskRecordService.markSuccess(getSchedulerProvider(), key);
        } catch (Exception e) {
            log.warn("Failed to persist SUCCESS status. scheduler={}, key={}", getSchedulerProvider(), key, e);
        }
    }

    protected boolean isMaintenanceInProgress() {
        try {
            return lockService.isLocked(GlobalConstants.MAINTENANCE_LOCK);
        } catch (Exception e) {
            log.warn("Failed to check maintenance lock, assuming not in progress. scheduler={}", getSchedulerProvider(), e);
            return false;
        }
    }

    protected abstract List<T> findTasks(City city);

    protected abstract String getTaskKey(T task);

    protected abstract Listing getListing(T task);

    protected ListingOutcome execute(T task, City city) {
        Listing listing = getListing(task);
        if (city != null) {
            listing.setCity(city.getName());
        }

        if (listingRepository.existsBySourceAndSourceId(listing.getSource(), listing.getSourceId())) {
            log.debug("Listing already persisted, skipping save. source={}, sourceId={}",
                listing.getSource(), listing.getSourceId());
            return ListingOutcome.DUPLICATE;
        }

        try {
            listingService.saveNew(listing);
        } catch (DataIntegrityViolationException e) {
            log.info("Listing was persisted concurrently by another worker. source={}, sourceId={}",
                listing.getSource(), listing.getSourceId());
            return ListingOutcome.DUPLICATE;
        }

        Tag providerTag = getMetricService().tag(GlobalConstants.SCHEDULER, getSchedulerProvider().name());
        getMetricService().increment("listing.new.processed", providerTag);
        getMetricService().recordCpuUsage("listing.process.cpu.usage", providerTag);

        return ListingOutcome.CREATED;
    }

    protected abstract ScheduleProvider getSchedulerProvider();

    protected void afterSuccessExecute(T task, City city) {
        log.info("Received new listing: {}, {}", getSchedulerProvider(), city.getName());
    }

    protected void onError(T task, Exception e) {
        log.error("Task execution failed. scheduler={}, task={}", getSchedulerProvider(), task, e);
    }

    @Override
    public String getSchedulerName() {
        return getSchedulerProvider().name();
    }

    @Override
    public boolean isActive() {
        return schedulerSettingsService.findByProvider(getSchedulerProvider()).getSchedulerEnabled();
    }

    protected Duration getProcessedTtl() {
        return Duration.ofHours(84); // 3.5 days
    }

    protected CacheType getProcessedCacheType() {
        return switch (getSchedulerProvider()) {
            case KUFAR -> CacheType.KUFAR_PROCESSED;
            case ONLINER -> CacheType.ONLINER_PROCESSED;
            case REALT -> CacheType.REALT_PROCESSED;
        };
    }

    protected SchedulerSettingsResponse getSettings() {
        return schedulerSettingsService.findByProvider(getSchedulerProvider());
    }

    protected enum ListingOutcome {
        CREATED,
        DUPLICATE
    }
}
