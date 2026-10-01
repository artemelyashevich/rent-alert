package com.rentalert.admin_ms.controller;

import com.rentalert.admin_ms.client.IdentityAdminClient;
import com.rentalert.admin_ms.config.PublicUrls;
import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.profile.FeedbackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin feedbacks", description = "Обратная связь пользователей")
@RestController
@RequestMapping(PublicUrls.FEEDBACK_TAG)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final IdentityAdminClient identityAdminClient;

    @Operation(summary = "Получить все отзывы")
    @GetMapping
    public PageResponse<FeedbackResponse> findAll(@ParameterObject @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return identityAdminClient.findFeedbacks(pageable.getPageNumber(), pageable.getPageSize(), PageUtil.toSortParams(pageable));
    }
}
