package com.rentalert.processing_ms.util;

import com.rentalert.processing_ms.dto.OnlinerAdsResponseDto;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OnlinerParserSupport {

    public static Integer parseRooms(String rentType) {
        if (rentType == null) {
            return null;
        }
        return switch (rentType) {
            case "studio" -> 1;
            case "1_room" -> 1;
            case "2_rooms" -> 2;
            case "3_rooms" -> 3;
            case "4_rooms_and_more" -> 4;
            default -> null;
        };
    }

    public static BigDecimal extractAmount(OnlinerAdsResponseDto.ApartmentDto dto, String currencyCode) {
        if (dto.getPrice() == null || dto.getPrice().getConverted() == null) {
            return null;
        }
        var converted = dto.getPrice().getConverted();
        var amount = "USD".equalsIgnoreCase(currencyCode) ? converted.getUsd() : converted.getByn();
        return amount != null ? amount.getAmount() : null;
    }
}