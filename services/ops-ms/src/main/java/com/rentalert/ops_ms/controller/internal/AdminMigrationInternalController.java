package com.rentalert.ops_ms.controller.internal;

import com.rentalert.dto.PageResponse;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.dto.migration.MigrationStartRequest;
import com.rentalert.ops_ms.configuration.PublicUrls;
import com.rentalert.ops_ms.migration.MigrationRunQueryService;
import com.rentalert.ops_ms.migration.MigrationSagaOrchestrator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(PublicUrls.INTERNAL_ADMIN_TAG + "/migrations")
@RequiredArgsConstructor
public class AdminMigrationInternalController {

    private final MigrationRunQueryService queryService;
    private final MigrationSagaOrchestrator orchestrator;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MigrationRunResponse start(@RequestBody MigrationStartRequest request, @RequestParam Long requestedBy) {
        return queryService.get(orchestrator.start(request, requestedBy));
    }

    @GetMapping
    public PageResponse<MigrationRunResponse> findAll(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return queryService.findAll(pageable);
    }

    @GetMapping("/{id}")
    public MigrationRunResponse get(@PathVariable UUID id) {
        return queryService.get(id);
    }

    @PostMapping("/{id}/cancel")
    public MigrationRunResponse cancel(@PathVariable UUID id) {
        orchestrator.cancel(id);
        return queryService.get(id);
    }
}
