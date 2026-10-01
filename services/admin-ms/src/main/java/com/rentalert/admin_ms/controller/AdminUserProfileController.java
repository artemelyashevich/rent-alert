package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.audit.AdminAuditPublisher;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.profile.UpdateUserRoleRequest;
import com.rentalert.dto.profile.UserProfileResponse;
import com.rentalert.internal.client.service.IdentityMsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin user profiles", description = "Профили пользователей")
@RestController
@RequestMapping(PublicUrls.PROFILE_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserProfileController {

    private final IdentityMsService identityMsService;
    private final AdminAuditPublisher auditPublisher;

    @Operation(summary = "Получить профили (поиск по username, email или id)")
    @GetMapping
    public PageResponse<UserProfileResponse> findAll(
        @ParameterObject @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) String filter
    ) {
        return identityMsService.findProfiles(filter, pageable);
    }

    @Operation(summary = "Получить профиль пользователя")
    @GetMapping("/{userId}")
    public UserProfileResponse findById(@PathVariable Long userId) {
        return identityMsService.findProfile(userId);
    }

    @Operation(summary = "Изменить роль пользователя; активная сессия пользователя сбрасывается")
    @PatchMapping("/{userId}/role")
    public UserProfileResponse updateRole(@PathVariable Long userId, @Valid @RequestBody UpdateUserRoleRequest request) {
        UserProfileResponse profile = identityMsService.updateRole(userId, request);
        auditPublisher.publish(AdminEvent.USER_PROFILES_CHANGE_ROLE, userId, request.role().name());
        return profile;
    }

    @Operation(summary = "Удалить пользователя и все его данные, кроме отзывов")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId) {
        identityMsService.deleteProfile(userId);
        auditPublisher.publish(AdminEvent.USER_PROFILES_DELETE, userId);
    }
}
