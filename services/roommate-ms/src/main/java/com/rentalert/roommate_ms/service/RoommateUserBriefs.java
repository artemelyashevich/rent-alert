package com.rentalert.roommate_ms.service;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import com.rentalert.redis.dto.CacheType;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoommateUserBriefs {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final IdentityMsService identityMsService;
    private final RoommateBatchCache batchCache;

    public Map<Long, UserBriefResponse> find(Collection<Long> userIds) {
        return batchCache.getAll(CacheType.USER_BRIEF, userIds, TTL, missing -> identityMsService.findUserBriefs(missing).stream()
            .collect(Collectors.toMap(UserBriefResponse::userId, Function.identity(), (a, b) -> a)));
    }
}
