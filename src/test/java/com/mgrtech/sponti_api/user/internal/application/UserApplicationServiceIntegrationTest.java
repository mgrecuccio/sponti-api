package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.*;

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
    void get__default_matching_user_preferences() {
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
}
