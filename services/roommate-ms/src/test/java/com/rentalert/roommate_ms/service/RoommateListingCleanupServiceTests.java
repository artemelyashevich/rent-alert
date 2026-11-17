package com.rentalert.roommate_ms.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.roommate_ms.repository.RoommateListingCleanupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RoommateListingCleanupServiceTests {

    private final RoommateListingCleanupRepository repository = mock(RoommateListingCleanupRepository.class);
    private final RoommateListingCleanupService service = new RoommateListingCleanupService(repository);

    @Test
    void appliesEachActionForDeletedListings() {
        List<Long> ids = List.of(1L, 2L);

        service.deleteListingReferences(ids);

        InOrder order = inOrder(repository);
        order.verify(repository).deleteSharedFavoritesByListingIdIn(ids);
        order.verify(repository).clearListingIdInListings(ids);
        order.verify(repository).clearListingIdInViewings(ids);
    }

    @Test
    void emptyOrNullInputChangesNothing() {
        service.deleteListingReferences(List.of());
        service.deleteListingReferences(null);

        verifyNoInteractions(repository);
    }
}
