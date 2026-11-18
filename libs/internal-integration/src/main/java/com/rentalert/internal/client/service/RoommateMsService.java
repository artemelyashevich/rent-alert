package com.rentalert.internal.client.service;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.roommate.RoommateMatchParticipantsDto;
import com.rentalert.dto.roommate.RoommateModerationDecisionRequest;
import com.rentalert.dto.roommate.RoommateModerationTaskResponse;
import com.rentalert.dto.roommate.RoommateViewingDto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface RoommateMsService {

    PageResponse<RoommateModerationTaskResponse> findModerationQueue(Pageable pageable);

    RoommateModerationTaskResponse approveModeration(Long taskId, RoommateModerationDecisionRequest request);

    RoommateModerationTaskResponse rejectModeration(Long taskId, RoommateModerationDecisionRequest request);

    Optional<RoommateViewingDto> getViewing(Long viewingId);

    List<RoommateMatchParticipantsDto> findMatchParticipants(List<Long> matchIds);
}
