package com.rentalert.processing_ms.controller;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.CityUpdateRequest;
import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.mapper.CityMapper;
import com.rentalert.processing_ms.service.CityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/cities")
@RequiredArgsConstructor
public class AdminCityInternalController {

    private final CityService cityService;
    private final CityMapper cityMapper;

    @GetMapping
    public PageResponse<CityResponse> findAll(
        @PageableDefault(size = 20, sort = {"provider", "name"}, direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return PageUtil.toResponse(cityService.findAll(pageable).map(cityMapper::toResponse));
    }

    @GetMapping("/{id}")
    public CityResponse findById(@PathVariable Long id) {
        return cityMapper.toResponse(cityService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@Valid @RequestBody CityRequest request) {
        return cityMapper.toResponse(
            cityService.create(request.getProvider(), request.getName(), request.getCityCode(), request.isActive()));
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable Long id, @Valid @RequestBody CityUpdateRequest request) {
        return cityMapper.toResponse(cityService.update(id, request.getName(), request.getActive()));
    }

    @PutMapping("/{id}/active")
    public CityResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return cityMapper.toResponse(cityService.setActive(id, active));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        cityService.delete(id);
    }
}
