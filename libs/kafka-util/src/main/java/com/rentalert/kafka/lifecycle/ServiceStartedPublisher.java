package com.rentalert.kafka.lifecycle;

import com.rentalert.dto.event.KafkaTopics;
import com.rentalert.dto.event.ServiceStartedEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Reports that this service has started, so notification-ms can tell the admin once every service is up.
 * <p>
 * Sent directly, not through the outbox: there is no business transaction to tie it to, and a lost report only
 * means a missing "application started" message.
 */
@Slf4j
@RequiredArgsConstructor
public class ServiceStartedPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String serviceName;

    /**
     * Publishes {@link ServiceStartedEvent} once the application is ready to serve requests.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void publish() {
        ServiceStartedEvent event = new ServiceStartedEvent(serviceName, hostName(), Instant.now());
        kafkaTemplate.send(KafkaTopics.SERVICE_STARTED, serviceName, event)
            .whenComplete((result, e) -> {
                if (e != null) {
                    log.warn("Failed to report start of {}: {}", serviceName, e.getMessage());
                }
            });
    }

    private static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
