package com.rentalert.core_ms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealtDto {

    private DataWrapper data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataWrapper {

        private SearchObjects searchObjects;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SearchObjects {

        private Body body;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {

        private List<ObjectDto> results;

        private Pagination pagination;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ObjectDto {

        private String uuid;

        private Integer code;

        private String headline;

        private String description;

        private BigDecimal price;

        private Integer priceCurrency;

        private Integer rooms;

        private BigDecimal areaTotal;

        private BigDecimal areaLiving;

        private Integer storey;

        private Integer storeys;

        private String address;

        private String streetName;

        private String metroStationName;

        private Integer metroTime;

        private List<String> contactPhones;

        private List<String> images;

        private String contactName;

        private String createdAt;

        private String updatedAt;

        private Integer furniture;

        private Integer repairState;

        private List<Integer> appliances;

        private List<BigDecimal> location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Pagination {

        private Integer page;

        private Integer pageSize;

        private Integer totalCount;
    }
}