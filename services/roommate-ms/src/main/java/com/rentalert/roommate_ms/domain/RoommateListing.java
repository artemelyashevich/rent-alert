package com.rentalert.roommate_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.roommate.ModerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roommate_listings")
public class RoommateListing extends AbstractEntity {

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "address_exact", length = 512)
    private String addressExact;

    @Column(name = "address_public", length = 512)
    private String addressPublic;

    @Column(name = "district")
    private String district;

    @Column(name = "metro_station")
    private String metroStation;

    @Column(name = "geo_lat")
    private Double geoLat;

    @Column(name = "geo_lng")
    private Double geoLng;

    @Column(name = "rent_share", precision = 12, scale = 2)
    private BigDecimal rentShare;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 8)
    private Currency currency;

    @Column(name = "room_count")
    private Integer roomCount;

    @Column(name = "flatmates_count")
    private Integer flatmatesCount;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 50)
    @Column(name = "house_rules", columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private String[] houseRules = new String[0];

    @Column(name = "description_submitted", columnDefinition = "TEXT")
    private String descriptionSubmitted;

    @Column(name = "description_approved", columnDefinition = "TEXT")
    private String descriptionApproved;

    @Enumerated(EnumType.STRING)
    @Column(name = "description_mod_status", nullable = false, length = 20)
    @Builder.Default
    private ModerationStatus descriptionModStatus = ModerationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private RoommateListingStatus status = RoommateListingStatus.DRAFT;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 4)
    @Column(name = "gender_pref", columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private String[] genderPref = new String[0];

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Column(name = "reports_count", nullable = false)
    @Builder.Default
    private Integer reportsCount = 0;
}
