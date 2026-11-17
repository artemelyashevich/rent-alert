package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.roommate_ms.domain.RoommateHiddenEmbeddedId;
import com.rentalert.roommate_ms.domain.RoommateHiddenSubjectType;
import com.rentalert.roommate_ms.repository.RoommateHiddenRepository;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.service.impl.RoommateHiddenServiceImpl;
import org.junit.jupiter.api.Test;

class RoommateHiddenServiceImplTests {

    private final RoommateHiddenRepository hiddenRepository = mock(RoommateHiddenRepository.class);
    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final RoommateHiddenService service = new RoommateHiddenServiceImpl(hiddenRepository, listingRepository);

    @Test
    void skippedCandidateIsHidden() {
        service.skipCandidate(5L, 6L);

        verify(hiddenRepository).save(argThat(hidden ->
            hidden.getId().equals(new RoommateHiddenEmbeddedId(5L, RoommateHiddenSubjectType.ROOMMATE_PROFILE, 6L))));
    }

    @Test
    void skippingTwiceIsNoOp() {
        when(hiddenRepository.existsById(new RoommateHiddenEmbeddedId(5L, RoommateHiddenSubjectType.ROOMMATE_PROFILE, 6L))).thenReturn(true);

        service.skipCandidate(5L, 6L);

        verify(hiddenRepository, never()).save(any());
    }

    @Test
    void selfCannotBeSkipped() {
        assertThatThrownBy(() -> service.skipCandidate(5L, 5L))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_SELF_ACTION));
    }

    @Test
    void missingListingCannotBeSkipped() {
        assertThatThrownBy(() -> service.skipListing(5L, 11L))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND));
    }

    @Test
    void existingListingIsHidden() {
        when(listingRepository.existsById(11L)).thenReturn(true);

        service.skipListing(5L, 11L);

        verify(hiddenRepository).save(argThat(hidden -> hidden.getId().getSubjectType() == RoommateHiddenSubjectType.ROOMMATE_LISTING));
    }
}
