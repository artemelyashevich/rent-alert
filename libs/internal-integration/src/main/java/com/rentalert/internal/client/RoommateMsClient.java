package com.rentalert.internal.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.dto.roommate.RoommateViewingDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = RoommateMsClient.SERVICE_NAME, path = "/roommate/internal")
public interface RoommateMsClient {

    String SERVICE_NAME = "roommate-ms";

    @GetMapping("/admin/moderation/queue")
    PageResponse<RoommateModerationTaskResponse> findModerationQueue(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );

    @PostMapping("/admin/moderation/{taskId}/approve")
    RoommateModerationTaskResponse approveModeration(@PathVariable("taskId") Long taskId,
        @RequestBody RoommateModerationDecisionRequest request);

    @PostMapping("/admin/moderation/{taskId}/reject")
    RoommateModerationTaskResponse rejectModeration(@PathVariable("taskId") Long taskId,
        @RequestBody RoommateModerationDecisionRequest request);

    @GetMapping("/viewings/{viewingId}")
    RoommateViewingDto getViewing(@PathVariable("viewingId") Long viewingId);
}
