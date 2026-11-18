package com.rentalert.internal.client.service;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NotificationMsService {

    BroadcastResponse broadcast(BroadcastRequest request, Long createdBy);

    PageResponse<BroadcastSummaryResponse> findBroadcasts(Pageable pageable);

    BroadcastSummaryResponse findBroadcast(Long broadcastId);

    PageResponse<BroadcastRecipientResponse> findRecipients(Long broadcastId, List<Long> userIds, Pageable pageable);

    void deleteBroadcast(Long broadcastId);
}
