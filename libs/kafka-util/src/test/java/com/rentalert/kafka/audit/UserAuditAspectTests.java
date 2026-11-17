package com.rentalert.kafka.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.EventStatus;
import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.UserActionEvent;
import com.rentalert.dto.event.user.UserEvent;
import com.rentalert.kafka.outbox.OutboxPublisher;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class UserAuditAspectTests {

    private final OutboxPublisher outboxPublisher = mock(OutboxPublisher.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @Test
    void userIdFromExpressionAndParamsAreRecordedOnSuccess() {
        AuditedService service = proxy(null);

        String result = service.reveal(100L, 5L);

        assertThat(result).isEqualTo("contact-100");
        UserActionEvent event = capturedEvent("5");
        assertThat(event.getInitiatorId()).isEqualTo(5L);
        assertThat(event.getEvent()).isEqualTo(UserEvent.ROOMMATE_CONTACT_REVEALED);
        assertThat(event.getEventStatus()).isEqualTo(EventStatus.SUCCESS);
        assertThat(event.getParams()).isEqualTo(List.of("100", "5"));
        assertThat(event.getFilterId()).isNull();
    }

    @Test
    void eventIsPublishedExactlyOnceOnSuccess() {
        proxy(null).reveal(100L, 5L);

        verify(outboxPublisher, times(1)).publish(eq(KafkaTopics.USER_EVENTS), eq("5"), any());
        verify(transactionManager, times(1)).getTransaction(any());
    }

    @Test
    void userIdFallsBackToProviderWhenExpressionIsBlank() {
        AuditedService service = proxy(() -> 9L);

        Long id = service.create(3L);

        assertThat(id).isEqualTo(7L);
        UserActionEvent event = capturedEvent("9");
        assertThat(event.getInitiatorId()).isEqualTo(9L);
        assertThat(event.getParams()).isEqualTo(List.of("3"));
    }

    @Test
    void filterIdIsEvaluatedFromResultAfterTheCall() {
        proxy(() -> 9L).create(3L);

        assertThat(capturedEvent("9").getFilterId()).isEqualTo(7L);
    }

    @Test
    void blankUserIdWithoutProviderFailsBeforeTheCall() {
        AuditedService service = proxy(null);

        assertThatThrownBy(() -> service.create(3L)).isInstanceOf(IllegalStateException.class);
        assertThat(service.calls()).isZero();
        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void providerReturningNullFailsBeforeTheCall() {
        AuditedService service = proxy(() -> null);

        assertThatThrownBy(() -> service.create(3L)).isInstanceOf(IllegalStateException.class);
        assertThat(service.calls()).isZero();
        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void failedCallPublishesNothing() {
        AuditedService service = proxy(null);

        assertThatThrownBy(() -> service.fail(5L)).isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(outboxPublisher);
    }

    @Test
    void withoutActiveTransactionCallAndPublishRunInOneTransactionTemplateCall() {
        proxy(null).reveal(100L, 5L);

        InOrder inOrder = inOrder(transactionManager, outboxPublisher);
        inOrder.verify(transactionManager).getTransaction(any());
        inOrder.verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq("5"), any());
        inOrder.verify(transactionManager).commit(any());
        verify(transactionManager, times(1)).getTransaction(any());
    }

    @Test
    void withoutActiveTransactionExceptionRollsBackAndPublishesNothing() {
        AuditedService service = proxy(null);

        assertThatThrownBy(() -> service.fail(5L)).isInstanceOf(IllegalArgumentException.class);

        verify(transactionManager, times(1)).rollback(any());
        verify(transactionManager, never()).commit(any());
        verifyNoInteractions(outboxPublisher);
    }

    private UserActionEvent capturedEvent(String expectedKey) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(outboxPublisher).publish(eq(KafkaTopics.USER_EVENTS), eq(expectedKey), captor.capture());
        return (UserActionEvent) captor.getValue();
    }

    private AuditedService proxy(CurrentUserIdProvider provider) {
        AspectJProxyFactory factory = new AspectJProxyFactory(new AuditedService());
        factory.setProxyTargetClass(true);
        factory.addAspect(new UserAuditAspect(provider, outboxPublisher, transactionManager));
        return factory.getProxy();
    }

    static class AuditedService {

        private int calls;

        @UserAudit(value = UserEvent.ROOMMATE_CONTACT_REVEALED, userId = "#userId", params = {"#matchId", "#userId"})
        public String reveal(Long matchId, Long userId) {
            calls++;
            return "contact-" + matchId;
        }

        @UserAudit(value = UserEvent.ROOMMATE_CONTACT_REVEALED, params = {"#listingId"}, filterId = "#result")
        public Long create(Long listingId) {
            calls++;
            return 7L;
        }

        @UserAudit(value = UserEvent.ROOMMATE_CONTACT_REVEALED, userId = "#userId")
        public void fail(Long userId) {
            calls++;
            throw new IllegalArgumentException("boom");
        }

        public int calls() {
            return calls;
        }
    }
}
