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
public class RoommateSharedFavoriteEmbeddedId implements Serializable {

    @Column(name = "match_id")
    private Long matchId;

    @Column(name = "listing_id")
    private Long listingId;
}
