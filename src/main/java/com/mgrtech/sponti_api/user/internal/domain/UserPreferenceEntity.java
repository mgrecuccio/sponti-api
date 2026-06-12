package com.mgrtech.sponti_api.user.internal.domain;

import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalTime;

@Entity
@Table(name = "user_preferences")
@Getter
public class UserPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Getter
    @Column(name = "allow_chat")
    private boolean allowChat = true;

    @Getter
    @Column(name = "allow_call")
    private boolean allowCall = true;

    @Getter
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Getter
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Getter
    @Column(name = "push_notifications_enabled", nullable = false)
    private boolean pushNotificationsEnabled = true;

    @Getter
    @Column(name = "suggestion_notifications_enabled", nullable = false)
    private boolean suggestionNotificationsEnabled = true;

    protected UserPreferenceEntity() {
    }

    public UserPreferenceEntity(UserEntity user) {
        this.user = user;
    }

    public void update(
            boolean allowChat,
            boolean allowCall,
            LocalTime quietHoursStart,
            LocalTime quietHoursEnd,
            boolean pushNotificationsEnabled,
            boolean suggestionNotificationsEnabled
    ) {
        this.allowChat = allowChat;
        this.allowCall = allowCall;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.pushNotificationsEnabled = pushNotificationsEnabled;
        this.suggestionNotificationsEnabled = suggestionNotificationsEnabled;
    }

    public static UserMatchingPreferencesView toMatchingPreferencesView(UserEntity user, UserPreferenceEntity preferences) {
        return new UserMatchingPreferencesView(
                user.getId(),
                user.getTimezone(),
                preferences.isAllowChat(),
                preferences.isAllowCall(),
                preferences.getQuietHoursStart(),
                preferences.getQuietHoursEnd(),
                preferences.isPushNotificationsEnabled(),
                preferences.isSuggestionNotificationsEnabled()
        );
    }
}
