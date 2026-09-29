package com.rentalert.listing_ms.service.impl;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.listing_ms.domain.ListingStatus;
import com.rentalert.listing_ms.dto.ListingStatusResponseDto;
import com.rentalert.listing_ms.mapper.ListingStatusMapper;
import com.rentalert.listing_ms.repository.ListingStatusRepository;
import com.rentalert.listing_ms.service.ListingStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
