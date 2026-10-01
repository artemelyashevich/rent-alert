package com.rentalert.admin_ms.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.AdminActionEvent;
import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.admin.AdminEvent;
import com.rentalert.dto.user.UserSession;
import com.rentalert.redis.service.UserSessionService;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

class AdminAuditPublisherTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final UserSessionService userSessionService = mock(UserSessionService.class);
    private final AdminAuditPublisher publisher = new AdminAuditPublisher(kafkaTemplate, userSessionService);

    @Test
    void publishesActionOfCurrentAdmin() {
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(5L).build());
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(new CompletableFuture<>());

        publisher.publish(AdminEvent.CITY_TOGGLE_ACTIVE, 7L, false);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.AUDIT_EVENTS), eq("5"), event.capture());
        AdminActionEvent sent = (AdminActionEvent) event.getValue();
        assertThat(sent.getEvent()).isEqualTo(AdminEvent.CITY_TOGGLE_ACTIVE);
        assertThat(sent.getParams()).containsExactly("7", "false");
        assertThat(sent.getInitiatorId()).isEqualTo(5L);
        assertThat(sent.getEventStatus()).isEqualTo(EventStatus.SUCCESS);
        assertThat(sent.getIdempotencyKey()).isNotNull();
    }

    @Test
    void actionWithoutParamsHasNoParams() {
        when(userSessionService.getCurrentUser()).thenReturn(UserSession.builder().userId(5L).build());
        when(kafkaTemplate.send(any(String.class), any(String.class), any())).thenReturn(new CompletableFuture<>());

        publisher.publish(AdminEvent.AUDIT_LOG_CLEAN_UP);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.AUDIT_EVENTS), eq("5"), event.capture());
        assertThat(((AdminActionEvent) event.getValue()).getParams()).isNull();
    }
}
