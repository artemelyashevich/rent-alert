package com.rentalert.listing_ms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HiddenListingEmbeddedId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "listing_id")
    private Long listingId;
}
