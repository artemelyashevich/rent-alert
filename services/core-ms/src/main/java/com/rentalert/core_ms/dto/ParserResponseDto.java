package com.rentalert.core_ms.dto;

import java.math.BigDecimal;
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
public class ParserResponseDto {

    private com.rentalert.dto.listing.ScheduleProvider source;

    private String title;

    private String address;

    private Integer rooms;

    private BigDecimal area;

    private Integer floor;

    private Integer totalFloors;

    private BigDecimal priceUsd;

    private BigDecimal priceByn;

    private String currency;

    private String houseType;

    private Boolean furnished;

    private String contactName;

    private String metro;

    private String description;

    private String link;

    private List<String> images;
}