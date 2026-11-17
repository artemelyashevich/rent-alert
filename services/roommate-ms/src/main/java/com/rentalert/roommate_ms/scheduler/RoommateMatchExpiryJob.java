package com.rentalert.roommate_ms.scheduler;

import com.rentalert.common.service.LockService;
import com.rentalert.roommate_ms.service.RoommateMatchService;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.schedulers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RoommateMatchExpiryJob {

    private static final Duration LOCK_LEASE = Duration.ofMinutes(10);

    private final LockService lockService;
    private final RoommateMatchService roommateMatchService;

    @Scheduled(fixedDelayString = "PT1H")
    public void job() {
        if (!lockService.tryWithLock("scheduled:roommate-match-expiry", LOCK_LEASE, this::expire)) {
            log.debug("Roommate match expiry skipped — another instance owns the lock");
        }
    }

    private void expire() {
        int expired = roommateMatchService.expireStaleMatches();
        if (expired > 0) {
            log.info("Roommate match expiry job: expired {} matches", expired);
        }
    }
}
