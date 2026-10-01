package com.rentalert.ops_ms.controller.internal;

import com.rentalert.common.exception.AppException;
import com.rentalert.common.exception.InternalExceptionCode;
import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.event.job.JobInitiator;
import com.rentalert.dto.job.JobSettingResponse;
import com.rentalert.ops_ms.cleanup.CleanupSagaOrchestrator;
import com.rentalert.ops_ms.configuration.PublicUrls;
import com.rentalert.ops_ms.service.JobSettingService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG)
@RequiredArgsConstructor
public class AdminJobInternalController {

    private final JobSettingService jobSettingService;
    private final CleanupSagaOrchestrator cleanupSagaOrchestrator;

    @GetMapping("/job-settings")
    public PageResponse<JobSettingResponse> findAll(
        @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return PageUtil.toResponse(jobSettingService.findAll(pageable));
    }

    @PostMapping("/jobs/cleanup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public UUID startCleanup() {
        return cleanupSagaOrchestrator.start(Instant.now(), JobInitiator.ADMIN)
            .orElseThrow(() -> new AppException(InternalExceptionCode.EX409_JOB_ALREADY_RUNNING, HttpStatus.CONFLICT));
    }
}
