package com.rentalert.kafka.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.ServiceStartedEvent;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.kafka.core.KafkaTemplate;

class ServiceStartedPublisherTests {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final ServiceStartedPublisher publisher = new ServiceStartedPublisher(kafkaTemplate, "listing-ms");

    @Test
    void reportsStartKeyedByServiceName() {
        when(kafkaTemplate.send(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.any()))
            .thenReturn(new CompletableFuture<>());

        publisher.publish();

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.SERVICE_STARTED), eq("listing-ms"), event.capture());
        ServiceStartedEvent started = (ServiceStartedEvent) event.getValue();
        assertThat(started.service()).isEqualTo("listing-ms");
        assertThat(started.host()).isNotBlank();
        assertThat(started.startedAt()).isNotNull();
    }
}
