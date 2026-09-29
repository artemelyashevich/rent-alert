package com.rentalert.dto.listing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ListingDto {

    private Long id;

    private ScheduleProvider source;

    private ListingCategory category;

    private String sourceUrl;

    private String title;

    private String description;

    private String street;

    private String metroStation;

    private BigDecimal price;

    private Currency currency;

    private Integer rooms;

    private Float areaTotal;

    private Float roomArea;

    private Double geoLat;

    private Double geoLng;

    private Integer floor;

    private Integer floorsTotal;

    private boolean furniture;

    private String previewPhoto;

    private List<String> photos;

    private Instant parsedAt;

    private Instant updatedAtProvider;
}