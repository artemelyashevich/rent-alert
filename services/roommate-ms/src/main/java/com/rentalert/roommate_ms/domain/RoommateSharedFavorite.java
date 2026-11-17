package com.rentalert.roommate_ms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@Entity
@Table(name = "roommate_shared_favorites")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RoommateSharedFavorite implements Serializable {

    @EmbeddedId
    private RoommateSharedFavoriteEmbeddedId id;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_status", nullable = false, length = 32)
    private ListingStatusName listingStatus;

    @Column(name = "viewing_date")
    private Instant viewingDate;

    @Column(name = "added_by_user_id", nullable = false)
    private Long addedByUserId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
