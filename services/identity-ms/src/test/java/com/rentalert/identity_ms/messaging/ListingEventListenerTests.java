package com.rentalert.identity_ms.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.dto.event.listing.ListingCreatedEvent;
import com.rentalert.identity_ms.service.NewListingNotificationService;
import com.rentalert.kafka.NonRetryableEventException;
import com.rentalert.kafka.inbox.InboxGuard;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListingEventListenerTests {

    private final NewListingNotificationService notificationService = mock(NewListingNotificationService.class);
    private final InboxGuard inboxGuard = mock(InboxGuard.class);
    private final ListingEventListener listener = createListener();

    private ListingEventListener createListener() {
        ListingEventListener created = new ListingEventListener(notificationService);
        ReflectionTestUtils.setField(created, "inboxGuard", inboxGuard);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(2).run();
            return true;
        }).when(inboxGuard).processOnce(any(), any(), any());
        return created;
    }

    private static ListingCreatedEvent listing(Long listingId) {
        return ListingCreatedEvent.builder().listingId(listingId).message("new flat").build();
    }

    @Test
    void listingIsNotifiedThroughInboxWithDerivedKey() {
        ListingCreatedEvent event = listing(7L);

        listener.onListingCreated(event);

        UUID expectedKey = UUID.nameUUIDFromBytes("ListingCreatedEvent:7".getBytes(StandardCharsets.UTF_8));
        verify(inboxGuard).processOnce(eq("identity.listings"), eq(expectedKey), any(Runnable.class));
        verify(notificationService).notifyMatchingUsers(event);
    }

    @Test
    void duplicateListingDoesNotNotifyAgain() {
        doReturn(false).when(inboxGuard).processOnce(any(), any(), any());

        listener.onListingCreated(listing(7L));

        verifyNoInteractions(notificationService);
    }

    @Test
    void listingWithoutIdIsNonRetryable() {
        assertThatThrownBy(() -> listener.onListingCreated(ListingCreatedEvent.builder().message("new flat").build()))
            .isInstanceOf(NonRetryableEventException.class);
        verifyNoInteractions(inboxGuard, notificationService);
    }
}
