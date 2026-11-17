package com.rentalert.roommate_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.currency.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roommate_viewings")
public class RoommateViewing extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private RoommateMatch match;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "address", length = 512)
    private String address;

    @Column(name = "viewing_date", nullable = false)
    private Instant viewingDate;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private RoommateViewingStatus status = RoommateViewingStatus.SCHEDULED;

    @Column(name = "listing_title", length = 1000)
    private String listingTitle;

    @Column(name = "listing_street", length = 255)
    private String listingStreet;

    @Column(name = "listing_metro_station", length = 255)
    private String listingMetroStation;

    @Column(name = "listing_price", precision = 12, scale = 2)
    private BigDecimal listingPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_currency", length = 10)
    private Currency listingCurrency;

    @Column(name = "listing_cover_photo_url", length = 2048)
    private String listingCoverPhotoUrl;
}
