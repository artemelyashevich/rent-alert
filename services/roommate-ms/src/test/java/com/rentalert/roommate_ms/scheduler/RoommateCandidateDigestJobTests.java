package com.rentalert.roommate_ms.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.service.LockService;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.domain.RoommateScenario;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class RoommateCandidateDigestJobTests {

    private final LockService lockService = mock(LockService.class);
    private final RoommateNotificationPublisher publisher = mock(RoommateNotificationPublisher.class);
    private final RoommateProfileRepository profileRepository = mock(RoommateProfileRepository.class);
    private final TransactionTemplate transactionTemplate = new TransactionTemplate(mock(PlatformTransactionManager.class));
    private final RoommateCandidateDigestJob job = new RoommateCandidateDigestJob(lockService, publisher, profileRepository, transactionTemplate);

    private static RoommateProfile profile(Instant lastDigestAt) {
        return RoommateProfile.builder()
            .userId(1L)
            .gender(Gender.MALE)
            .genderPref(new String[0])
            .city("Минск")
            .lastDigestAt(lastDigestAt)
            .build();
    }

    @Test
    void freshCandidatesQueueDigestAndAdvanceLastDigestAt() {
        RoommateProfile profile = profile(null);
        Instant now = Instant.now();
        when(profileRepository.countFreshCandidates(
            eq(1L), eq("Минск"), any(), any(), any(), any(), any(), eq("MALE"), eq(RoommateScenario.PARTNER), any()))
            .thenReturn(3L);

        boolean queued = job.processOne(profile, now);

        assertThat(queued).isTrue();
        assertThat(profile.getLastDigestAt()).isEqualTo(now);
        verify(profileRepository).save(profile);
        verify(publisher).publish(eq(1L), eq(NotificationType.ROOMMATE_NEW_CANDIDATES),
            eq("3 новых кандидата в разделе «Соседи» — загляните в ленту!"),
            eq("ROOMMATE_DIGEST:1:" + now.minusSeconds(6 * 3600).getEpochSecond()));
    }

    @Test
    void noFreshCandidatesAdvancesLastDigestAtButDoesNotNotify() {
        RoommateProfile profile = profile(Instant.now().minusSeconds(3600));
        when(profileRepository.countFreshCandidates(
            anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(0L);

        boolean queued = job.processOne(profile, Instant.now());

        assertThat(queued).isFalse();
        verify(profileRepository).save(profile);
        verify(publisher, Mockito.never()).publish(any(), any(), any(), any());
    }

    @Test
    void wordFormsFollowRussianPluralRules() {
        assertThat(messageFor(1)).startsWith("1 новый кандидат ");
        assertThat(messageFor(22)).startsWith("22 новых кандидата ");
        assertThat(messageFor(12)).startsWith("12 новых кандидатов ");
        assertThat(messageFor(5)).startsWith("5 новых кандидатов ");
    }

    private String messageFor(long count) {
        Mockito.clearInvocations(publisher);
        RoommateProfile profile = profile(Instant.now());
        when(profileRepository.countFreshCandidates(
            anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(count);
        job.processOne(profile, Instant.now());
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(publisher).publish(any(), any(), message.capture(), any());
        return message.getValue();
    }
}
