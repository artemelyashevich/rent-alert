package com.rentalert.roommate_ms.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import com.rentalert.roommate_ms.repository.RoommateUserDataRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RoommateUserDataServiceTests {

    private final RoommateUserDataRepository repository = mock(RoommateUserDataRepository.class);
    private final RoommateUserDataService service = new RoommateUserDataService(repository);

    @Test
    void deletesMatchesBeforeListingsAndProfile() {
        service.deleteUserData(5L);

        InOrder order = inOrder(repository);
        order.verify(repository).deleteMatchesOfUser(5L);
        order.verify(repository).deleteHiddenOfUser(5L);
        order.verify(repository).deleteModerationTasksOfUser(5L);
        order.verify(repository).deleteListingsOfUser(5L);
        order.verify(repository).deleteProfileOfUser(5L);
    }
}
