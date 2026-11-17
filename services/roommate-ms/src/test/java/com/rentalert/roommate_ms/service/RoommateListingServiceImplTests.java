package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.listing.ListingDto;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.internal.client.service.ListingMsService;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.dto.RoommateListingRequest;
import com.rentalert.roommate_ms.mapper.RoommateListingMapper;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.service.impl.RoommateListingServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;

class RoommateListingServiceImplTests {

    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final ListingMsService listingMsService = mock(ListingMsService.class);
    private final RoommateModerationGate moderationGate = mock(RoommateModerationGate.class);
    private final RoommateListingService service = new RoommateListingServiceImpl(listingRepository, listingMsService,
        Mappers.getMapper(RoommateListingMapper.class), moderationGate);

    private final RoommateListingRequest request = RoommateListingRequest.builder().city("Минск").description("Светлая комната").build();

    @BeforeEach
    void savesAssignId() {
        when(listingRepository.save(any())).thenAnswer(invocation -> {
            RoommateListing listing = invocation.getArgument(0);
            if (listing.getId() == null) {
                listing.setId(11L);
            }
            return listing;
        });
        when(listingRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private RoommateListing owned(Long authorUserId) {
        RoommateListing listing = RoommateListing.builder().authorUserId(authorUserId).descriptionSubmitted("Светлая комната").build();
        listing.setId(11L);
        when(listingRepository.findById(11L)).thenReturn(Optional.of(listing));
        return listing;
    }

    @Test
    void autoApprovedListingIsActiveRightAway() {
        when(moderationGate.moderate(ModerationSubjectType.LISTING, 11L, "description", "Светлая комната"))
            .thenReturn(ModerationStatus.APPROVED_AUTO);

        assertThat(service.create(5L, request))
            .satisfies(response -> {
                assertThat(response.authorUserId()).isEqualTo(5L);
                assertThat(response.status()).isEqualTo(RoommateListingStatus.ACTIVE);
                assertThat(response.descriptionApproved()).isEqualTo("Светлая комната");
                assertThat(response.genderPref()).containsExactly("MALE", "FEMALE");
            });
    }

    @Test
    void suspiciousDescriptionWaitsForModeration() {
        when(moderationGate.moderate(any(), any(), anyString(), any())).thenReturn(ModerationStatus.PENDING_MANUAL);

        assertThat(service.create(5L, request).status()).isEqualTo(RoommateListingStatus.PENDING_MODERATION);
    }

    @Test
    void missingLinkedListingIsRejected() {
        request.setListingId(42L);
        when(listingMsService.findCards(List.of(42L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(5L, request))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX404_LISTING_NOT_FOUND));
        verify(listingRepository, never()).save(any());
    }

    @Test
    void existingLinkedListingIsStored() {
        request.setListingId(42L);
        when(listingMsService.findCards(List.of(42L))).thenReturn(List.of(ListingDto.builder().id(42L).build()));
        when(moderationGate.moderate(any(), any(), anyString(), any())).thenReturn(ModerationStatus.APPROVED_AUTO);

        assertThat(service.create(5L, request).listingId()).isEqualTo(42L);
    }

    @Test
    void unchangedDescriptionIsNotModeratedAgain() {
        owned(5L);

        service.update(11L, 5L, request);

        verify(moderationGate, never()).moderate(any(), any(), anyString(), any());
    }

    @Test
    void foreignListingIsForbidden() {
        owned(6L);

        assertThatThrownBy(() -> service.getOwned(11L, 5L))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX403_ROOMMATE_LISTING_NOT_OWNER));
    }

    @Test
    void ownerCannotSetModerationStatuses() {
        assertThatThrownBy(() -> service.setStatus(11L, 5L, RoommateListingStatus.PENDING_MODERATION))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_LISTING_STATUS_NOT_ALLOWED));
    }

    @Test
    void unapprovedListingCannotBeActivated() {
        owned(5L).setDescriptionModStatus(ModerationStatus.PENDING_MANUAL);

        assertThatThrownBy(() -> service.setStatus(11L, 5L, RoommateListingStatus.ACTIVE))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX400_ROOMMATE_DESCRIPTION_NOT_APPROVED));
    }

    @Test
    void secondActiveListingIsConflict() {
        owned(5L).setDescriptionModStatus(ModerationStatus.APPROVED);
        when(listingRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_roommate_listings_active_author"));

        assertThatThrownBy(() -> service.setStatus(11L, 5L, RoommateListingStatus.ACTIVE))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX409_ROOMMATE_LISTING_ALREADY_ACTIVE));
    }

    @Test
    void pausingDoesNotNeedApproval() {
        owned(5L).setDescriptionModStatus(ModerationStatus.PENDING_MANUAL);

        assertThat(service.setStatus(11L, 5L, RoommateListingStatus.PAUSED).status()).isEqualTo(RoommateListingStatus.PAUSED);
        verify(listingRepository).save(any());
        verify(listingRepository, never()).saveAndFlush(any());
    }
}
