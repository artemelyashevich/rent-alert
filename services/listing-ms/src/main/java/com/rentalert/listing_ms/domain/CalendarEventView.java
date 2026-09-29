package com.rentalert.listing_ms.domain;

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

@Setter
@Getter
@Entity
@Immutable
@Table(name = "v_calendar_events")
@NoArgsConstructor
public class CalendarEventView {

    @EmbeddedId
    private FavoriteEmbeddedId id;

    @Column(name = "title", length = 1000)
    private String title;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "metro_station", length = 255)
    private String metroStation;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency")
    private Currency currency;

    @Column(name = "cover_photo_url", length = 2048)
    private String coverPhotoUrl;

    @Column(name = "viewing_date")
    private Instant viewingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private ListingStatusName status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}