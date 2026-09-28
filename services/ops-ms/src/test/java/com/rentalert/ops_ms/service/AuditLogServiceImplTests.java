package com.rentalert.ops_ms.service;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.ops_ms.domain.AuditLog;
import com.rentalert.ops_ms.mapper.AuditLogMapper;
import com.rentalert.ops_ms.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditLogServiceImplTests {

    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditLogService service =
        new AuditLogServiceImpl(repository, Mappers.getMapper(AuditLogMapper.class));

    private final AdminActionEvent event = AdminActionEvent.builder().event(AdminEvent.JOB_CLEAN_UP).build();

    @Test
    void recordsNewEvent() {
        service.record(event);

        verify(repository).save(argThat((AuditLog log) -> event.getIdempotencyKey().equals(log.getEventId())));
    }

    @Test
    void ignoresRedeliveredEvent() {
        when(repository.existsByEventId(event.getIdempotencyKey())).thenReturn(true);

        service.record(event);

        verify(repository, never()).save(any());
    }
}
