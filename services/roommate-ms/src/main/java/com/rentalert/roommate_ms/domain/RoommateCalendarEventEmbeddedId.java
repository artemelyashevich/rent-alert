package com.rentalert.roommate_ms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoommateCalendarEventEmbeddedId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "viewing_id")
    private Long viewingId;
}
