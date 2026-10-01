package com.rentalert.internal.client.service.impl;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import com.rentalert.internal.client.AbstractResilientClient;
import com.rentalert.internal.client.NotificationMsClient;
import com.rentalert.internal.client.service.NotificationMsService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.data.domain.Pageable;

public class NotificationMsServiceImpl extends AbstractResilientClient implements NotificationMsService {

    private final NotificationMsClient notificationMsClient;

    public NotificationMsServiceImpl(NotificationMsClient notificationMsClient, RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        super(NotificationMsClient.SERVICE_NAME, retryRegistry, circuitBreakerRegistry);
        this.notificationMsClient = notificationMsClient;
    }

    @Override
    public BroadcastResponse broadcast(BroadcastRequest request, Long createdBy) {
        return executeOnce(() -> notificationMsClient.broadcast(request, createdBy));
    }

    @Override
    public PageResponse<BroadcastSummaryResponse> findBroadcasts(Pageable pageable) {
        return execute(() -> notificationMsClient.findBroadcasts(pageable.getPageNumber(), pageable.getPageSize(),
            PageUtil.toSortParams(pageable)));
    }

    @Override
    public BroadcastSummaryResponse findBroadcast(Long broadcastId) {
        return execute(() -> notificationMsClient.findBroadcast(broadcastId));
    }

    @Override
    public PageResponse<BroadcastRecipientResponse> findRecipients(Long broadcastId, Long userId, Pageable pageable) {
        return execute(() -> notificationMsClient.findRecipients(broadcastId, userId, pageable.getPageNumber(),
            pageable.getPageSize(), PageUtil.toSortParams(pageable)));
    }

    @Override
    public void deleteBroadcast(Long broadcastId) {
        executeOnce(() -> notificationMsClient.deleteBroadcast(broadcastId));
    }
}
