package com.rentalert.internal.client;

import com.rentalert.dto.profile.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = IdentityMsClient.SERVICE_NAME, path = "/identity/internal")
public interface IdentityMsClient {

    String SERVICE_NAME = "identity-ms";

    @GetMapping("/profiles/{userId}")
    UserProfileResponse getProfile(@PathVariable("userId") Long userId);
}
