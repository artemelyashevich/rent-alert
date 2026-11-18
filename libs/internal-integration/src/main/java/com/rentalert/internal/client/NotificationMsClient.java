package com.rentalert.internal.client;

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

@FeignClient(name = NotificationMsClient.SERVICE_NAME, path = "/notification/internal")
public interface NotificationMsClient {

    String SERVICE_NAME = "notification-ms";

    @PostMapping("/admin/broadcasts")
    BroadcastResponse broadcast(@RequestBody BroadcastRequest request, @RequestParam("createdBy") Long createdBy);

    @GetMapping("/admin/broadcasts")
    PageResponse<BroadcastSummaryResponse> findBroadcasts(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @GetMapping("/admin/broadcasts/{broadcastId}")
    BroadcastSummaryResponse findBroadcast(@PathVariable("broadcastId") Long broadcastId);

    @GetMapping("/admin/broadcasts/{broadcastId}/recipients")
    PageResponse<BroadcastRecipientResponse> findRecipients(
        @PathVariable("broadcastId") Long broadcastId,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @PostMapping("/admin/broadcasts/{broadcastId}/recipients")
    PageResponse<BroadcastRecipientResponse> findRecipientsByUserIds(
        @PathVariable("broadcastId") Long broadcastId,
        @RequestBody List<Long> userIds,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @DeleteMapping("/admin/broadcasts/{broadcastId}")
    void deleteBroadcast(@PathVariable("broadcastId") Long broadcastId);
}
