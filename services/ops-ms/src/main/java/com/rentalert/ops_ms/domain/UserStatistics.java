package com.rentalert.ops_ms.domain;

import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.event.user.UserEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "user_statistics")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics extends AbstractEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "filter_id")
    private Long filterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private UserEvent eventType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "count", nullable = false)
    private int count;
}
