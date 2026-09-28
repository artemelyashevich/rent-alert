package com.rentalert.identity_ms.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@Entity
@Table(
    name = "user_filters",
    indexes = @Index(name = "idx_user_filters_profile_id", columnList = "user_profile_id")
)
@NoArgsConstructor
@ToString
@AllArgsConstructor
public class UserFilter extends AbstractEntity {

    @ToString.Exclude
    @JsonBackReference("profile-filters")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id")
    private UserProfile userProfile;

    @Column(name = "owner_match_id")
    private Long ownerMatchId;

    /** NULL — фильтр действует для обеих категорий (квартиры и комнаты). */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ListingCategory category;

    @Column(name = "price_from")
    private Integer priceFrom;

    @Column(name = "price_to")
    private Integer priceTo;

    @Column(name = "rooms_from")
    private Integer roomsFrom;

    @Column(name = "rooms_to")
    private Integer roomsTo;

    @Column(name = "area_from")
    private Float areaFrom;

    @Column(name = "area_to")
    private Float areaTo;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(nullable = false, length = 255)
    @Builder.Default
    private String city = "Минск";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;
}