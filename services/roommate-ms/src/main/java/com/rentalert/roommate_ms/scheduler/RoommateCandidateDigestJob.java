package com.rentalert.roommate_ms.scheduler;

import com.rentalert.common.service.LockService;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.util.RoommateGenderPrefUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@ConditionalOnProperty(value = "app.schedulers.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class RoommateCandidateDigestJob {

    private static final Duration RUN_INTERVAL = Duration.ofHours(6);
    private static final Duration FIRST_RUN_WINDOW = RUN_INTERVAL;
    private static final Duration LOCK_LEASE = Duration.ofMinutes(15);

    private final LockService lockService;
    private final RoommateNotificationPublisher notificationPublisher;
    private final RoommateProfileRepository profileRepository;
    private final TransactionTemplate transactionTemplate;

    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT5M")
    public void job() {
        if (!lockService.tryWithLock("scheduled:roommate-candidate-digest", LOCK_LEASE, this::digest)) {
            log.debug("Roommate candidate digest skipped — another instance owns the lock");
        }
    }

    private void digest() {
        Instant now = Instant.now();
        List<RoommateProfile> ready = profileRepository.findAllByActiveTrueAndSearchableTrueAndBioModStatusIn(
            List.of(ModerationStatus.APPROVED_AUTO, ModerationStatus.APPROVED));

        int notified = 0;
        for (RoommateProfile profile : ready) {
            Boolean queued = transactionTemplate.execute(status -> processOne(profile, now));
            notified += Boolean.TRUE.equals(queued) ? 1 : 0;
        }
        log.info("Roommate candidate digest: profiles scanned={}, notified={}", ready.size(), notified);
    }

    boolean processOne(RoommateProfile profile, Instant now) {
        Instant since = profile.getLastDigestAt() != null ? profile.getLastDigestAt() : now.minus(FIRST_RUN_WINDOW);

        long fresh = profileRepository.countFreshCandidates(
            profile.getUserId(), profile.getCity(),
            profile.getBudgetMin(), profile.getBudgetMax(),
            profile.getMoveInFrom(), profile.getMoveInTo(),
            RoommateGenderPrefUtil.resolve(profile.getGenderPref()), profile.getGender().name(),
            RoommateScenario.PARTNER, since);

        profile.setLastDigestAt(now);
        profileRepository.save(profile);

        if (fresh <= 0) {
            return false;
        }
        String message = fresh + " " + candidatesWord(fresh) + " в разделе «Соседи» — загляните в ленту!";
        notificationPublisher.publish(profile.getUserId(), NotificationType.ROOMMATE_NEW_CANDIDATES, message,
            "ROOMMATE_DIGEST:" + profile.getUserId() + ":" + since.getEpochSecond());
        return true;
    }

    private String candidatesWord(long count) {
        long mod100 = count % 100;
        long mod10 = count % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return "новых кандидатов";
        }
        if (mod10 == 1) {
            return "новый кандидат";
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return "новых кандидата";
        }
        return "новых кандидатов";
    }
}
