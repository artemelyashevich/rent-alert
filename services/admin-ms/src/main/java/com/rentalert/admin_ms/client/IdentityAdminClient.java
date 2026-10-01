package com.rentalert.admin_ms.client;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.auth.AdminLoginRequest;
import com.rentalert.dto.auth.LoginResponse;
import com.rentalert.dto.profile.FeedbackResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "identity-ms", contextId = "identityAdminClient", path = "/identity/internal/admin")
public interface IdentityAdminClient {

    @PostMapping("/login")
    LoginResponse login(@RequestBody AdminLoginRequest request);

    @GetMapping("/feedbacks")
    PageResponse<FeedbackResponse> findFeedbacks(
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") List<String> sort
    );
}
