package com.rentalert.roommate_ms.domain;

import com.rentalert.dto.currency.Currency;
import com.rentalert.dto.roommate.ModerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Builder
@Entity
@Table(name = "roommate_profiles")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RoommateProfile implements Serializable {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 16)
    private Gender gender;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 4)
    @Column(name = "gender_pref", columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private String[] genderPref = new String[0];

    @Column(name = "birth_year")
    private Short birthYear;

    @Column(name = "budget_min")
    private Integer budgetMin;

    @Column(name = "budget_max")
    private Integer budgetMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 8)
    private Currency currency;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "move_in_from")
    private LocalDate moveInFrom;

    @Column(name = "move_in_to")
    private LocalDate moveInTo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Array(length = 50)
    @Column(name = "lifestyle_tags", columnDefinition = "varchar[]", nullable = false)
    @Builder.Default
    private String[] lifestyleTags = new String[0];

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule", length = 16)
    private LivingSchedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "smoking", length = 16)
    private SmokingPolicy smoking;

    @Enumerated(EnumType.STRING)
    @Column(name = "pets", length = 16)
    private PetsPolicy pets;

    @Enumerated(EnumType.STRING)
    @Column(name = "guests", length = 16)
    private GuestsPolicy guests;

    @Column(name = "cleanliness")
    private Short cleanliness;

    @Column(name = "bio_submitted", columnDefinition = "TEXT")
    private String bioSubmitted;

    @Column(name = "bio_approved", columnDefinition = "TEXT")
    private String bioApproved;

    @Enumerated(EnumType.STRING)
    @Column(name = "bio_mod_status", nullable = false, length = 20)
    @Builder.Default
    private ModerationStatus bioModStatus = ModerationStatus.PENDING;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_searchable", nullable = false)
    @Builder.Default
    private boolean searchable = true;

    @Column(name = "profile_version", nullable = false)
    @Builder.Default
    private Integer profileVersion = 1;

    @Column(name = "reports_count", nullable = false)
    @Builder.Default
    private Integer reportsCount = 0;

    @Column(name = "last_digest_at")
    private Instant lastDigestAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
