package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.roommate.RoommateComplaintEvent.SubjectType;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RoommateComplaintHandlerTests {

    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final RoommateModerationTaskRepository taskRepository = mock(RoommateModerationTaskRepository.class);
    private final RoommateProfileRepository profileRepository = mock(RoommateProfileRepository.class);
    private final RoommateComplaintHandler handler = new RoommateComplaintHandler(listingRepository, taskRepository, profileRepository);

    @Test
    void profileBelowThresholdOnlyCountsReport() {
        RoommateProfile profile = profile(1);
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        handler.handle(SubjectType.ROOMMATE_PROFILE, 5L);

        assertThat(profile.getReportsCount()).isEqualTo(2);
        assertThat(profile.isSearchable()).isTrue();
        verify(profileRepository).save(profile);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void profileAtThresholdIsHiddenAndTaskOpened() {
        RoommateProfile profile = profile(2);
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));
        when(taskRepository.findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(
            ModerationSubjectType.PROFILE, 5L, "report", ModerationStatus.PENDING_MANUAL)).thenReturn(Optional.empty());

        handler.handle(SubjectType.ROOMMATE_PROFILE, 5L);

        assertThat(profile.isSearchable()).isFalse();
        ArgumentCaptor<RoommateModerationTask> task = ArgumentCaptor.forClass(RoommateModerationTask.class);
        verify(taskRepository).save(task.capture());
        assertThat(task.getValue().getSubjectType()).isEqualTo(ModerationSubjectType.PROFILE);
        assertThat(task.getValue().getSubjectId()).isEqualTo(5L);
        assertThat(task.getValue().getField()).isEqualTo("report");
        assertThat(task.getValue().getStatus()).isEqualTo(ModerationStatus.PENDING_MANUAL);
    }

    @Test
    void existingOpenTaskIsNotDuplicated() {
        RoommateProfile profile = profile(2);
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));
        when(taskRepository.findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(
            ModerationSubjectType.PROFILE, 5L, "report", ModerationStatus.PENDING_MANUAL))
            .thenReturn(Optional.of(RoommateModerationTask.builder().build()));

        handler.handle(SubjectType.ROOMMATE_PROFILE, 5L);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void listingAtThresholdIsPausedAndTaskOpened() {
        RoommateListing listing = listing(2, RoommateListingStatus.ACTIVE);
        when(listingRepository.findById(9L)).thenReturn(Optional.of(listing));
        when(taskRepository.findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(
            ModerationSubjectType.LISTING, 9L, "report", ModerationStatus.PENDING_MANUAL)).thenReturn(Optional.empty());

        handler.handle(SubjectType.ROOMMATE_LISTING, 9L);

        assertThat(listing.getStatus()).isEqualTo(RoommateListingStatus.PAUSED);
        assertThat(listing.getReportsCount()).isEqualTo(3);
        verify(listingRepository).save(listing);
        verify(taskRepository).save(any(RoommateModerationTask.class));
    }

    @Test
    void missingSubjectIsIgnored() {
        when(profileRepository.findById(5L)).thenReturn(Optional.empty());
        when(listingRepository.findById(9L)).thenReturn(Optional.empty());

        handler.handle(SubjectType.ROOMMATE_PROFILE, 5L);
        handler.handle(SubjectType.ROOMMATE_LISTING, 9L);

        verify(profileRepository, never()).save(any());
        verify(listingRepository, never()).save(any());
        verify(taskRepository, never()).save(any());
    }

    private static RoommateProfile profile(int reports) {
        return RoommateProfile.builder()
            .userId(5L)
            .gender(Gender.MALE)
            .city("Минск")
            .reportsCount(reports)
            .searchable(true)
            .build();
    }

    private static RoommateListing listing(int reports, RoommateListingStatus status) {
        RoommateListing listing = new RoommateListing();
        listing.setId(9L);
        listing.setReportsCount(reports);
        listing.setStatus(status);
        return listing;
    }
}
