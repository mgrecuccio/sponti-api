package com.mgrtech.sponti_api.user.internal.domain;

import com.mgrtech.sponti_api.user.api.view.UserCredentialsView;
import com.mgrtech.sponti_api.user.api.view.UserLookupView;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "users")
@NoArgsConstructor
@Getter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", unique = true, length = 16)
    private String phoneNumber;

    @Column(name = "phone_number_verified", nullable = false)
    private boolean phoneNumberVerified;

    @Column(name = "whats_app_opt_in", nullable = false)
    private boolean whatsAppOptIn;

    @Column(name = "phone_number_verified_at")
    private Instant phoneNumberVerifiedAt;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "timezone")
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public UserEntity(
            String email,
            String passwordHash,
            String displayName,
            String phoneNumber,
            String timezone
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.phoneNumber = phoneNumber;
        this.timezone = timezone;
    }

    public String getStatusAsString() {
        return status.name();
    }

    public void update(
            String displayName,
            String timezone,
            String phoneNumber
    ) {
        this.displayName = displayName;
        this.timezone = timezone;
        this.phoneNumber = phoneNumber;
    }

    public static UserCredentialsView toCredentialsView(UserEntity user) {
        return new UserCredentialsView(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash()
        );
    }

    public static UserProfileView toProfileView(UserEntity user) {
        return new UserProfileView(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatusAsString(),
                user.getTimezone()
        );
    }

    public static UserLookupView toLookupView(UserEntity user) {
        return new UserLookupView(user.getId(), user.getEmail());
    }

    public static UserMatchingPreferencesView defaultMatchingPreferencesView(UserEntity user) {
        return new UserMatchingPreferencesView(
                user.getId(),
                user.getTimezone(),
                true,
                true,
                null,
                null,
                true,
                true
        );
    }
}
