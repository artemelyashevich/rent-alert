package com.rentalert.admin_ms.client;

import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserBriefs {

    private final IdentityMsService identityMsService;

    public Map<Long, UserBriefResponse> byId(Collection<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return identityMsService.findBriefs(ids).stream()
            .collect(Collectors.toMap(UserBriefResponse::userId, Function.identity()));
    }
}
