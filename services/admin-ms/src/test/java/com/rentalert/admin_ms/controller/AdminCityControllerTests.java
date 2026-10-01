package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.ProcessingAdminClient;
import com.rentalert.admin_ms.exception.ProxiedHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.listing.CityRequest;
import com.rentalert.dto.listing.CityResponse;
import com.rentalert.dto.listing.ScheduleProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class AdminCityControllerTests {

    private final ProcessingAdminClient client = mock(ProcessingAdminClient.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final AdminCityController controller = new AdminCityController(client, auditPublisher);

    private final CityRequest request = CityRequest.builder().provider(ScheduleProvider.KUFAR).name("Минск").cityCode("minsk").build();

    @Test
    void createdCityIsAudited() {
        CityResponse city = new CityResponse(1L, ScheduleProvider.KUFAR, "Минск", "minsk", true, null, null);
        when(client.createCity(request)).thenReturn(city);

        assertThat(controller.create(request)).isSameAs(city);
        verify(auditPublisher).publish(AdminEvent.CITY_CREATE, "Минск", "KUFAR");
    }

    @Test
    void failedCreateIsNotAudited() {
        when(client.createCity(any())).thenThrow(new ProxiedHttpException("exists", 400, "{}"));

        assertThatThrownBy(() -> controller.create(request)).isInstanceOf(ProxiedHttpException.class);
        verifyNoInteractions(auditPublisher);
    }

    @Test
    void pagingIsForwarded() {
        PageResponse<CityResponse> page = new PageResponse<>(List.of(), 2, 10, 0, 0);
        when(client.findCities(2, 10, List.of("provider,ASC", "name,DESC"))).thenReturn(page);

        assertThat(controller.findAll(PageRequest.of(2, 10, Sort.by(Sort.Order.asc("provider"), Sort.Order.desc("name")))))
            .isSameAs(page);
    }
}
