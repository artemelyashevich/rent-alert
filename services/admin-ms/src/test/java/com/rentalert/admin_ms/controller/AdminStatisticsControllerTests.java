package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.client.UserBriefs;
import com.rentalert.dto.profile.UserBriefResponse;
import com.rentalert.dto.statistics.UserActionSummary;
import com.rentalert.internal.client.service.OpsMsService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdminStatisticsControllerTests {

    private final OpsMsService opsMsService = mock(OpsMsService.class);
    private final UserBriefs userBriefs = mock(UserBriefs.class);
    private final AdminStatisticsController controller = new AdminStatisticsController(opsMsService, userBriefs);

    @Test
    void summariesGetUsernamesFromIdentity() {
        LocalDate since = LocalDate.of(2026, 9, 1);
        when(opsMsService.getUserActionSummary(null, since, 10)).thenReturn(List.of(
            UserActionSummary.builder().userId(1L).totalActions(5L).build(),
            UserActionSummary.builder().userId(2L).totalActions(3L).build()));
        when(userBriefs.byId(List.of(1L, 2L))).thenReturn(Map.of(1L, new UserBriefResponse(1L, "anna", null)));

        List<UserActionSummary> summaries = controller.getUserActionSummary(null, since, 10);

        assertThat(summaries).extracting(UserActionSummary::username).containsExactly("anna", null);
        assertThat(summaries.getFirst().totalActions()).isEqualTo(5L);
    }
}
