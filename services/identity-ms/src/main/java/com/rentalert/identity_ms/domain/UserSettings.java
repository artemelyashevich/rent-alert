package com.rentalert.identity_ms.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.rentalert.common.util.jpa.AbstractEntity;
import com.rentalert.dto.profile.BotStatus;
import com.rentalert.dto.profile.ColorTheme;
import com.rentalert.dto.profile.SupportedLanguage;
import com.rentalert.dto.profile.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_settings")
public class UserSettings extends AbstractEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false, unique = true)
    @JsonBackReference("profile-settings")
    private UserProfile userProfile;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "color_theme", nullable = false)
    @Builder.Default
    private ColorTheme colorTheme = ColorTheme.SYSTEM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupportedLanguage language = SupportedLanguage.RU;

    @Column(name = "refresh_token")
    private String refreshTokenHash;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 50)
    @Builder.Default
    private UserRole roleName = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "bot_status", nullable = false)
    @Builder.Default
    private BotStatus botStatus = BotStatus.ACTIVE;

    @Column(name = "is_email_active", nullable = false)
    @Builder.Default
    private Boolean emailActive = false;

    @Column(name = "quiet_from")
    private LocalTime quietFrom;

    @Column(name = "quiet_to")
    private LocalTime quietTo;
}
