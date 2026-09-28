package com.rentalert.identity_ms.mapper;

import com.rentalert.dto.currency.Currency;
import com.rentalert.identity_ms.domain.UserFilter;
import com.rentalert.dto.profile.filter.UserFilterRequest;
import com.rentalert.dto.profile.filter.UserFilterResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserFilterMapper {

    UserFilterResponse toResponse(UserFilter userFilter);

    List<UserFilterResponse> toResponse(List<UserFilter> userFilters);

    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "active", constant = "false")
    @Mapping(target = "currency", source = "request.currency", qualifiedByName = "mapCurrency")
    @Mapping(target = "city", source = "request.city", qualifiedByName = "mapCity")
    UserFilter toEntity(UserFilterRequest request);

    @Mapping(target = "userProfile", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "city", source = "request.city", qualifiedByName = "mapCity")
    void update(@MappingTarget UserFilter userFilter, UserFilterRequest request);

    @Named("mapCurrency")
    default Currency mapCurrency(Currency currency) {
        if (currency == null) {
            return Currency.USD;
        }
        return currency;
    }

    @Named("mapCity")
    default String mapCity(String city) {
        if (city == null || city.isBlank()) {
            return "Минск";
        }
        return city.trim();
    }
}