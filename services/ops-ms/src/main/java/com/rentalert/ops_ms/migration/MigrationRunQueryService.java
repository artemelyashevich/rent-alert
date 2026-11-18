package com.rentalert.ops_ms.migration;

import com.rentalert.common.exception.AppNotFoundException;
import com.rentalert.common.util.jpa.PageUtil;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.migration.MigrationRunResponse;
import com.rentalert.ops_ms.domain.MigrationRun;
import com.rentalert.ops_ms.mapper.MigrationRunMapper;
import com.rentalert.ops_ms.repository.MigrationRunRepository;
import com.rentalert.ops_ms.repository.MigrationRunStepRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MigrationRunQueryService {

    private final MigrationRunMapper migrationRunMapper;
    private final MigrationRunRepository runRepository;
    private final MigrationRunStepRepository stepRepository;

    @Transactional(readOnly = true)
    public MigrationRunResponse get(UUID migrationId) {
        MigrationRun run = runRepository.findById(migrationId)
            .orElseThrow(() -> new AppNotFoundException("migration run not found: " + migrationId));
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public PageResponse<MigrationRunResponse> findAll(Pageable pageable) {
        return PageUtil.toResponse(runRepository.findAll(pageable).map(this::toResponse));
    }

    private MigrationRunResponse toResponse(MigrationRun run) {
        return migrationRunMapper.toResponse(run, stepRepository.findAllByMigrationIdOrderByStepOrder(run.getId()));
    }
}
