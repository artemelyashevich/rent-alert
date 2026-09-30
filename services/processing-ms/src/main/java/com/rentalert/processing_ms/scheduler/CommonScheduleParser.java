package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.LockService;
import com.rentalert.dto.GlobalConstants;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.dto.listing.SchedulerSettingsResponse;
import com.rentalert.processing_ms.service.SchedulerSettingsService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("!local")
@Component
public class CommonScheduleParser {

    private static final Duration ITERATION_LOCK_LEASE = Duration.ofMinutes(10);

    private final SchedulerSettingsService schedulerSettingsService;
    private final LockService lockService;
    private final List<AbstractScheduleParser<?>> scheduleParsers;
    private final ScheduledExecutorService schedulerParserExecutor;
    private final Map<AbstractScheduleParser<?>, Instant> nextRun = new ConcurrentHashMap<>();
    private final Set<AbstractScheduleParser<?>> running = ConcurrentHashMap.newKeySet();

    public CommonScheduleParser(
        SchedulerSettingsService schedulerSettingsService,
        LockService lockService,
        List<AbstractScheduleParser<?>> scheduleParsers,
        @Qualifier(GlobalConstants.SCHEDULER_PARSER) ScheduledExecutorService schedulerParserExecutor
    ) {
        this.schedulerSettingsService = schedulerSettingsService;
        this.lockService = lockService;
        this.scheduleParsers = scheduleParsers;
        this.schedulerParserExecutor = schedulerParserExecutor;
    }

    @Scheduled(fixedDelay = 45000, scheduler = GlobalConstants.SCHEDULER_PARSER)
    public void tick() {
        scheduleParsers.forEach(parser -> schedulerParserExecutor.execute(() -> tryRun(parser)));
    }

    private void tryRun(AbstractScheduleParser<?> parser) {
        ScheduleProvider provider = parser.getSchedulerProvider();
        SchedulerSettingsResponse settings = schedulerSettingsService.findByProvider(provider);

        if (Boolean.FALSE.equals(settings.getSchedulerEnabled())) {
            return;
        }

        Instant now = Instant.now();
        Instant nextExecution = nextRun.getOrDefault(parser, Instant.MIN);

        if (now.isBefore(nextExecution)) {
            return;
        }

        if (!running.add(parser)) {
            return;
        }

        try {
            nextRun.put(parser, now.plusMillis(settings.getFixedRateMs()));
            boolean ran = lockService.tryWithLock(
                "scheduled:parser:" + parser.getSchedulerName(),
                ITERATION_LOCK_LEASE,
                parser::schedule
            );
            if (!ran) {
                log.debug("Parser iteration skipped — another instance owns the lock. scheduler={}", parser.getSchedulerName());
            }
        } finally {
            running.remove(parser);
        }
    }
}
