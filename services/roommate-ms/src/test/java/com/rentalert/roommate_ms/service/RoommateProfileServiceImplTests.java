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
import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.Gender;
import com.rentalert.roommate_ms.domain.RoommateProfile;
import com.rentalert.roommate_ms.dto.RoommateProfileRequest;
import com.rentalert.roommate_ms.mapper.RoommateProfileMapper;
import com.rentalert.roommate_ms.repository.RoommateProfileRepository;
import com.rentalert.roommate_ms.service.impl.RoommateProfileServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class RoommateProfileServiceImplTests {

    private final RoommateProfileRepository profileRepository = mock(RoommateProfileRepository.class);
    private final RoommateModerationGate moderationGate = mock(RoommateModerationGate.class);
    private final RoommateProfileService service = new RoommateProfileServiceImpl(profileRepository,
        Mappers.getMapper(RoommateProfileMapper.class), moderationGate);

    private final RoommateProfileRequest request = RoommateProfileRequest.builder()
        .gender(Gender.FEMALE)
        .city("Минск")
        .bio("Люблю тишину")
        .build();

    @BeforeEach
    void saveReturnsEntity() {
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newProfileWithApprovedBioIsPublished() {
        when(profileRepository.findById(5L)).thenReturn(Optional.empty());
        when(moderationGate.moderate(ModerationSubjectType.PROFILE, 5L, "bio", "Люблю тишину")).thenReturn(ModerationStatus.APPROVED_AUTO);

        assertThat(service.upsert(5L, request))
            .satisfies(response -> {
                assertThat(response.userId()).isEqualTo(5L);
                assertThat(response.bioApproved()).isEqualTo("Люблю тишину");
                assertThat(response.genderPref()).containsExactly(Gender.MALE, Gender.FEMALE);
            });
    }

    @Test
    void pendingBioIsNotPublished() {
        when(profileRepository.findById(5L)).thenReturn(Optional.empty());
        when(moderationGate.moderate(any(), any(), anyString(), any())).thenReturn(ModerationStatus.PENDING_MANUAL);

        assertThat(service.upsert(5L, request))
            .satisfies(response -> {
                assertThat(response.bioApproved()).isNull();
                assertThat(response.bioModStatus()).isEqualTo(ModerationStatus.PENDING_MANUAL);
            });
    }

    @Test
    void unchangedBioSkipsModerationAndBumpsVersion() {
        RoommateProfile existing = RoommateProfile.builder().userId(5L).bioSubmitted("Люблю тишину").profileVersion(3).build();
        when(profileRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.upsert(5L, request);

        assertThat(existing.getProfileVersion()).isEqualTo(4);
        verify(moderationGate, never()).moderate(any(), any(), anyString(), any());
    }

    @Test
    void deactivationHidesProfile() {
        RoommateProfile existing = RoommateProfile.builder().userId(5L).build();
        when(profileRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.deactivate(5L);

        assertThat(existing.isActive()).isFalse();
        assertThat(existing.isSearchable()).isFalse();
    }

    @Test
    void missingProfileIsNotFound() {
        when(profileRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setSearchable(5L, true))
            .isInstanceOfSatisfying(AppException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(InternalExceptionCode.EX404_ROOMMATE_PROFILE_NOT_FOUND));
    }
}
