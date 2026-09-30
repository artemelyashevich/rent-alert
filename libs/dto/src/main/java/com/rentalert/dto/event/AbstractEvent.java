package com.rentalert.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonIgnoreProperties(value = "eventType", allowGetters = true)
public abstract class AbstractEvent implements Serializable {

    public EventStatus eventStatus;
    public Long initiatorId;
    private UUID idempotencyKey = UUID.randomUUID();

    public abstract EventInitiatorType getEventType();
}
