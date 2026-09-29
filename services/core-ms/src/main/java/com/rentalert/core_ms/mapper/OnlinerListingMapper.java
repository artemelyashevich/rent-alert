package com.rentalert.core_ms.mapper;

import com.rentalert.core_ms.domain.Listing;
import com.rentalert.core_ms.dto.OnlinerAdsResponseDto.ApartmentDto;
import com.rentalert.core_ms.util.OnlinerParserSupport;
import com.rentalert.dto.currency.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", imports = Currency.class)
public interface OnlinerListingMapper {

    @Mapping(target = "source", constant = "ONLINER")
    @Mapping(target = "sourceId", expression = "java(dto.getId().toString())")
    @Mapping(target = "sourceUrl", source = "url")
    @Mapping(target = "title", source = "location.userAddress")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "street", source = "location.address")
    @Mapping(target = "metroStation", ignore = true)
    @Mapping(target = "price", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "rooms", source = "rentType", qualifiedByName = "parseRooms")
    @Mapping(target = "areaTotal", source = "areaTotal", qualifiedByName = "onlinerAreaToFloat")
    @Mapping(target = "roomArea", source = "areaLiving", qualifiedByName = "onlinerAreaToFloat")
    @Mapping(target = "geoLat", source = "location.latitude")
    @Mapping(target = "geoLng", source = "location.longitude")
    @Mapping(target = "floor", ignore = true)
    @Mapping(target = "floorsTotal", ignore = true)
    @Mapping(target = "furniture", ignore = true)
    @Mapping(target = "photos", source = "photo", qualifiedByName = "toPhotosArray")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "parsedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAtProvider", source = "lastTimeUp", qualifiedByName = "toInstant")
    @Mapping(target = "category", ignore = true)
    Listing toEntity(ApartmentDto dto);

    @AfterMapping
    default void after(ApartmentDto dto, @MappingTarget Listing listing) {
        if (dto.getPrice().getUsdAmount() != null) {
            listing.setCurrency(Currency.USD);
            listing.setPrice(dto.getPrice().getUsdAmount().setScale(2, RoundingMode.HALF_UP));
            return;
        }
        if (dto.getPrice().getBynAmount() != null) {
            listing.setCurrency(Currency.BYN);
            listing.setPrice(dto.getPrice().getBynAmount().setScale(2, RoundingMode.HALF_UP));
        }
    }

    @Named("parseRooms")
    default Integer parseRooms(String rentType) {
        return OnlinerParserSupport.parseRooms(rentType);
    }

    @Named("onlinerAreaToFloat")
    default Float onlinerAreaToFloat(BigDecimal value) {
        return value == null ? null : value.floatValue();
    }

    @Named("toPhotosArray")
    default String[] toPhotosArray(String photo) {
        return photo != null ? new String[]{photo} : new String[0];
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