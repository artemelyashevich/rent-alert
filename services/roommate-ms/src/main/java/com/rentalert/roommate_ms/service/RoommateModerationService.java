package com.rentalert.roommate_ms.service;

import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoommateModerationService {

    /**
     * Tasks waiting for manual moderation, oldest first.
     */
    Page<RoommateModerationTaskResponse> queue(Pageable pageable);

    /**
     * Approves a pending task: publishes the submitted text, or for a complaint blocks the reported profile or
     * listing. The author of a text is notified about the decision.
     */
    RoommateModerationTaskResponse approve(Long taskId, Long adminId, String reason);

    /**
     * Rejects a pending task: the previously approved text stays visible, or for a complaint the subject is restored
     * and its report counter reset. The author of a text is notified about the decision.
     */
    RoommateModerationTaskResponse reject(Long taskId, Long adminId, String reason);
}
