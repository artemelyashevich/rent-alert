package com.rentalert.internal.client.service.impl;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.dto.roommate.RoommateViewingDto;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.RoommateMsClient;
import com.rentalert.internal.client.service.RoommateMsService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public class RoommateMsServiceImpl extends AbstractResilientClient implements RoommateMsService {

    private final RoommateMsClient roommateMsClient;

    public RoommateMsServiceImpl(RoommateMsClient roommateMsClient, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        super(RoommateMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.roommateMsClient = roommateMsClient;
    }

    @Override
    public PageResponse<RoommateModerationTaskResponse> findModerationQueue(Pageable pageable) {
        return execute(() -> roommateMsClient.findModerationQueue(pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public RoommateModerationTaskResponse approveModeration(Long taskId, RoommateModerationDecisionRequest request) {
        return executeOnce(() -> roommateMsClient.approveModeration(taskId, request));
    }

    @Override
    public RoommateModerationTaskResponse rejectModeration(Long taskId, RoommateModerationDecisionRequest request) {
        return executeOnce(() -> roommateMsClient.rejectModeration(taskId, request));
    }

    @Override
    public Optional<RoommateViewingDto> getViewing(Long viewingId) {
        return Optional.ofNullable(execute(() -> roommateMsClient.getViewing(viewingId)));
    }

    @Override
    public List<RoommateMatchParticipantsDto> findMatchParticipants(List<Long> matchIds) {
        return Objects.requireNonNullElse(execute(() -> roommateMsClient.findMatchParticipants(matchIds)), List.of());
    }
}
