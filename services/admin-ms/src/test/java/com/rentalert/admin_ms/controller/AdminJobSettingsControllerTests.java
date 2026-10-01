package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.OpsAdminClient;
import com.rentalert.admin_ms.client.ProcessingAdminClient;
import com.rentalert.admin_ms.job.CacheCleanupJob;
import com.rentalert.common.exception.AppBadRequestException;
import com.rentalert.dto.event.admin.AdminEvent;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AdminJobSettingsControllerTests {

    private final OpsAdminClient opsAdminClient = mock(OpsAdminClient.class);
    private final ProcessingAdminClient processingAdminClient = mock(ProcessingAdminClient.class);
    private final CacheCleanupJob cacheCleanupJob = mock(CacheCleanupJob.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final AdminJobSettingsController controller =
        new AdminJobSettingsController(opsAdminClient, processingAdminClient, cacheCleanupJob, auditPublisher);

    @Test
    void cacheCleanupIsAuditedBeforeSessionsAreWiped() {
        when(cacheCleanupJob.run()).thenReturn(42L);

        assertThat(controller.cleanCache(true)).isEqualTo(42L);

        InOrder order = inOrder(auditPublisher, cacheCleanupJob);
        order.verify(auditPublisher).publish(AdminEvent.JOB_CLEANUP_CACHE_ALL);
        order.verify(cacheCleanupJob).run();
    }

    @Test
    void partialCacheCleanupIsUnsupported() {
        assertThatThrownBy(() -> controller.cleanCache(false)).isInstanceOf(AppBadRequestException.class);
        verifyNoInteractions(cacheCleanupJob, auditPublisher);
    }
}
