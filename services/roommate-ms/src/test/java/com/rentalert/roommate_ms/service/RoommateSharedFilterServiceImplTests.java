package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.roommate_ms.domain.RoommateMatch;
import com.rentalert.roommate_ms.repository.RoommateMatchRepository;
import com.rentalert.roommate_ms.service.impl.RoommateSharedFilterServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoommateSharedFilterServiceImplTests {

    private static final Long MATCH_ID = 7L;
    private static final Long USER_ID = 5L;

    private final IdentityMsService identityMsService = mock(IdentityMsService.class);
    private final RoommateMatchRepository matchRepository = mock(RoommateMatchRepository.class);
    private final RoommateSharedFilterService service = new RoommateSharedFilterServiceImpl(
        identityMsService, new RoommateMatchAccess(matchRepository));

    @Test
    void findDelegatesToIdentityForUnlockedMatch() {
        unlockedMatch();
        UserFilterResponse filter = UserFilterResponse.builder().id(3L).active(true).build();
        when(identityMsService.getMatchFilter(MATCH_ID)).thenReturn(Optional.of(filter));

        assertThat(service.find(MATCH_ID, USER_ID)).contains(filter);
    }

    @Test
    void upsertDelegatesToIdentityForUnlockedMatch() {
        unlockedMatch();
        UserFilterRequest request = UserFilterRequest.builder().priceFrom(100).priceTo(500).build();
        UserFilterResponse filter = UserFilterResponse.builder().id(3L).build();
        when(identityMsService.upsertMatchFilter(MATCH_ID, request)).thenReturn(filter);

        assertThat(service.upsert(MATCH_ID, USER_ID, request)).isEqualTo(filter);
    }

    @Test
    void setActivationDelegatesToIdentityForUnlockedMatch() {
        unlockedMatch();
        UserFilterResponse filter = UserFilterResponse.builder().id(3L).active(false).build();
        when(identityMsService.setMatchFilterActivation(MATCH_ID, false)).thenReturn(filter);

        assertThat(service.setActivation(MATCH_ID, USER_ID, false)).isEqualTo(filter);
    }

    @Test
    void lockedMatchForbidsFilterAccess() {
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match()));

        assertThatThrownBy(() -> service.upsert(MATCH_ID, USER_ID, UserFilterRequest.builder().build()))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX403_ROOMMATE_MATCH_NOT_UNLOCKED));
        verifyNoInteractions(identityMsService);
    }

    @Test
    void nonParticipantForbidsFilterAccess() {
        RoommateMatch match = match();
        match.setContactsUnlockedAt(Instant.now());
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

        assertThatThrownBy(() -> service.find(MATCH_ID, 99L))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX403_ROOMMATE_NOT_PARTICIPANT));
        verifyNoInteractions(identityMsService);
    }

    private void unlockedMatch() {
        RoommateMatch match = match();
        match.setContactsUnlockedAt(Instant.now());
        when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
    }

    private static RoommateMatch match() {
        RoommateMatch match = new RoommateMatch();
        match.setId(MATCH_ID);
        match.setUserLoId(USER_ID);
        match.setUserHiId(6L);
        return match;
    }
}
