package com.rentalert.ops_ms.mapper;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.ops_ms.domain.AuditLog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogMapperTests {

    private final AuditLogMapper mapper = Mappers.getMapper(AuditLogMapper.class);

    @Test
    void mapsEventAndFormatsDescription() {
        AdminActionEvent event = AdminActionEvent.builder()
            .event(AdminEvent.USER_PROFILES_CHANGE_ROLE)
            .params(List.of("42", "ADMIN"))
            .build();
        event.setInitiatorId(7L);
        event.setEventStatus(EventStatus.ERROR);

        AuditLog auditLog = mapper.toEntity(event);

        assertThat(auditLog.getEventId()).isEqualTo(event.getIdempotencyKey());
        assertThat(auditLog.getEventName()).isEqualTo("USER_PROFILES_CHANGE_ROLE");
        assertThat(auditLog.getEventDescription())
            .isEqualTo("Изменение роли пользователя. Id пользователя = '42'. Новая роль = 'ADMIN'");
        assertThat(auditLog.getUserId()).isEqualTo(7L);
        assertThat(auditLog.getEventInitiator()).isEqualTo(EventInitiatorType.ADMIN);
        assertThat(auditLog.getEventStatus()).isEqualTo(EventStatus.ERROR);
    }

    @Test
    void keepsTemplateWhenParamsAreMissing() {
        AdminActionEvent event = AdminActionEvent.builder()
            .event(AdminEvent.USER_PROFILES_CHANGE_ROLE)
            .params(List.of("42"))
            .build();

        AuditLog auditLog = mapper.toEntity(event);

        assertThat(auditLog.getEventDescription()).isEqualTo(AdminEvent.USER_PROFILES_CHANGE_ROLE.getDescription() + " [42]");
        assertThat(auditLog.getEventStatus()).isEqualTo(EventStatus.SUCCESS);
    }

    @Test
    void usesPlainDescriptionWithoutParams() {
        AuditLog auditLog = mapper.toEntity(AdminActionEvent.builder().event(AdminEvent.JOB_CLEAN_UP).build());

        assertThat(auditLog.getEventDescription()).isEqualTo("Очистка базы данных");
    }

    @Test
    void mapsUserEventWithGroupNameAndUser() {
        UserActionEvent event = UserActionEvent.builder()
            .event(UserEvent.FAVORITE_ADDED)
            .filterId(3L)
            .params(List.of("15"))
            .build();
        event.setInitiatorId(42L);

        AuditLog auditLog = mapper.toEntity(event);

        assertThat(auditLog.getEventId()).isEqualTo(event.getIdempotencyKey());
        assertThat(auditLog.getEventName()).isEqualTo("Избранное");
        assertThat(auditLog.getEventDescription())
            .isEqualTo("Добавление объявления id='15' в избранное. Пользователь id='42'. Фильтр id='3'");
        assertThat(auditLog.getUserId()).isEqualTo(42L);
        assertThat(auditLog.getEventInitiator()).isEqualTo(EventInitiatorType.USER);
        assertThat(auditLog.getEventStatus()).isEqualTo(EventStatus.SUCCESS);
    }
}
