package com.rentalert.processing_ms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlinerAdsResponseDto {

    private List<ApartmentDto> apartments;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApartmentDto {
        private Long id;
        private Price price;
        @JsonProperty("rent_type")
        private String rentType;
        private Location location;
        private String photo;
        private Contact contact;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("last_time_up")
        private String lastTimeUp;
        @JsonProperty("up_available_in")
        private Long upAvailableIn;
        private String url;
        @JsonProperty("area_total")
        private BigDecimal areaTotal;
        @JsonProperty("area_living")
        private BigDecimal areaLiving;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Price {
        private BigDecimal amount;
        private String currency;
        private Converted converted;

        public BigDecimal getUsdAmount() {
            if (converted != null && converted.getUsd() != null) {
                return converted.getUsd().getAmount();
            }

            if (Currency.USD.name().equalsIgnoreCase(currency)) {
                return amount;
            }

            return null;
        }

        public BigDecimal getBynAmount() {
            if (converted != null && converted.getByn() != null) {
                return converted.getByn().getAmount();
            }

            if (Currency.BYN.name().equalsIgnoreCase(currency)) {
                return amount;
            }

            return null;
        }
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Converted {
        @JsonProperty("BYN")
        private Amount byn;
        @JsonProperty("USD")
        private Amount usd;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Amount {
        private BigDecimal amount;
        private String currency;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String address;
        @JsonProperty("user_address")
        private String userAddress;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Contact {
        private Boolean owner;
    }
}