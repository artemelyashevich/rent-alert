package com.rentalert.dto.profile.filter;

import com.rentalert.dto.AbstractDto;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.listing.ListingCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
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
@Schema(description = "Фильтр пользователя")
public class UserFilterResponse extends AbstractDto {

    @Schema(description = "Идентификатор фильтра", example = "1")
    private Long id;

    @Schema(description = "Категория объявлений (FLAT/ROOM), null — обе категории")
    private ListingCategory category;

    @Schema(description = "Минимальная цена", example = "200")
    private Integer priceFrom;

    @Schema(description = "Максимальная цена", example = "800")
    private Integer priceTo;

    @Schema(description = "Минимальное количество комнат", example = "1")
    private Integer roomsFrom;

    @Schema(description = "Максимальное количество комнат", example = "3")
    private Integer roomsTo;

    @Schema(description = "Минимальная площадь", example = "30.0")
    private Float areaFrom;

    @Schema(description = "Максимальная площадь", example = "80.0")
    private Float areaTo;

    @Schema(description = "Фильтр активен", example = "true")
    private boolean active;

    @Schema(description = "Город фильтра", example = "Минск")
    private String city;

    @Schema(description = "Валюта, к которой привязан фильтр (бел рубль = BYN)", example = "USD")
    private Currency currency;

    @Schema(description = "Дата и время создания")
    private Instant createdAt;

    @Schema(description = "Дата и время последнего обновления")
    private Instant updatedAt;
}