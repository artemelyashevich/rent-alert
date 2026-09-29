package com.rentalert.dto.listing;

import com.rentalert.dto.AbstractDto;
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
@Schema(description = "Настройки планировщика")
public class SchedulerSettingsResponse extends AbstractDto {

    @Schema(description = "Идентификатор записи", example = "1")
    private Long id;

    @Schema(description = "Планировщик включён", example = "true")
    private Boolean schedulerEnabled;

    @Schema(description = "Название провайдера", example = "kufar")
    private ScheduleProvider providerName;

    @Schema(description = "Уведомления включены", example = "false")
    private Boolean notificationsEnabled;

    @Schema(description = "Интервал опроса в миллисекундах", example = "60000")
    private Long fixedRateMs;

    @Schema(description = "Размер страницы при запросе объявлений", example = "50")
    private Integer pageSize;

    @Schema(description = "Минимальная цена фильтра", example = "100")
    private Integer priceFrom;

    @Schema(description = "Максимальная цена фильтра", example = "5000")
    private Integer priceTo;

    @Schema(description = "Валюта цены", example = "BYN")
    private String priceCurrency;

    @Schema(description = "Идентификатор региона", example = "0")
    private String regionId;

    @Schema(description = "Поле сортировки", example = "lst.d")
    private String sortBy;

    @Schema(description = "Направление сортировки", example = "DESC")
    private String sortOrder;

    @Schema(description = "Идентификатор категории квартир", example = "2")
    private Integer categoryId;

    @Schema(description = "Идентификатор категории комнат", example = "3")
    private Integer roomCategoryId;

    @Schema(description = "Дата и время создания записи")
    private Instant createdAt;

    @Schema(description = "Дата и время последнего обновления")
    private Instant updatedAt;
}