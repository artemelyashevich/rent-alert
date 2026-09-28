package com.rentalert.ops_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.event.EventInitiatorType;
import com.rentalert.dto.event.EventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends AbstractEntity {

    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "event_description", nullable = false)
    private String eventDescription;

    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    @Column(name = "event_initiator", nullable = false)
    private EventInitiatorType eventInitiator = EventInitiatorType.USER;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    @Column(name = "event_status", nullable = false)
    private EventStatus eventStatus = EventStatus.SUCCESS;
}
