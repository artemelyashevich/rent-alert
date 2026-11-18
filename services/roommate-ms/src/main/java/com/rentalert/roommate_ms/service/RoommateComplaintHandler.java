package com.rentalert.roommate_ms.service;

import com.rentalert.dto.event.roommate.RoommateComplaintEvent;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoommateComplaintHandler {

    private static final String FIELD_REPORT = "report";
    private static final int REPORTS_THRESHOLD = 3;

    private final RoommateListingRepository listingRepository;
    private final RoommateModerationTaskRepository taskRepository;
    private final RoommateProfileRepository profileRepository;

    public void handle(RoommateComplaintEvent.SubjectType subjectType, Long subjectId) {
        if (subjectType == RoommateComplaintEvent.SubjectType.ROOMMATE_PROFILE) {
            profileRepository.findById(subjectId).ifPresent(this::handleProfile);
        } else if (subjectType == RoommateComplaintEvent.SubjectType.ROOMMATE_LISTING) {
            listingRepository.findById(subjectId).ifPresent(this::handleListing);
        }
    }

    private void handleProfile(RoommateProfile profile) {
        profile.setReportsCount(profile.getReportsCount() + 1);
        if (profile.getReportsCount() >= REPORTS_THRESHOLD && profile.isSearchable()) {
            profile.setSearchable(false);
            openReportTask(ModerationSubjectType.PROFILE, profile.getUserId());
            log.info("Roommate profile auto-hidden after reports threshold. userId={}", profile.getUserId());
        }
        profileRepository.save(profile);
    }

    private void handleListing(RoommateListing listing) {
        listing.setReportsCount(listing.getReportsCount() + 1);
        if (listing.getReportsCount() >= REPORTS_THRESHOLD && listing.getStatus() != RoommateListingStatus.PAUSED) {
            listing.setStatus(RoommateListingStatus.PAUSED);
            openReportTask(ModerationSubjectType.LISTING, listing.getId());
            log.info("Roommate listing auto-paused after reports threshold. id={}", listing.getId());
        }
        listingRepository.save(listing);
    }

    private void openReportTask(ModerationSubjectType subjectType, Long subjectId) {
        boolean alreadyOpen = taskRepository
            .findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(subjectType, subjectId, FIELD_REPORT, ModerationStatus.PENDING_MANUAL)
            .isPresent();
        if (alreadyOpen) {
            return;
        }
        taskRepository.save(RoommateModerationTask.builder()
            .subjectType(subjectType)
            .subjectId(subjectId)
            .field(FIELD_REPORT)
            .status(ModerationStatus.PENDING_MANUAL)
            .build());
    }
}
