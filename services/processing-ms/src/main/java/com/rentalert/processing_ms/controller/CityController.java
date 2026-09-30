package com.rentalert.processing_ms.controller;

import com.rentalert.processing_ms.config.PublicUrls;
import com.rentalert.processing_ms.service.CityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cities", description = "Справочник городов")
@RestController
@RequestMapping(PublicUrls.CITY_TAG)
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @Operation(
        summary = "Получить список уникальных названий городов",
        description = "Возвращает отсортированный список уникальных названий городов из справочника."
    )
    @GetMapping("/names")
    public List<String> getCityNames() {
        return cityService.findDistinctNames();
    }
}
