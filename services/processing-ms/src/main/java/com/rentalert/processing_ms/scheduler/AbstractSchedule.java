package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.MetricService;
import com.rentalert.processing_ms.config.ApplicationProperties;
import com.rentalert.dto.GlobalConstants;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Profile("!integration-test")
@ConditionalOnProperty(
        value = "app.schedulers.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public abstract class AbstractSchedule {

    @Autowired
    private MetricService metricService;

    @Autowired
    public ApplicationProperties applicationProperties;

    private final AtomicBoolean active = new AtomicBoolean(true);

    private ThreadPoolTaskExecutor executor;

    private final RateLimiter rateLimiter = RateLimiter.of("scheduler-rate-limiter", RateLimiterConfig.custom()
            .limitForPeriod(2)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofDays(1))
            .build());

    @PostConstruct
    public void init() {
        final int poolSize = getThreadPoolSize();
        final int queueCapacity = getQueueCapacity();
        final String schedulerName = getSchedulerName();

        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(schedulerName + "-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        log.info("Initialized scheduler [{}]. poolSize={}, queueCapacity={}", schedulerName, poolSize, queueCapacity);
    }

    @PreDestroy
    public void destroy() {
        active.set(false);
        if (executor != null) {
            executor.shutdown();
        }
        log.info("Scheduler destroyed: {}", getSchedulerName());
    }

    protected boolean isActive() {
        return active.get();
    }

    protected void submitTask(Runnable task) {
        executor.execute(task);
    }

    protected void acquireRateLimit() {
        rateLimiter.acquirePermission();
    }

    protected void collectExecutorMetrics() {
        ThreadPoolExecutor threadPool = executor.getThreadPoolExecutor();
        String scheduler = getSchedulerName();

        metricService.gauge(
                "scheduler.executor.active",
                threadPool.getActiveCount(),
                metricService.tag(GlobalConstants.SCHEDULER, scheduler)
        );
        metricService.gauge(
                "scheduler.executor.queue",
                threadPool.getQueue().size(),
                metricService.tag(GlobalConstants.SCHEDULER, scheduler)
        );
        metricService.gauge(
                "scheduler.executor.pool",
                threadPool.getPoolSize(),
                metricService.tag(GlobalConstants.SCHEDULER, scheduler)
        );
    }

    protected MetricService getMetricService() {
        return metricService;
    }

    protected abstract String getSchedulerName();

    protected int getThreadPoolSize() {
        return 3;
    }

    protected int getQueueCapacity() {
        return 100;
    }
}
