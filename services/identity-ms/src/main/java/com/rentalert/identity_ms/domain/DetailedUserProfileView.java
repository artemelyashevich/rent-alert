package com.rentalert.identity_ms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.Immutable;

@Getter
@Setter
@Entity
@Immutable
@NoArgsConstructor
@FieldNameConstants
@Table(name = "detailed_user_profiles")
public class DetailedUserProfileView {

    @Id
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "bot_status")
    private BotStatus botStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name")
    private UserRole roleName;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "quiet_from")
    private LocalTime quietFrom;

    @Column(name = "quiet_to")
    private LocalTime quietTo;

    @Column(name = "email")
    private String email;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "active_filters")
    private String activeFiltersJson;

    @Column(name = "recent_feedbacks")
    private String recentFeedbacksJson;
}
