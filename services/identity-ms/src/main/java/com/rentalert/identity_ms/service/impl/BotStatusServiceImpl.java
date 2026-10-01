package com.rentalert.identity_ms.service.impl;

import com.rentalert.dto.profile.BotStatus;
import com.rentalert.identity_ms.domain.UserProfile;
import com.rentalert.identity_ms.repository.UserProfileRepository;
import com.rentalert.identity_ms.service.BotStatusService;
import com.rentalert.identity_ms.service.UserProfileService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotStatusServiceImpl implements BotStatusService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    @Override
    @Transactional
    public void markBotBlocked(Long userId) {
        setBotStatus(userId, BotStatus.BLOCKED);
    }

    @Override
    @Transactional
    public void markBotUnblocked(Long userId) {
        setBotStatus(userId, BotStatus.ACTIVE);
    }

    private void setBotStatus(Long userId, BotStatus botStatus) {
        Optional<UserProfile> found = userProfileRepository.findById(userId);
        if (found.isEmpty()) {
            log.warn("Bot status {} for unknown user {}, ignoring", botStatus, userId);
            return;
        }
        UserProfile profile = found.get();
        if (profile.getSettings().getBotStatus() == botStatus) {
            return;
        }
        profile.getSettings().setBotStatus(botStatus);
        userProfileService.save(profile);
        log.info("Bot status of user {} changed to {}", userId, botStatus);
    }
}
