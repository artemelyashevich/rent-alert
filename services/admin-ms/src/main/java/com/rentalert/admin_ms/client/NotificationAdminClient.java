package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRequest;
import com.rentalert.dto.notification.BroadcastResponse;
import com.rentalert.dto.notification.BroadcastSummaryResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-ms", contextId = "notificationAdminClient", path = "/notification/internal/admin")
public interface NotificationAdminClient {

    @PostMapping("/broadcasts")
    BroadcastResponse broadcast(@RequestBody BroadcastRequest request, @RequestParam("createdBy") Long createdBy);

    @GetMapping("/broadcasts")
    PageResponse<BroadcastSummaryResponse> findBroadcasts(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/broadcasts/{broadcastId}")
    BroadcastSummaryResponse findBroadcast(@PathVariable("broadcastId") Long broadcastId);

    @GetMapping("/broadcasts/{broadcastId}/recipients")
    PageResponse<BroadcastRecipientResponse> findRecipients(
        @PathVariable("broadcastId") Long broadcastId,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @DeleteMapping("/broadcasts/{broadcastId}")
    void deleteBroadcast(@PathVariable("broadcastId") Long broadcastId);
}
