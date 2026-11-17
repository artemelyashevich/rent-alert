package com.rentalert.roommate_ms.repository;

import com.rentalert.roommate_ms.domain.RoommateCalendarEventEmbeddedId;
import com.rentalert.roommate_ms.domain.RoommateCalendarEventView;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoommateCalendarEventViewRepository
    extends JpaRepository<RoommateCalendarEventView, RoommateCalendarEventEmbeddedId> {

    List<RoommateCalendarEventView> findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(
        Long userId, Instant from, Instant to);
}
