package com.rentalert.processing_ms.scheduler;

import com.rentalert.common.service.RestService;
import com.rentalert.common.util.JsonHelper;
import com.rentalert.dto.listing.ListingCategory;
import com.rentalert.dto.listing.ScheduleProvider;
import com.rentalert.processing_ms.domain.City;
import com.rentalert.processing_ms.domain.Listing;
import com.rentalert.processing_ms.dto.RealtDto;
import com.rentalert.processing_ms.dto.RealtDto.ObjectDto;
import com.rentalert.processing_ms.dto.RealtParams;
import com.rentalert.processing_ms.mapper.RealtListingMapper;
import com.rentalert.processing_ms.util.ParserRequestUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtRoomListingScheduleParser extends AbstractScheduleParser<RealtDto.ObjectDto> {

    private final RestService restService;
    private final RealtListingMapper realtListingMapper;

    @Override
    protected List<RealtDto.ObjectDto> findTasks(City city) {
        RealtParams params = ParserRequestUtil.buildRealtRoomParams(getSettings(), city);
        RealtDto[] response = restService.processPostRequest(
            applicationProperties.getRealt().getUrl(),
            JsonHelper.toJson(List.of(params)),
            RealtDto[].class
        );
        return response[0].getData()
            .getSearchObjects()
            .getBody()
            .getResults();
    }

    @Override
    protected String getTaskKey(RealtDto.ObjectDto task) {
        return task.getUuid();
    }

    @Override
    protected Listing getListing(ObjectDto task) {
        Listing listing = realtListingMapper.toEntity(task);
        listing.setCategory(ListingCategory.ROOM);
        return listing;
    }

    @Override
    protected ScheduleProvider getSchedulerProvider() {
        return ScheduleProvider.REALT;
    }

    @Override
    public String getSchedulerName() {
        return getSchedulerProvider().name() + "_ROOM";
    }
}
