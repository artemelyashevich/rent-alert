package com.rentalert.admin_ms.client;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import feign.RetryableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    public List<Long> searchUserIds(String username) {
        try {
            return identityMsService.searchUserBriefs(username).stream().map(UserBriefResponse::userId).toList();
        } catch (UpstreamHttpException | RetryableException | CallNotPermittedException e) {
            throw new AppException("Сервис пользователей недоступен, поиск по имени временно невозможен", e,
                HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
