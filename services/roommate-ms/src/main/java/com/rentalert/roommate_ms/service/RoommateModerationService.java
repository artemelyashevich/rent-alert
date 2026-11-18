package com.rentalert.roommate_ms.service;

import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoommateModerationService {

    Page<RoommateModerationTaskResponse> queue(Pageable pageable);

    RoommateModerationTaskResponse approve(Long taskId, Long adminId, String reason);

    RoommateModerationTaskResponse reject(Long taskId, Long adminId, String reason);
}
