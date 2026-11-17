package com.rentalert.roommate_ms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.mapper.RoommateModerationTaskMapper;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.impl.RoommateModerationServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RoommateModerationServiceImplTests {

    private final RoommateModerationTaskRepository taskRepository = mock(RoommateModerationTaskRepository.class);
    private final RoommateProfileRepository profileRepository = mock(RoommateProfileRepository.class);
    private final RoommateListingRepository listingRepository = mock(RoommateListingRepository.class);
    private final RoommateNotificationPublisher notificationPublisher = mock(RoommateNotificationPublisher.class);
    private final RoommateModerationService service = new RoommateModerationServiceImpl(taskRepository, profileRepository,
        listingRepository, Mappers.getMapper(RoommateModerationTaskMapper.class), notificationPublisher);

    private RoommateModerationTask task(ModerationSubjectType subjectType, Long subjectId, String field) {
        RoommateModerationTask task = RoommateModerationTask.builder()
            .subjectType(subjectType)
            .subjectId(subjectId)
            .field(field)
            .submittedText("new text")
            .build();
        task.setId(7L);
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
        return task;
    }

    private RoommateListing listing(Long id, Long authorUserId) {
        RoommateListing listing = RoommateListing.builder().authorUserId(authorUserId).build();
        listing.setId(id);
        when(listingRepository.findById(id)).thenReturn(Optional.of(listing));
        return listing;
    }

    @Test
    void approvedBioIsPublishedAndAuthorNotified() {
        task(ModerationSubjectType.PROFILE, 5L, "bio");
        RoommateProfile profile = RoommateProfile.builder().userId(5L).bioModStatus(ModerationStatus.PENDING_MANUAL).build();
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        assertThat(service.approve(7L, 1L, null))
            .satisfies(response -> {
                assertThat(response.status()).isEqualTo(ModerationStatus.APPROVED);
                assertThat(response.reviewerId()).isEqualTo(1L);
            });
        assertThat(profile.getBioApproved()).isEqualTo("new text");
        assertThat(profile.getBioModStatus()).isEqualTo(ModerationStatus.APPROVED);
        verify(notificationPublisher).publish(5L, NotificationType.ROOMMATE_PROFILE_APPROVED,
            "Ваш текст в разделе «Соседи» прошёл модерацию и опубликован.", "roommate-moderation:7");
    }

    @Test
    void rejectedDescriptionKeepsPreviousTextAndNotifiesListingAuthor() {
        task(ModerationSubjectType.LISTING, 11L, "description");
        RoommateListing listing = listing(11L, 5L);
        listing.setDescriptionApproved("old text");

        service.reject(7L, 1L, "контакты в тексте");

        assertThat(listing.getDescriptionApproved()).isEqualTo("old text");
        assertThat(listing.getDescriptionModStatus()).isEqualTo(ModerationStatus.APPROVED);
        verify(notificationPublisher).publish(5L, NotificationType.ROOMMATE_PROFILE_REJECTED,
            "Текст в разделе «Соседи» не прошёл модерацию: контакты в тексте", "roommate-moderation:7");
    }

    @Test
    void rejectedFirstBioLeavesProfileRejected() {
        task(ModerationSubjectType.PROFILE, 5L, "bio");
        RoommateProfile profile = RoommateProfile.builder().userId(5L).build();
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        service.reject(7L, 1L, null);

        assertThat(profile.getBioModStatus()).isEqualTo(ModerationStatus.REJECTED);
    }

    @Test
    void confirmedComplaintArchivesListingWithoutNotification() {
        task(ModerationSubjectType.LISTING, 11L, "report");
        RoommateListing listing = listing(11L, 5L);
        listing.setStatus(RoommateListingStatus.ACTIVE);

        service.approve(7L, 1L, null);

        assertThat(listing.getStatus()).isEqualTo(RoommateListingStatus.ARCHIVED);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    void dismissedComplaintRestoresProfile() {
        task(ModerationSubjectType.PROFILE, 5L, "report");
        RoommateProfile profile = RoommateProfile.builder().userId(5L).searchable(false).reportsCount(3).build();
        when(profileRepository.findById(5L)).thenReturn(Optional.of(profile));

        service.reject(7L, 1L, null);

        assertThat(profile.isSearchable()).isTrue();
        assertThat(profile.getReportsCount()).isZero();
    }

    @Test
    void resolvedTaskCannotBeDecidedAgain() {
        task(ModerationSubjectType.PROFILE, 5L, "bio").setStatus(ModerationStatus.APPROVED);

        assertThatThrownBy(() -> service.reject(7L, 1L, null))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX409_ROOMMATE_MODERATION_TASK_ALREADY_RESOLVED));
        verifyNoInteractions(notificationPublisher);
    }
}
