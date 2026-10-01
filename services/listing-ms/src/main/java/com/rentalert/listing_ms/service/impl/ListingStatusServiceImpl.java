package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.domain.ListingStatusName;
import com.rentalert.listing_ms.dto.ListingStatusResponseDto;
import com.rentalert.listing_ms.mapper.ListingStatusMapper;
import com.rentalert.listing_ms.repository.ListingStatusRepository;
import com.rentalert.listing_ms.service.ListingStatusService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListingStatusServiceImpl implements ListingStatusService {

    private final ListingStatusRepository listingStatusRepository;
    private final ListingStatusMapper listingStatusMapper;

    @Override
    public List<ListingStatusResponseDto> findAll() {
        return listingStatusMapper.toDto(listingStatusRepository.findAll());
    }

    @Override
    public ListingStatus findById(Long id) {
        return listingStatusRepository.findById(id)
            .orElseThrow(() -> new AppException(
                InternalExceptionCode.EX404_LISTING_STATUS_NOT_FOUND,
                HttpStatus.NOT_FOUND
            ));
    }

    @Override
    public ListingStatus findByName(ListingStatusName name) {
        return listingStatusRepository.findByName(name)
            .orElseThrow(() -> new AppException(
                InternalExceptionCode.EX404_LISTING_STATUS_NOT_FOUND,
                HttpStatus.NOT_FOUND
            ));
    }
}
