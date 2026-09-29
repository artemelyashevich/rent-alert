package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.RestService;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.OnlinerAdsResponseDto;
import com.rentalert.processing_ms.mapper.OnlinerListingMapper;
import com.rentalert.processing_ms.util.ParserRequestUtil;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnlinerRoomListingScheduleParser extends AbstractScheduleParser<OnlinerAdsResponseDto.ApartmentDto> {

    private final RestService restService;
    private final OnlinerListingMapper onlinerListingMapper;

    @Override
    protected List<OnlinerAdsResponseDto.ApartmentDto> findTasks(City city) {
        String uri = restService.buildUri(
            applicationProperties.getOnliner().getUrl(),
            ParserRequestUtil.buildOnlinerRoomParams(getSettings(), city)
        );
        OnlinerAdsResponseDto response = restService.processGetRequest(uri, OnlinerAdsResponseDto.class);
        return response.getApartments() != null ? response.getApartments() : List.of();
    }

    @Override
    protected String getTaskKey(OnlinerAdsResponseDto.ApartmentDto task) {
        return task.getId().toString();
    }

    @Override
    protected Listing getListing(OnlinerAdsResponseDto.ApartmentDto task) {
        Listing listing = onlinerListingMapper.toEntity(task);
        listing.setCategory(ListingCategory.ROOM);
        return listing;
    }

    @Override
    protected ScheduleProvider getSchedulerProvider() {
        return ScheduleProvider.ONLINER;
    }

    @Override
    public String getSchedulerName() {
        return getSchedulerProvider().name() + "_ROOM";
    }
}
