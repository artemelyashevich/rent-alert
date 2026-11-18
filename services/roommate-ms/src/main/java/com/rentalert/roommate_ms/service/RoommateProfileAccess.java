package com.rentalert.roommate_ms.service;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateProfileAccess {

    private final RoommateProfileRepository profileRepository;

    public RoommateProfile requireExists(Long userId) {
        return profileRepository.findById(userId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    public RoommateProfile requireReady(Long userId) {
        RoommateProfile profile = requireExists(userId);
        ModerationStatus status = profile.getBioModStatus();
        boolean approved = status == ModerationStatus.APPROVED_AUTO || status == ModerationStatus.APPROVED;
        if (!profile.isActive() || !approved) {
            throw new AppException(InternalExceptionCode.EX409_ROOMMATE_PROFILE_NOT_READY, HttpStatus.CONFLICT);
        }
        return profile;
    }
}
