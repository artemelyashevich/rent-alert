package com.rentalert.processing_ms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KufarParams {

    @JsonProperty("cur")
    private String currency;

    @JsonProperty("prc")
    private String price;

    @JsonProperty("cat")
    private String category;

    @JsonProperty("size")
    private Integer size;

    @JsonProperty("sort")
    private String sort;
}
