package com.rentalert.core_ms.mapper;

import com.rentalert.core_ms.domain.Listing;
import com.rentalert.core_ms.dto.KufarAdsResponseDto;
import com.rentalert.core_ms.dto.KufarAdsResponseDto.ImageDto;
import com.rentalert.dto.GlobalConstants;
import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
public interface KufarListingMapper {

    @Mapping(target = "source", expression = "java(ScheduleProvider.KUFAR)")
    @Mapping(target = "sourceId", expression = "java(String.valueOf(source.getAdId()))")
    @Mapping(target = "sourceUrl", source = "adLink")
    @Mapping(target = "title", source = "subject")
    @Mapping(target = "description", source = "bodyShort")
    @Mapping(target = "updatedAtProvider", source = "listTime", qualifiedByName = "toInstant")
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "photos", source = "images", qualifiedByName = "photos")
    @Mapping(target = "rooms", source = "adParameters", qualifiedByName = "rooms")
    @Mapping(target = "floor", source = "adParameters", qualifiedByName = "floor")
    @Mapping(target = "floorsTotal", source = "adParameters", qualifiedByName = "floorsTotal")
    @Mapping(target = "areaTotal", source = "adParameters", qualifiedByName = "areaTotal")
    @Mapping(target = "furniture", source = "adParameters", qualifiedByName = "furniture")
    @Mapping(target = "street", source = "accountParameters", qualifiedByName = "address")
    @Mapping(target = "metroStation", source = "adParameters", qualifiedByName = "metroStation")
    @Mapping(target = "roomArea", source = "adParameters", qualifiedByName = "roomArea")
    @Mapping(target = "category", ignore = true)
    Listing toEntity(KufarAdsResponseDto.AdDto source);

    @AfterMapping
    default void after(KufarAdsResponseDto.AdDto dto, @MappingTarget Listing listing) {
        if (dto.getPriceUsd() != null) {
            listing.setCurrency(Currency.USD);
            listing.setPrice(getPrice(dto.getPriceUsd()));
        } else {
            listing.setCurrency(Currency.BYN);
            listing.setPrice(getPrice(dto.getPriceByn()));
        }
    }

    @Named("address")
    default String address(List<KufarAdsResponseDto.ParameterDto> params) {
        return getParameterValue(params, "address");
    }

    default String getParameterValue(
            List<KufarAdsResponseDto.ParameterDto> params,
            String code
    ) {
        if (params == null) {
            return null;
        }

        return params.stream()
                .filter(p -> code.equalsIgnoreCase(p.getP()))
                .map(KufarAdsResponseDto.ParameterDto::getV)
                .filter(Objects::nonNull)
                .map(this::extractValue)
                .findFirst()
                .orElse(null);
    }

    default String extractValue(Object value) {
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }

            return list.get(0).toString();
        }

        return value.toString();
    }

    default BigDecimal getPrice(String price) {
        return new BigDecimal(price).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    @Named("photos")
    default String[] photos(List<KufarAdsResponseDto.ImageDto> images) {
        if (images == null) {
            return new String[0];
        }

        List<String> list = new ArrayList<>();
        for (ImageDto imageDto : images) {
            String s = GlobalConstants.KUFAR_IMG_PREFIX + imageDto.getPath();
            list.add(s);
        }
        return list.toArray(new String[0]);
    }

    @Named("rooms")
    default Integer rooms(List<KufarAdsResponseDto.ParameterDto> params) {
        return getIntegerParameter(params, "rooms");
    }

    @Named("floor")
    default Integer floor(List<KufarAdsResponseDto.ParameterDto> params) {
        return getIntegerParameter(params, "floor");
    }

    @Named("floorsTotal")
    default Integer floorsTotal(List<KufarAdsResponseDto.ParameterDto> params) {
        return getIntegerParameter(params, "re_number_floors");
    }

    @Named("areaTotal")
    default Float areaTotal(List<KufarAdsResponseDto.ParameterDto> params) {
        if (params == null) {
            return null;
        }

        Object value = params.stream()
                .filter(p -> "size".equalsIgnoreCase(p.getP()))
                .map(KufarAdsResponseDto.ParameterDto::getV)
                .findFirst()
                .orElse(null);

        return parseFloatSafe(value);
    }

    /** Площадь непосредственно комнаты — заполнено только у объявлений категории "комната". */
    @Named("roomArea")
    default Float roomArea(List<KufarAdsResponseDto.ParameterDto> params) {
        if (params == null) {
            return null;
        }

        Object value = params.stream()
                .filter(p -> "size_living_space".equalsIgnoreCase(p.getP()) ||
                        "room_size".equalsIgnoreCase(p.getP()) || "komn_size".equalsIgnoreCase(p.getP()) ||
                        "living_area".equalsIgnoreCase(p.getP()) || "room_area".equalsIgnoreCase(p.getP()) ||
                        "s_komn".equalsIgnoreCase(p.getP()) || "living_space".equalsIgnoreCase(p.getP()))
                .map(KufarAdsResponseDto.ParameterDto::getV)
                .findFirst()
                .orElse(null);

        return parseFloatSafe(value);
    }

    @Named("furniture")
    default boolean furniture(List<KufarAdsResponseDto.ParameterDto> params) {
        Object value = getParameter(params, "furniture");
        return value != null && Boolean.parseBoolean(value.toString());
    }

    @Named("metroStation")
    default String metroStation(List<KufarAdsResponseDto.ParameterDto> params) {
        Object value = getParameter(params, "metro");
        if (value == null) {
            return null;
        }

        if (value instanceof List<?> list && !list.isEmpty()) {
            return extractValue(list.get(0));
        }

        return extractValue(value);
    }

    default Integer getIntegerParameter(
            List<KufarAdsResponseDto.ParameterDto> params,
            String code
    ) {
        Object value = getParameter(params, code);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof List<?> list && !list.isEmpty()) {
            return parseIntSafe(list.get(0));
        }

        return parseIntSafe(value);
    }

    default Integer parseIntSafe(Object value) {
        if (value == null) {
            return null;
        }

        String str = value.toString()
                .replace("[", "")
                .replace("]", "")
                .trim();

        if (str.isEmpty()) {
            return null;
        }

        if (str.endsWith("+")) {
            str = str.substring(0, str.length() - 1);
        }

        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float parseFloatSafe(Object value) {
        if (value == null) return null;

        String str = value.toString()
                .replace("[", "")
                .replace("]", "")
                .trim();

        if (str.isEmpty()) return null;

        try {
            return Float.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default Object getParameter(
            List<KufarAdsResponseDto.ParameterDto> params,
            String code
    ) {
        if (params == null) {
            return null;
        }

        return params.stream()
                .filter(p -> code.equalsIgnoreCase(p.getP()))
                .map(KufarAdsResponseDto.ParameterDto::getVl)
                .findFirst()
                .orElse(null);
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
}