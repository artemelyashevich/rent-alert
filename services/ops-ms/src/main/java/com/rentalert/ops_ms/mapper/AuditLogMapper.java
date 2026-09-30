package com.rentalert.ops_ms.mapper;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.ops_ms.domain.AuditLog;
import com.rentalert.ops_ms.dto.AuditLogResponse;
import java.util.IllegalFormatException;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AuditLogMapper {

    // A producer sending fewer params than the template expects must not lose the audit entry
    private static String describe(String template, List<String> params) {
        if (params == null || params.isEmpty()) {
            return template;
        }
        try {
            return template.formatted(params.toArray());
        } catch (IllegalFormatException e) {
            return template + " " + params;
        }
    }

    AuditLogResponse toResponse(AuditLog auditLog);

    default AuditLog toEntity(AdminActionEvent event) {
        return AuditLog.builder()
            .eventId(event.getIdempotencyKey())
            .eventName(event.getEvent().name())
            .eventDescription(describe(event.getEvent().getDescription(), event.getParams()))
            .userId(event.getInitiatorId())
            .eventInitiator(event.getEventType())
            .eventStatus(event.getEventStatus() == null ? EventStatus.SUCCESS : event.getEventStatus())
            .build();
    }

    default AuditLog toEntity(UserActionEvent event) {
        StringBuilder description = new StringBuilder(describe(event.getEvent().getDescription(), event.getParams()))
            .append(". Пользователь id='").append(event.getInitiatorId()).append('\'');
        if (event.getFilterId() != null) {
            description.append(". Фильтр id='").append(event.getFilterId()).append('\'');
        }
        return AuditLog.builder()
            .eventId(event.getIdempotencyKey())
            .eventName(event.getEvent().getGroup().getName())
            .eventDescription(description.toString())
            .userId(event.getInitiatorId())
            .eventInitiator(event.getEventType())
            .eventStatus(event.getEventStatus() == null ? EventStatus.SUCCESS : event.getEventStatus())
            .build();
    }
}
