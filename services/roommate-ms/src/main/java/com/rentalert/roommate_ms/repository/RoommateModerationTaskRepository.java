package com.rentalert.roommate_ms.repository;

import com.rentalert.dto.roommate.ModerationStatus;
import com.rentalert.dto.roommate.ModerationSubjectType;
import com.rentalert.roommate_ms.domain.RoommateModerationTask;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateModerationTaskRepository extends JpaRepository<RoommateModerationTask, Long> {

    Page<RoommateModerationTask> findAllByStatusOrderByCreatedAtAsc(ModerationStatus status, Pageable pageable);

    Optional<RoommateModerationTask> findFirstBySubjectTypeAndSubjectIdAndFieldAndStatus(
        ModerationSubjectType subjectType, Long subjectId, String field, ModerationStatus status);
}
