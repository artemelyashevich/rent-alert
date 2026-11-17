package com.rentalert.roommate_ms.domain;

import com.rentalert.dto.currency.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@NoArgsConstructor
@Table(name = "v_roommate_calendar_events")
public class RoommateCalendarEventView {

    @EmbeddedId
    private RoommateCalendarEventEmbeddedId id;

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "metro_station", length = 255)
    private String metroStation;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private Currency currency;

    @Column(name = "cover_photo_url", length = 2048)
    private String coverPhotoUrl;

    @Column(name = "viewing_date")
    private Instant viewingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16)
    private RoommateViewingStatus status;

    @Column(name = "counterparty_user_id")
    private Long counterpartyUserId;
}
