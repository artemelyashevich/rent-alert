package com.rentalert.roommate_ms.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.service.LockService;
import com.rentalert.roommate_ms.service.RoommateMatchService;
import org.junit.jupiter.api.Test;

class RoommateMatchExpiryJobTests {

    private final LockService lockService = mock(LockService.class);
    private final RoommateMatchService matchService = mock(RoommateMatchService.class);
    private final RoommateMatchExpiryJob job = new RoommateMatchExpiryJob(lockService, matchService);

    @Test
    void jobRunsExpiryUnderLock() {
        when(matchService.expireStaleMatches()).thenReturn(2);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(lockService).tryWithLock(eq("scheduled:roommate-match-expiry"), any(), any(Runnable.class));

        job.job();

        verify(matchService).expireStaleMatches();
    }

    @Test
    void jobDoesNothingWhenAnotherInstanceHoldsLock() {
        when(lockService.tryWithLock(eq("scheduled:roommate-match-expiry"), any(), any(Runnable.class))).thenReturn(false);

        job.job();

        verify(matchService, never()).expireStaleMatches();
    }
}
