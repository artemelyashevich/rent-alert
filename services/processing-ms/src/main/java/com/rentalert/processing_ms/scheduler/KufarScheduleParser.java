package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.RestService;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.KufarAdsResponseDto;
import com.rentalert.processing_ms.dto.KufarAdsResponseDto.AdDto;
import com.rentalert.processing_ms.mapper.KufarListingMapper;
import com.rentalert.processing_ms.util.ParserRequestUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KufarScheduleParser extends AbstractScheduleParser<KufarAdsResponseDto.AdDto> {

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
            ParserRequestUtil.buildKufarParams(getSettings(), city)
        );
        KufarAdsResponseDto kufarAdsResponseDto = restService.processGetRequest(uri, KufarAdsResponseDto.class);
        return kufarAdsResponseDto.getAds();
    }

    @Override
    protected String getTaskKey(KufarAdsResponseDto.AdDto task) {
        return task.getAdId().toString();
    }

    @Override
    protected Listing getListing(AdDto task) {
        return kufarListingMapper.toEntity(task);
    }

    @Override
    protected ScheduleProvider getSchedulerProvider() {
        return ScheduleProvider.KUFAR;
    }
}
