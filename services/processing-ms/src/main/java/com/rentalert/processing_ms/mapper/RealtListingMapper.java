package com.rentalert.processing_ms.mapper;

import com.rentalert.dto.currency.Currency;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.RealtDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    imports = {com.rentalert.dto.listing.ScheduleProvider.class}
)
public interface RealtListingMapper {

    @Mapping(target = "source", expression = "java(ScheduleProvider.REALT)")
    @Mapping(target = "sourceId", source = "uuid")
    @Mapping(target = "title", source = "headline")
    @Mapping(target = "street", source = "streetName")
    @Mapping(target = "metroStation", source = "metroStationName")
    @Mapping(target = "price", source = "price", qualifiedByName = "bigDecimalToInteger")
    @Mapping(target = "currency", source = "priceCurrency", qualifiedByName = "toCurrency")
    @Mapping(target = "rooms", source = "rooms")
    @Mapping(target = "areaTotal", source = "areaTotal", qualifiedByName = "bigDecimalToFloat")
    @Mapping(target = "floor", source = "storey")
    @Mapping(target = "floorsTotal", source = "storeys")
    @Mapping(target = "photos", source = "images", qualifiedByName = "toArray")
    @Mapping(target = "furniture", source = "furniture", qualifiedByName = "toBoolean")
    @Mapping(target = "updatedAtProvider", source = "updatedAt", qualifiedByName = "toInstant")
    @Mapping(target = "roomArea", source = "areaLiving", qualifiedByName = "bigDecimalToFloat")
    @Mapping(target = "category", ignore = true)
    Listing toEntity(RealtDto.ObjectDto source);

    @Named("bigDecimalToInteger")
    default Integer bigDecimalToInteger(BigDecimal value) {
        return value == null ? null : value.intValue();
    }

    @Named("bigDecimalToFloat")
    default Float bigDecimalToFloat(BigDecimal value) {
        return value == null ? null : value.floatValue();
    }

    @Named("toArray")
    default String[] toArray(List<String> values) {
        return values == null
            ? new String[0]
            : values.toArray(new String[0]);
    }

    @Named("toBoolean")
    default boolean toBoolean(Integer value) {
        return value != null && value > 0;
    }

    @Named("toCurrency")
    default Currency toCurrency(Integer value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case 840 -> Currency.USD;
            case 933 -> Currency.BYN;
            case 978 -> Currency.EUR;
            default -> null;
        };
    }

    @Named("toInstant")
    default Instant toInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(value);
        } catch (Exception e) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(value.replace(" ", "T"));
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @AfterMapping
    default void fillSourceUrl(
        RealtDto.ObjectDto source,
        @MappingTarget Listing target
    ) {
        if (source.getCode() != null) {
            target.setSourceUrl(
                "https://realt.by/rent-flat-for-long/object/" + source.getCode()
            );
        }
    }
}