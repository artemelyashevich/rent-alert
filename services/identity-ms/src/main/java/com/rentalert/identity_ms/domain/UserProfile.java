package com.rentalert.identity_ms.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.rentalert.common.util.jpa.AbstractEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;

@FieldNameConstants
@Getter
@Setter
@Entity
@Table(name = "user_profiles")
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile extends AbstractEntity {

    @Column(name = "telegram_id", nullable = false)
    private String telegramId;

    @Column(name = "telegram_id_hash", nullable = false, unique = true)
    private String telegramIdHash;

    @Column(name = "username", unique = true)
    private String username;

    @JsonManagedReference("profile-settings")
    @OneToOne(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true,
        optional = false, fetch = FetchType.EAGER)
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private UserSettings settings;

    @JsonManagedReference("profile-filters")
    @OneToMany(mappedBy = "userProfile", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserFilter> filters = new ArrayList<>();

    @Column(name = "photo_url")
    private String photoUrl;

    @CreatedDate
    @Column(name = "last_login")
    private Instant lastLogin;

    /**
     * Settings / credentials split out into {@link UserSettings}. Lazily instantiated so freshly built profiles and mappers never hit an NPE; the back reference is always kept in sync.
     */
    public UserSettings getSettings() {
        if (settings == null) {
            settings = UserSettings.builder().build();
            settings.setUserProfile(this);
        }
        return settings;
    }

    public void setSettings(UserSettings settings) {
        this.settings = settings;
        if (settings != null) {
            settings.setUserProfile(this);
        }
    }
}
