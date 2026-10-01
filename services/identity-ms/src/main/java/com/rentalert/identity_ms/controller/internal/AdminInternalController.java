package com.rentalert.identity_ms.controller.internal;

import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.profile.FeedbackResponse;
import com.rentalert.identity_ms.config.PublicUrls;
import com.rentalert.identity_ms.service.FeedbackService;
import com.rentalert.identity_ms.service.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG)
@RequiredArgsConstructor
public class AdminInternalController {

    private final LoginService<AdminLoginRequest> adminLoginService;
    private final FeedbackService feedbackService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody AdminLoginRequest request) {
        return adminLoginService.auth(request);
    }

    @GetMapping("/feedbacks")
    public PageResponse<FeedbackResponse> getAllFeedbacks(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageUtil.toResponse(feedbackService.getAllFeedbacks(pageable));
    }
}
