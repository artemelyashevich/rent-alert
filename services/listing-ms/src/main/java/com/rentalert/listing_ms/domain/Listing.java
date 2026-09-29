package com.rentalert.listing_ms.domain;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@Entity
@Table(
    name = "listings",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_listings_source_source_id",
        columnNames = {"source", "source_id"}
    )
)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Listing implements Persistable<Long> {

    /** Id assigned by processing-ms, the same in every service. */
    @Id
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.rentalert.dto.listing.ScheduleProvider source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ListingCategory category = ListingCategory.FLAT;

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(length = 1000)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String street;

    @Column(name = "metro_station", length = 255)
    private String metroStation;

    @Column(length = 255)
    private String city;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column
    private Integer rooms;

    @Column(name = "area_total")
    private Float areaTotal;

    /** Площадь конкретной комнаты — заполняется только для {@link ListingCategory#ROOM}. */
    @Column(name = "room_area")
    private Float roomArea;

    @Column(name = "geo_lat")
    private Double geoLat;

    @Column(name = "geo_lng")
    private Double geoLng;

    @Column
    private Integer floor;

    @Column(name = "floors_total")
    private Integer floorsTotal;

    @Column(nullable = false)
    @Builder.Default
    private boolean furniture = false;

    @ToString.Exclude
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 50)
    @Column(columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private String[] photos = new String[0];

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "parsed_at", nullable = false)
    @Builder.Default
    private Instant parsedAt = Instant.now();

    @Column(name = "updated_at_provider")
    private Instant updatedAtProvider;

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
