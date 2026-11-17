package com.rentalert.roommate_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.dto.RoommateProfileResponse;
import com.rentalert.roommate_ms.mapper.RoommateProfileMapper;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.RoommateModerationGate;
import com.rentalert.roommate_ms.service.RoommateProfileService;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoommateProfileServiceImpl implements RoommateProfileService {

    private final RoommateProfileRepository profileRepository;
    private final RoommateProfileMapper profileMapper;
    private final RoommateModerationGate moderationGate;

    @Override
    @Transactional(readOnly = true)
    public Optional<RoommateProfileResponse> findMine(Long userId) {
        return profileRepository.findById(userId).map(profileMapper::toResponse);
    }

    @Override
    @Transactional
    public RoommateProfileResponse upsert(Long userId, RoommateProfileRequest request) {
        Optional<RoommateProfile> existing = profileRepository.findById(userId);
        boolean isNew = existing.isEmpty();
        String previousBio = existing.map(RoommateProfile::getBioSubmitted).orElse(null);

        RoommateProfile profile = existing.orElseGet(() -> RoommateProfile.builder().userId(userId).build());
        profileMapper.update(profile, request);

        if (isNew || !Objects.equals(profile.getBioSubmitted(), previousBio)) {
            ModerationStatus status = moderationGate.moderate(ModerationSubjectType.PROFILE, userId, "bio", profile.getBioSubmitted());
            profile.setBioModStatus(status);
            if (isApproved(status)) {
                profile.setBioApproved(profile.getBioSubmitted());
            }
        }
        if (!isNew) {
            profile.setProfileVersion(profile.getProfileVersion() + 1);
        }

        profile = profileRepository.save(profile);
        log.info("Roommate profile {}. userId={}", isNew ? "created" : "updated", userId);
        return profileMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public RoommateProfileResponse setSearchable(Long userId, boolean searchable) {
        RoommateProfile profile = requireProfile(userId);
        profile.setSearchable(searchable);
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional
    public void deactivate(Long userId) {
        RoommateProfile profile = requireProfile(userId);
        profile.setActive(false);
        profile.setSearchable(false);
        profileRepository.save(profile);
        log.info("Roommate profile deactivated. userId={}", userId);
    }

    private static boolean isApproved(ModerationStatus status) {
        return status == ModerationStatus.APPROVED_AUTO || status == ModerationStatus.APPROVED;
    }

    private RoommateProfile requireProfile(Long userId) {
        return profileRepository.findById(userId)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }
}
