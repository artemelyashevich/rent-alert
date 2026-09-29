package com.rentalert.core_ms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.ToString;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KufarAdsResponseDto {

    private List<AdDto> ads;

    private Integer total;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @ToString
    public static class AdDto {

        @JsonProperty("ad_id")
        private Long adId;

        @JsonProperty("ad_link")
        private String adLink;

        private String subject;

        @JsonProperty("body_short")
        private String bodyShort;

        @JsonProperty("price_byn")
        private String priceByn;

        @JsonProperty("price_usd")
        private String priceUsd;

        private String currency;

        private List<ImageDto> images;

        @JsonProperty("ad_parameters")
        private List<ParameterDto> adParameters;

        @JsonProperty("account_parameters")
        private List<ParameterDto> accountParameters;

        @JsonProperty("phone_hidden")
        private Boolean phoneHidden;

        @JsonProperty("show_parameters")
        private ShowParametersDto showParameters;

        @JsonProperty("list_time")
        private String listTime;

        public String getAddress() {
            return findParameterValue(accountParameters, "address");
        }

        public String getPhone() {
            return findParameterValue(accountParameters, "phone");
        }

        private String findParameterValue(
                List<ParameterDto> parameters,
                String parameterName
        ) {
            if (parameters == null) {
                return null;
            }

            return parameters.stream()
                    .filter(parameter -> parameterName.equals(parameter.getP()))
                    .map(ParameterDto::getV)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .findFirst()
                    .orElse(null);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageDto {
        private String path;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParameterDto {

        private String pl;
        private Object vl;
        private String p;
        private Object v;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ShowParametersDto {

        @JsonProperty("show_call")
        private Boolean showCall;

        @JsonProperty("show_chat")
        private Boolean showChat;

        @JsonProperty("show_import_link")
        private Boolean showImportLink;

        @JsonProperty("show_web_shop_link")
        private Boolean showWebShopLink;
    }
}
