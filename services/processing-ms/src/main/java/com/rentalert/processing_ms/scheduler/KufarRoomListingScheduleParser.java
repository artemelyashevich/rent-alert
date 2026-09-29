package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.RestService;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.KufarAdsResponseDto;
import com.rentalert.processing_ms.dto.KufarAdsResponseDto.AdDto;
import com.rentalert.processing_ms.mapper.KufarListingMapper;
import com.rentalert.processing_ms.util.ParserRequestUtil;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class KufarRoomListingScheduleParser extends AbstractScheduleParser<KufarAdsResponseDto.AdDto> {

    private final RestService restService;
    private final KufarListingMapper kufarListingMapper;

    @Override
    protected List<KufarAdsResponseDto.AdDto> findTasks(City city) {
        String baseUrl = UriComponentsBuilder.fromUriString(applicationProperties.getKufar().getUrl())
            .replaceQueryParam("gtsy")
            .build()
            .toUriString();
        String uri = restService.buildUri(
            baseUrl,
            ParserRequestUtil.buildKufarRoomParams(getSettings(), city)
        );
        KufarAdsResponseDto response = restService.processGetRequest(uri, KufarAdsResponseDto.class);
        return response.getAds();
    }

    @Override
    protected String getTaskKey(AdDto task) {
        return task.getAdId().toString();
    }

    @Override
    protected Listing getListing(AdDto task) {
        Listing listing = kufarListingMapper.toEntity(task);
        listing.setCategory(ListingCategory.ROOM);
        return listing;
    }

    @Override
    protected ScheduleProvider getSchedulerProvider() {
        return ScheduleProvider.KUFAR;
    }

    @Override
    public String getSchedulerName() {
        return getSchedulerProvider().name() + "_ROOM";
    }
}
