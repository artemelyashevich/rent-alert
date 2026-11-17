package com.rentalert.roommate_ms.service.impl;

import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.roommate_ms.service.RoommateMatchAccess;
import com.rentalert.roommate_ms.service.RoommateSharedFilterService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoommateSharedFilterServiceImpl implements RoommateSharedFilterService {

    private final IdentityMsService identityMsService;
    private final RoommateMatchAccess matchAccess;

    @Override
    @Transactional(readOnly = true)
    public Optional<UserFilterResponse> find(Long matchId, Long requesterUserId) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        return identityMsService.getMatchFilter(matchId);
    }

    @Override
    @Transactional
    public UserFilterResponse upsert(Long matchId, Long requesterUserId, UserFilterRequest request) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        return identityMsService.upsertMatchFilter(matchId, request);
    }

    @Override
    @Transactional
    public UserFilterResponse setActivation(Long matchId, Long requesterUserId, boolean active) {
        matchAccess.requireUnlocked(matchId, requesterUserId);
        return identityMsService.setMatchFilterActivation(matchId, active);
    }
}
