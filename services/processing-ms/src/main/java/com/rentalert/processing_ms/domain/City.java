package com.rentalert.processing_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Entity
@Table(
        name = "cities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cities_provider_city_code",
                columnNames = {"provider", "city_code"}
        )
)
@NoArgsConstructor
@AllArgsConstructor
public class City extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.rentalert.dto.listing.ScheduleProvider provider;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "city_code", nullable = false, length = 255)
    private String cityCode;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
