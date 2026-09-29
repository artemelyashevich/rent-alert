package com.rentalert.listing_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "listing_status")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ListingStatus extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private ListingStatusName name;

    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "shows_in_calendar", nullable = false)
    @Builder.Default
    private boolean showsInCalendar = false;
}