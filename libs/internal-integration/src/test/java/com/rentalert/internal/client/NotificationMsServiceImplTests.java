package com.rentalert.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.common.exception.UpstreamHttpException;
import com.rentalert.dto.PageResponse;
import com.rentalert.dto.notification.BroadcastRecipientResponse;
import com.rentalert.dto.notification.BroadcastRecipientStatus;
import com.rentalert.internal.client.service.NotificationMsService;
import com.rentalert.internal.client.service.impl.NotificationMsServiceImpl;
import com.rentalert.internal.resilience.RetryableExceptionPredicate;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class NotificationMsServiceImplTests {

    private final NotificationMsClient client = mock(NotificationMsClient.class);
    private final NotificationMsService service = new NotificationMsServiceImpl(
        client,
        RetryRegistry.of(RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(1))
            .retryOnException(new RetryableExceptionPredicate())
            .build()),
        CircuitBreakerRegistry.ofDefaults()
    );

    private final PageResponse<BroadcastRecipientResponse> page = new PageResponse<>(
        List.of(new BroadcastRecipientResponse(1L, 7L, "ivan", null, BroadcastRecipientStatus.SENT, null, null)), 0, 50, 1, 1);

    @Test
    void recipientsWithoutUserFilterUseGetWithoutIds() {
        when(client.findRecipients(eq(3L), anyInt(), anyInt(), anyList())).thenReturn(page);

        assertThat(service.findRecipients(3L, null, PageRequest.of(0, 50))).isSameAs(page);

        verify(client).findRecipients(eq(3L), eq(0), eq(50), anyList());
        verify(client, never()).findRecipientsByUserIds(any(), any(), anyInt(), anyInt(), anyList());
    }

    @Test
    void recipientsByUserIdsSendIdsInRequestBody() {
        List<Long> userIds = List.of(7L, 8L);
        when(client.findRecipientsByUserIds(eq(3L), eq(userIds), anyInt(), anyInt(), anyList())).thenReturn(page);

        assertThat(service.findRecipients(3L, userIds, PageRequest.of(1, 20))).isSameAs(page);

        verify(client).findRecipientsByUserIds(eq(3L), eq(userIds), eq(1), eq(20), anyList());
        verify(client, never()).findRecipients(any(), anyInt(), anyInt(), anyList());
    }

    @Test
    void recipientsByUserIdsAreRetriedAsReadOperation() {
        List<Long> userIds = List.of(7L);
        when(client.findRecipientsByUserIds(eq(3L), eq(userIds), anyInt(), anyInt(), anyList()))
            .thenThrow(new UpstreamHttpException("unavailable", 503, null))
            .thenReturn(page);

        assertThat(service.findRecipients(3L, userIds, PageRequest.of(0, 50))).isSameAs(page);

        verify(client, times(2)).findRecipientsByUserIds(eq(3L), eq(userIds), eq(0), eq(50), anyList());
    }
}
