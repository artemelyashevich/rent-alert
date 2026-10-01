package com.rentalert.identity_ms.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.profile.BotStatus;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.UserProfileService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotStatusServiceImplTests {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private BotStatusServiceImpl service;

    private static UserProfile profile(BotStatus botStatus) {
        UserProfile profile = UserProfile.builder().build();
        profile.setId(10L);
        profile.getSettings().setBotStatus(botStatus);
        return profile;
    }

    @Test
    void marksBotBlocked() {
        UserProfile profile = profile(BotStatus.ACTIVE);
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(profile));

        service.markBotBlocked(10L);

        assertThat(profile.getSettings().getBotStatus()).isEqualTo(BotStatus.BLOCKED);
        verify(userProfileService).save(profile);
    }

    @Test
    void alreadyBlockedIsLeftAlone() {
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(profile(BotStatus.BLOCKED)));

        service.markBotBlocked(10L);

        verify(userProfileService, never()).save(any());
    }

    @Test
    void marksBotUnblocked() {
        UserProfile profile = profile(BotStatus.BLOCKED);
        when(userProfileRepository.findById(10L)).thenReturn(Optional.of(profile));

        service.markBotUnblocked(10L);

        assertThat(profile.getSettings().getBotStatus()).isEqualTo(BotStatus.ACTIVE);
        verify(userProfileService).save(profile);
    }

    @Test
    void unknownUserIsIgnored() {
        when(userProfileRepository.findById(10L)).thenReturn(Optional.empty());

        service.markBotBlocked(10L);

        verifyNoInteractions(userProfileService);
    }
}
