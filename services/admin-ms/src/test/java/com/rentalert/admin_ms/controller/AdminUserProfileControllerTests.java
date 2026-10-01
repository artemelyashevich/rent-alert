package com.rentalert.admin_ms.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.client.IdentityAdminClient;
import com.rentalert.admin_ms.exception.ProxiedHttpException;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserRole;
import org.junit.jupiter.api.Test;

class AdminUserProfileControllerTests {

    private final IdentityAdminClient identityAdminClient = mock(IdentityAdminClient.class);
    private final AdminAuditPublisher auditPublisher = mock(AdminAuditPublisher.class);
    private final AdminUserProfileController controller = new AdminUserProfileController(identityAdminClient, auditPublisher);

    @Test
    void roleChangeIsAuditedWithNewRole() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.BLOCKED);

        controller.updateRole(5L, request);

        verify(identityAdminClient).updateRole(5L, request);
        verify(auditPublisher).publish(AdminEvent.USER_PROFILES_CHANGE_ROLE, 5L, "BLOCKED");
    }

    @Test
    void failedDeleteIsNotAudited() {
        doThrow(new ProxiedHttpException("not found", 404, "{}")).when(identityAdminClient).deleteProfile(5L);

        assertThatThrownBy(() -> controller.delete(5L)).isInstanceOf(ProxiedHttpException.class);
        verifyNoInteractions(auditPublisher);
    }
}
