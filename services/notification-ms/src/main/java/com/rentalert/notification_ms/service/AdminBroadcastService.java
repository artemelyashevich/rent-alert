package com.rentalert.notification_ms.service;

import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminBroadcastService {

    BroadcastResponse broadcast(String message, List<Long> userIds, boolean pin, Long adminUserId);

    Page<BroadcastSummaryResponse> findAll(Pageable pageable);

    BroadcastSummaryResponse findById(Long broadcastId);

    Page<BroadcastRecipientResponse> findRecipients(Long broadcastId, Long userId, Pageable pageable);

    void delete(Long broadcastId);
}
