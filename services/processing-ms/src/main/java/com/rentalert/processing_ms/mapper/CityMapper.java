package com.rentalert.processing_ms.mapper;

import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.dto.CityResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityResponse toResponse(City city);

    List<CityResponse> toResponse(List<City> cities);
}
