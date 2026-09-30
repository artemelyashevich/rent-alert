package com.rentalert.processing_ms.mapper;

import com.rentalert.processing_ms.domain.CurrencyRate;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.ParserResponseDto;
import com.rentalert.processing_ms.service.CurrencyRateService;
import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.currency.CurrencyPair;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", imports = {Arrays.class, Collections.class, BigDecimal.class, com.rentalert.dto.listing.ScheduleProvider.class})
public abstract class ParserResponseMapper {

    @Autowired
    private CurrencyRateService currencyRateService;

    @Mapping(target = "source", expression = "java(mapSource(listing.getSource()))")
    @Mapping(target = "address", source = "street")
    @Mapping(target = "area", expression = "java(toBigDecimal(listing.getAreaTotal()))")
    @Mapping(target = "totalFloors", source = "floorsTotal")
    @Mapping(target = "priceUsd", qualifiedByName = "getPriceUSD", source = "listing")
    @Mapping(target = "priceByn", qualifiedByName = "getPriceBYN",  source = "listing")
    @Mapping(target = "furnished", source = "furniture")
    @Mapping(target = "metro", source = "metroStation")
    @Mapping(target = "link", source = "sourceUrl")
    @Mapping(target = "images",
            expression = "java(listing.getPhotos() == null ? Collections.emptyList() : Arrays.asList(listing.getPhotos()))")
    @Mapping(target = "houseType", ignore = true)
    @Mapping(target = "contactName", ignore = true)
    public abstract ParserResponseDto toDto(Listing listing);

    @Named("getPriceBYN")
    protected BigDecimal getPriceBYN(Listing listing) {
        return getPrice(listing, Currency.BYN, CurrencyPair.USD_BYN);
    }

    @Named("getPriceUSD")
    protected BigDecimal getPriceUSD(Listing listing) {
        return getPrice(listing, Currency.USD, CurrencyPair.BYN_USD);
    }

    protected BigDecimal getPrice(Listing listing, Currency currency, CurrencyPair pair) {
        if (listing.getPrice() == null || currency.equals(listing.getCurrency())) {
            return listing.getPrice();
        }
        CurrencyRate latestRate = currencyRateService.getLatestRate(pair);
        // No rate loaded yet: leave the converted price out rather than fail
        return latestRate == null ? null : listing.getPrice().multiply(latestRate.getRate());
    }

    protected BigDecimal toBigDecimal(Float value) {
        return value == null ? null : new BigDecimal(value.toString());
    }

    protected com.rentalert.dto.listing.ScheduleProvider mapSource(com.rentalert.dto.listing.ScheduleProvider source) {
        if (source == null) {
            return null;
        }

        return switch (source) {
            case REALT -> com.rentalert.dto.listing.ScheduleProvider.REALT;
            case KUFAR -> com.rentalert.dto.listing.ScheduleProvider.KUFAR;
            case ONLINER -> com.rentalert.dto.listing.ScheduleProvider.ONLINER;
        };
    }
}