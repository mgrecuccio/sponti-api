package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.error.UserPreferencesNotFoundException;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ModuleIntegrationTest
class UserApplicationServiceIntegrationTest {

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    UserProfileQuery userProfileQuery;

    @Autowired
    UserMatchingPreferencesQuery userMatchingPreferencesQuery;

    @Autowired
    UserCredentialsQuery userCredentialsQuery;

    @Autowired
    UserPreferenceRepository userPreferenceRepository;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    UserFacade userFacade;

    @Autowired
    UserPreferenceFacade userPreferenceFacade;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void create_user_persists_and_returns_view() {
        var result = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "test@email.com",
                        "password-hash",
                        "nickname",
                        "UTC"
                )
        );

        assertThat(result.email()).isEqualTo("test@email.com");

        var profile = userProfileQuery.getProfileById(result.id());
        assertThat(profile).isPresent();
        assertThat(profile.get().email()).isEqualTo("test@email.com");
        assertThat(profile.get().displayName()).isEqualTo("nickname");
        assertThat(profile.get().timezone()).isEqualTo("UTC");

        var persistedPreferences = userPreferenceRepository.findByUserId(result.id());
        assertThat(persistedPreferences).isPresent();
        assertThat(persistedPreferences.get().isAllowChat()).isTrue();
        assertThat(persistedPreferences.get().isAllowCall()).isTrue();
        assertThat(persistedPreferences.get().getQuietHoursStart()).isNull();
        assertThat(persistedPreferences.get().getQuietHoursEnd()).isNull();

        var matchingPreferences = userMatchingPreferencesQuery.getMatchingPreferences(result.id());
        assertThat(matchingPreferences).isPresent();
        assertThat(matchingPreferences.get().allowChat()).isTrue();
        assertThat(matchingPreferences.get().allowCall()).isTrue();
        assertThat(matchingPreferences.get().quietHoursStart()).isNull();
        assertThat(matchingPreferences.get().quietHoursEnd()).isNull();
    }

    @Test
    void create_user_rejects_duplicate_email_after_normalization() {
        userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash1",
                        "John",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> userRegistrationFacade.createUser(
                new CreateUserCommand(" John@Example.com ", "hash2", "Johnny", "UTC")
        )).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void find_by_email_normalizes_input() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        var result = userCredentialsQuery.findByEmail("  JOHN@EXAMPLE.COM ");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(created.id());
    }

    @Test
    void get_default_matching_user_preferences() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        var preferences = userMatchingPreferencesQuery.getMatchingPreferences(created.id());

        assertThat(preferences.isPresent()).isTrue();
        assertThat(preferences.get().userId()).isEqualTo(created.id());
        assertThat(preferences.get().allowCall()).isTrue();
        assertThat(preferences.get().allowChat()).isTrue();
        assertThat(preferences.get().matchingEnabled()).isTrue();
        assertThat(preferences.get().quietHoursStart()).isNull();
        assertThat(preferences.get().quietHoursEnd()).isNull();
    }

    @Test
    void update_user_profile() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        userFacade.updateProfile(created.id(),
                new UpdateUserCommand("new-display-name", "Europe/Brussels"));

        var updated = userProfileQuery.getProfileById(created.id());

        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.get().displayName()).isEqualTo("new-display-name");
        assertThat(updated.get().timezone()).isEqualTo("Europe/Brussels");
    }

    @Test
    void update_user_profile_fails_if_user_not_found() {
        assertThatThrownBy(() -> userFacade.updateProfile(11L,
                new UpdateUserCommand("new-display-name", "Europe/Brussels")))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Impossible to update the profile: user not found.");
    }

    @Test
    void update_user_preferences() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        var preferences = userPreferenceRepository.findByUserId(created.id());
        assertThat(preferences.isPresent()).isTrue();
        assertThat(preferences.get().isAllowChat()).isTrue();
        assertThat(preferences.get().isAllowCall()).isTrue();
        assertThat(preferences.get().getQuietHoursStart()).isNull();
        assertThat(preferences.get().getQuietHoursEnd()).isNull();

        userPreferenceFacade.updatePreferences(
                created.id(),
                new UpdatePreferencesCommand(
                        false,
                        false,
                        LocalTime.parse("09:00:00"),
                        LocalTime.parse("11:00:00")
                ));

        var updatedPreferences = userPreferenceRepository.findByUserId(created.id());

        assertThat(updatedPreferences.isPresent()).isTrue();
        assertThat(updatedPreferences.get().isAllowChat()).isFalse();
        assertThat(updatedPreferences.get().isAllowCall()).isFalse();
        assertThat(updatedPreferences.get().getQuietHoursStart()).isEqualTo(LocalTime.parse("09:00:00"));
        assertThat(updatedPreferences.get().getQuietHoursEnd()).isEqualTo(LocalTime.parse("11:00:00"));
    }

    @Test
    void update_user_preferences_fails_if_user_not_found() {
        assertThatThrownBy(() -> userPreferenceFacade.updatePreferences(11L,
                new UpdatePreferencesCommand(
                        true,
                        true,
                        LocalTime.parse("09:00:00"),
                        LocalTime.parse("11:00:00")
                )))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Impossible to update the preferences: user not found.");
    }

    @Test
    void update_user_preferences_fails_if_preferences_not_found() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );
        userPreferenceRepository.deleteAll();

        assertThatThrownBy(() -> userPreferenceFacade.updatePreferences(
                created.id(),
                new UpdatePreferencesCommand(
                        true,
                        true,
                        LocalTime.parse("09:00:00"),
                        LocalTime.parse("11:00:00")
                )))
                .isInstanceOf(UserPreferencesNotFoundException.class)
                .hasMessage("No user preferences found.");
    }
}
