package com.rentalert.listing_ms.repository;

import com.rentalert.listing_ms.domain.CalendarEventView;
import com.rentalert.listing_ms.domain.FavoriteEmbeddedId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface CalendarEventViewRepository extends JpaRepository<CalendarEventView, FavoriteEmbeddedId> {

    List<CalendarEventView> findAllByIdUserIdAndViewingDateBetweenOrderByViewingDateAsc(
            Long userId, Instant from, Instant to);

    List<CalendarEventView> findAllByIdUserIdOrderByViewingDateAsc(Long userId);
}
