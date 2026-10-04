package com.mgrtech.sponti_api.user.internal.domain;

import com.mgrtech.sponti_api.user.api.view.*;
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

    @Column(name = "phone_number", unique = true, length = 16)
    private String phoneNumber;

    @Column(name = "phone_number_verified", nullable = false)
    private boolean phoneNumberVerified;

    @Column(name = "whats_app_opt_in", nullable = false)
    private boolean whatsAppOptIn;

    @Column(name = "phone_number_verified_at")
    private Instant phoneNumberVerifiedAt;

    @Column(name = "phone_number_changed_at")
    private Instant phoneNumberChangedAt;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "timezone")
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public UserEntity(
            String passwordHash,
            String displayName,
            String phoneNumber,
            String timezone
    ) {
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

    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void markPhoneNumberChanged(Instant changedAt) {
        this.phoneNumberChangedAt = changedAt;
    }

    public void verify(Instant verifiedAt) {
        this.phoneNumberVerified = true;
        this.whatsAppOptIn = true;
        this.phoneNumberVerifiedAt = verifiedAt;
    }

    public void resetVerification() {
        this.phoneNumberVerified = false;
        this.whatsAppOptIn = false;
        this.phoneNumberVerifiedAt = null;
    }

    public void requestDeletion() {
        if (status == UserStatus.DELETED || status == UserStatus.DELETION_REQUESTED || status == UserStatus.DELETING) {
            return;
        }
        this.status = UserStatus.DELETION_REQUESTED;
    }

    public static UserCredentialsView toCredentialsView(UserEntity user) {
        return new UserCredentialsView(
                user.getId(),
                user.getStatusAsString(),
                user.getPhoneNumber(),
                user.getPasswordHash()
        );
    }

    public void anonymizeForDeletion(Instant deletedAt) {
        this.phoneNumber = null;
        this.displayName = "Deleted user";
        this.passwordHash = "";
        this.timezone = null;
        this.phoneNumberVerified = false;
        this.whatsAppOptIn = false;
        this.phoneNumberVerifiedAt = null;
        this.phoneNumberChangedAt = null;
        this.status = UserStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public static UserProfileView toProfileView(UserEntity user) {
        return new UserProfileView(
                user.getId(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getStatusAsString(),
                user.getTimezone()
        );
    }

    public static UserPrivateProfileView toPrivateProfileView(UserEntity user) {
        return new UserPrivateProfileView(
                user.getId(),
                user.getPhoneNumber(),
                user.getDisplayName(),
                user.getStatusAsString(),
                user.getTimezone()
        );
    }

    public static UserLookupView toLookupView(UserEntity user) {
        return new UserLookupView(user.getId(), user.getPhoneNumber());
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

    public boolean isDeleted() {
        return UserStatus.DELETED == status;
    }

    public boolean isDeletionRequested() {
        return UserStatus.DELETION_REQUESTED == status;
    }
}
