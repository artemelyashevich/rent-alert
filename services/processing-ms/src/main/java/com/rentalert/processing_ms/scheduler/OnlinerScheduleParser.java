package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.RestService;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.OnlinerAdsResponseDto;
import com.rentalert.processing_ms.mapper.OnlinerListingMapper;
import com.rentalert.processing_ms.util.ParserRequestUtil;
import com.rentalert.dto.listing.ScheduleProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlinerScheduleParser extends AbstractScheduleParser<OnlinerAdsResponseDto.ApartmentDto> {

    private final RestService restService;
    private final OnlinerListingMapper onlinerListingMapper;

    @Override
    protected List<OnlinerAdsResponseDto.ApartmentDto> findTasks(City city) {
        String uri = restService.buildUri(
            applicationProperties.getOnliner().getUrl(),
            ParserRequestUtil.buildOnlinerParams(getSettings(), city)
        );
        OnlinerAdsResponseDto response = restService.processGetRequest(uri, OnlinerAdsResponseDto.class);
        List<OnlinerAdsResponseDto.ApartmentDto> apartments = response.getApartments() != null ? response.getApartments() : List.of();
        return apartments;
    }

    @Override
    protected String getTaskKey(OnlinerAdsResponseDto.ApartmentDto task) {
        return task.getId().toString();
    }

    @Override
    protected Listing getListing(OnlinerAdsResponseDto.ApartmentDto task) {
        return onlinerListingMapper.toEntity(task);
    }

    @Override
    protected ScheduleProvider getSchedulerProvider() {
        return ScheduleProvider.ONLINER;
    }
}
