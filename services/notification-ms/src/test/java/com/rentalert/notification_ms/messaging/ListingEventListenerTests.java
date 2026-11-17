package com.rentalert.notification_ms.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.rentalert.dto.event.listing.ListingsDeletedEvent;
import com.rentalert.notification_ms.service.NotificationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ListingEventListenerTests {

    private final NotificationService notificationService = mock(NotificationService.class);
    private final ListingEventListener listener = createListener();

    private ListingEventListener createListener() {
        ListingEventListener created = new ListingEventListener(notificationService);
        ReflectionTestUtils.setField(created, "inboxGuard", new InMemoryInboxGuard());
        return created;
    }

    @Test
    void deletesNotificationsOfDeletedListings() {
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(3L, 9L)));

        verify(notificationService).deleteByListingIds(List.of(3L, 9L));
    }

    @Test
    void redeliveredEventDeletesOnlyOnce() {
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(3L, 9L)));
        listener.onListingsDeleted(new ListingsDeletedEvent(List.of(3L, 9L)));

        verify(notificationService, times(1)).deleteByListingIds(List.of(3L, 9L));
    }
}
