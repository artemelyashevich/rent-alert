package com.rentalert.roommate_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.roommate.RoommateComplaintEvent;
import com.rentalert.dto.event.roommate.RoommateComplaintEvent.SubjectType;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import com.rentalert.roommate_ms.service.RoommateComplaintHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RoommateComplaintListenerTests {

    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final RoommateComplaintHandler handler = mock(RoommateComplaintHandler.class);
    private final RoommateComplaintListener listener = createListener();

    private RoommateComplaintListener createListener() {
        RoommateComplaintListener created = new RoommateComplaintListener(handler);
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        return created;
    }

    @Test
    void complaintIsHandledThroughInboxWithEventIdAsKey() {
        UUID eventId = UUID.randomUUID();

        listener.onComplaint(new RoommateComplaintEvent(eventId, SubjectType.ROOMMATE_PROFILE, 7L));

        verify(inboxGuard).processOnce(eq("roommate.complaints"), eq(eventId), any(Runnable.class));
    }

    @Test
    void inboxRunsHandlerWithSubjectOfEvent() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(inboxGuard).processOnce(eq("roommate.complaints"), any(), any(Runnable.class));

        listener.onComplaint(new RoommateComplaintEvent(UUID.randomUUID(), SubjectType.ROOMMATE_LISTING, 9L));

        verify(handler).handle(SubjectType.ROOMMATE_LISTING, 9L);
    }

    @Test
    void missingEventIdIsNonRetryable() {
        assertThatThrownBy(() -> listener.onComplaint(new RoommateComplaintEvent(null, SubjectType.ROOMMATE_PROFILE, 7L)))
            .isInstanceOf(NonRetryableEventException.class);
        verifyNoInteractions(inboxGuard, handler);
    }

    @Test
    void missingSubjectTypeOrIdIsNonRetryable() {
        UUID eventId = UUID.randomUUID();

        assertThatThrownBy(() -> listener.onComplaint(new RoommateComplaintEvent(eventId, null, 7L)))
            .isInstanceOf(NonRetryableEventException.class);
        assertThatThrownBy(() -> listener.onComplaint(new RoommateComplaintEvent(eventId, SubjectType.ROOMMATE_PROFILE, null)))
            .isInstanceOf(NonRetryableEventException.class);
        verify(inboxGuard, never()).processOnce(any(), any(), any());
    }
}
