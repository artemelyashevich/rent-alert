package com.rentalert.dto.event;

import java.time.Instant;

/**
 * A service instance finished starting; sent to {@link KafkaTopics#SERVICE_STARTED} with key = service name.
 */
public record ServiceStartedEvent(String service, String host, Instant startedAt) {

}
