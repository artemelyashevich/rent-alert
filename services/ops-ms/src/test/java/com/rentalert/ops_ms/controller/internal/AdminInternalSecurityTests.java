package com.rentalert.ops_ms.controller.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rentalert.common.security.AuthenticatedUser;
import com.rentalert.common.security.AuthenticationService;
import com.rentalert.common.security.InternalAuth;
import com.rentalert.common.security.IpAccessGuard;
import com.rentalert.ops_ms.configuration.SecurityConfiguration;
import com.rentalert.ops_ms.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminAuditLogInternalController.class, properties = "app.internal.token=internal-secret")
@Import(SecurityConfiguration.class)
class AdminInternalSecurityTests {

    private static final String URL = "/ops/internal/admin/audit-logs";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private IpAccessGuard ipAccessGuard;

    @MockitoBean
    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        when(authenticationService.findUserByToken("admin-token")).thenReturn(new AuthenticatedUser(1L, "admin", null, "ADMIN"));
        when(authenticationService.findUserByToken("user-token")).thenReturn(new AuthenticatedUser(2L, "user", null, "USER"));
        when(auditLogService.findAll(any(), any(), any())).thenReturn(Page.empty());
    }

    @Test
    void adminCallThroughInternalTokenIsAllowed() throws Exception {
        mockMvc.perform(get(URL)
                .header(InternalAuth.HEADER, "internal-secret")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
            .andExpect(status().isOk());
    }

    @Test
    void regularUserIsForbidden() throws Exception {
        mockMvc.perform(get(URL)
                .header(InternalAuth.HEADER, "internal-secret")
                .header(HttpHeaders.AUTHORIZATION, "Bearer user-token"))
            .andExpect(status().isForbidden());
    }

    @Test
    void serviceCallWithoutUserIsForbidden() throws Exception {
        mockMvc.perform(get(URL).header(InternalAuth.HEADER, "internal-secret"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminWithoutInternalTokenIsForbidden() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
            .andExpect(status().isForbidden());
    }
}
