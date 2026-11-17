package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.notification.NotificationType;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.roommate_ms.domain.RoommateListing;
import com.rentalert.roommate_ms.domain.RoommateListingStatus;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.mapper.RoommateModerationTaskMapper;
import com.rentalert.roommate_ms.messaging.RoommateNotificationPublisher;
import com.rentalert.roommate_ms.repository.RoommateListingRepository;
import com.rentalert.roommate_ms.repository.RoommateModerationTaskRepository;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.RoommateModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateModerationServiceImpl implements RoommateModerationService {

    private static final String FIELD_REPORT = "report";

    private final RoommateModerationTaskRepository taskRepository;
    private final RoommateProfileRepository profileRepository;
    private final RoommateListingRepository listingRepository;
    private final RoommateModerationTaskMapper taskMapper;
    private final RoommateNotificationPublisher notificationPublisher;

    @Override
    @Transactional(readOnly = true)
    public Page<RoommateModerationTaskResponse> queue(Pageable pageable) {
        return taskRepository.findAllByStatusOrderByCreatedAtAsc(ModerationStatus.PENDING_MANUAL, pageable)
            .map(taskMapper::toResponse);
    }

    @Override
    @Transactional
    public RoommateModerationTaskResponse approve(Long taskId, Long adminId, String reason) {
        RoommateModerationTask task = requirePending(taskId);
        if (FIELD_REPORT.equals(task.getField())) {
            blockSubject(task.getSubjectType(), task.getSubjectId());
        } else {
            approveField(task);
        }
        return resolve(task, ModerationStatus.APPROVED, adminId, reason);
    }

    @Override
    @Transactional
    public RoommateModerationTaskResponse reject(Long taskId, Long adminId, String reason) {
        RoommateModerationTask task = requirePending(taskId);
        if (FIELD_REPORT.equals(task.getField())) {
            restoreSubject(task.getSubjectType(), task.getSubjectId());
        } else {
            rejectField(task);
        }
        return resolve(task, ModerationStatus.REJECTED, adminId, reason);
    }

    private RoommateModerationTask requirePending(Long taskId) {
        RoommateModerationTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_MODERATION_TASK_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (task.getStatus() != ModerationStatus.PENDING_MANUAL) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_MODERATION_TASK_ALREADY_RESOLVED, HttpStatus.CONFLICT);
        }
        return task;
    }

    private RoommateModerationTaskResponse resolve(RoommateModerationTask task, ModerationStatus outcome, Long adminId, String reason) {
        task.setStatus(outcome);
        task.setReviewerId(adminId);
        task.setDecisionReason(reason);
        taskRepository.save(task);
        log.info("Roommate moderation task resolved. id={}, outcome={}, adminId={}", task.getId(), outcome, adminId);
        if (!FIELD_REPORT.equals(task.getField())) {
            notifyAuthor(task, outcome == ModerationStatus.APPROVED);
        }
        return taskMapper.toResponse(task);
    }

    private void approveField(RoommateModerationTask task) {
        if (task.getSubjectType() == ModerationSubjectType.PROFILE) {
            RoommateProfile profile = requireProfile(task.getSubjectId());
            profile.setBioApproved(task.getSubmittedText());
            profile.setBioModStatus(ModerationStatus.APPROVED);
            profileRepository.save(profile);
        } else {
            RoommateListing listing = requireListing(task.getSubjectId());
            listing.setDescriptionApproved(task.getSubmittedText());
            listing.setDescriptionModStatus(ModerationStatus.APPROVED);
            listingRepository.save(listing);
        }
    }

    private void rejectField(RoommateModerationTask task) {
        if (task.getSubjectType() == ModerationSubjectType.PROFILE) {
            RoommateProfile profile = requireProfile(task.getSubjectId());
            profile.setBioModStatus(profile.getBioApproved() != null ? ModerationStatus.APPROVED : ModerationStatus.REJECTED);
            profileRepository.save(profile);
        } else {
            RoommateListing listing = requireListing(task.getSubjectId());
            listing.setDescriptionModStatus(listing.getDescriptionApproved() != null ? ModerationStatus.APPROVED : ModerationStatus.REJECTED);
            listingRepository.save(listing);
        }
    }

    private void blockSubject(ModerationSubjectType subjectType, Long subjectId) {
        if (subjectType == ModerationSubjectType.PROFILE) {
            RoommateProfile profile = requireProfile(subjectId);
            profile.setActive(false);
            profile.setSearchable(false);
            profileRepository.save(profile);
        } else {
            RoommateListing listing = requireListing(subjectId);
            listing.setStatus(RoommateListingStatus.ARCHIVED);
            listingRepository.save(listing);
        }
    }

    private void restoreSubject(ModerationSubjectType subjectType, Long subjectId) {
        if (subjectType == ModerationSubjectType.PROFILE) {
            RoommateProfile profile = requireProfile(subjectId);
            profile.setSearchable(true);
            profile.setReportsCount(0);
            profileRepository.save(profile);
        } else {
            RoommateListing listing = requireListing(subjectId);
            listing.setStatus(RoommateListingStatus.PAUSED);
            listing.setReportsCount(0);
            listingRepository.save(listing);
        }
    }

    private void notifyAuthor(RoommateModerationTask task, boolean approved) {
        Long authorId = task.getSubjectType() == ModerationSubjectType.PROFILE
            ? task.getSubjectId()
            : requireListing(task.getSubjectId()).getAuthorUserId();
        NotificationType type = approved ? NotificationType.ROOMMATE_PROFILE_APPROVED : NotificationType.ROOMMATE_PROFILE_REJECTED;
        String message = approved
            ? "Ваш текст в разделе «Соседи» прошёл модерацию и опубликован."
            : "Текст в разделе «Соседи» не прошёл модерацию" + (task.getDecisionReason() == null ? "." : ": " + task.getDecisionReason());
        notificationPublisher.publish(authorId, type, message, "roommate-moderation:" + task.getId());
    }

    private RoommateProfile requireProfile(Long userId) {
        return profileRepository.findById(userId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private RoommateListing requireListing(Long listingId) {
        return listingRepository.findById(listingId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_LISTING_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
