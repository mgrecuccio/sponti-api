package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.PhoneNumberAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.shared.error.UserPreferencesNotFoundException;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserContactInfoQuery;
import com.mgrtech.sponti_api.user.api.query.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.internal.repository.UserPreferenceRepository;
import com.mgrtech.sponti_api.user.internal.repository.UserRepository;
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
    UserContactInfoQuery userContactInfoQuery;

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

    @Autowired
    UserRepository userRepository;

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
        assertThat(persistedPreferences.get().isPushNotificationsEnabled()).isTrue();
        assertThat(persistedPreferences.get().isSuggestionNotificationsEnabled()).isTrue();

        var matchingPreferences = userMatchingPreferencesQuery.getMatchingPreferences(result.id());
        assertThat(matchingPreferences).isPresent();
        assertThat(matchingPreferences.get().allowChat()).isTrue();
        assertThat(matchingPreferences.get().allowCall()).isTrue();
        assertThat(matchingPreferences.get().quietHoursStart()).isNull();
        assertThat(matchingPreferences.get().quietHoursEnd()).isNull();
        assertThat(matchingPreferences.get().pushNotificationsEnabled()).isTrue();
        assertThat(matchingPreferences.get().suggestionNotificationsEnabled()).isTrue();
    }

    @Test
    void create_user_persists_phone_number() {
        final var phoneNumber = "+32987778844";
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "test@email.com",
                        "password-hash",
                        "nickname",
                        phoneNumber,
                        "UTC"
                )
        );

        assertThat(userRepository.existsByPhoneNumber(phoneNumber)).isTrue();

        var privateProfile = userFacade.getCurrentUserProfile(created.id());
        assertThat(privateProfile.email()).isEqualTo("test@email.com");
        assertThat(privateProfile.phoneNumber()).isEqualTo(phoneNumber);

        assertThat(userContactInfoQuery.hasPhoneNumber(created.id())).isTrue();
        assertThat(userContactInfoQuery.getPhoneNumber(created.id())).contains(phoneNumber);
    }

    @Test
    void create_user_persists_when_phone_number_null() {
        var result = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "test@email.com",
                        "password-hash",
                        "nickname",
                        null,
                        "UTC"
                )
        );


        var user = userRepository.findById(result.id());
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("test@email.com");
        assertThat(user.get().getPhoneNumber()).isNull();
        assertThat(userContactInfoQuery.hasPhoneNumber(result.id())).isFalse();
        assertThat(userContactInfoQuery.getPhoneNumber(result.id())).isEmpty();
    }

    @Test
    void create_user_persists_when_empty_phone_number_is_normalized() {
        var result = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "test@email.com",
                        "password-hash",
                        "nickname",
                        "",
                        "UTC"
                )
        );

        var user = userRepository.findById(result.id());
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("test@email.com");
        assertThat(user.get().getPhoneNumber()).isNull();
    }

    @Test
    void create_user_persists_multiple_users_with_null_phone_number() {
        var result1 = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "user1@email.com",
                        "password-hash",
                        "nickname",
                        "",
                        "UTC"
                )
        );

        var result2 = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "user2@email.com",
                        "password-hash",
                        "nickname",
                        null,
                        "UTC"
                )
        );

        var user1 = userRepository.findById(result1.id());

        assertThat(user1).isPresent();
        assertThat(user1.get().getEmail()).isEqualTo("user1@email.com");
        assertThat(user1.get().getPhoneNumber()).isNull();

        var user2 = userRepository.findById(result2.id());
        assertThat(user2).isPresent();
        assertThat(user2.get().getEmail()).isEqualTo("user2@email.com");
        assertThat(user2.get().getPhoneNumber()).isNull();
    }

    @Test
    void create_user_rejects_duplicate_phone_number() {
        userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash1",
                        "John",
                        "+32455786633",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> userRegistrationFacade.createUser(
                new CreateUserCommand(" another@example.com ", "hash2", "Other", "+32455786633", "UTC")
        )).isInstanceOf(PhoneNumberAlreadyUsedException.class);
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
        assertThat(preferences.get().pushNotificationsEnabled()).isTrue();
        assertThat(preferences.get().suggestionNotificationsEnabled()).isTrue();
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
    void update_user_profile_with_phone_number() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        final var userId = created.id();

        var persistedUser = userRepository.findById(userId);
        assertThat(persistedUser.isPresent()).isTrue();
        assertThat(persistedUser.get().getPhoneNumber()).isNull();

        userFacade.updateProfile(created.id(),
                new UpdateUserCommand(
                        "new-display-name",
                        "Europe/Brussels",
                        "+32468009911")
        );

        var updated = userProfileQuery.getProfileById(created.id());

        assertThat(updated.isPresent()).isTrue();
        assertThat(updated.get().displayName()).isEqualTo("new-display-name");
        assertThat(updated.get().timezone()).isEqualTo("Europe/Brussels");


        persistedUser = userRepository.findById(updated.get().id());
        assertThat(persistedUser.isPresent()).isTrue();
        assertThat(persistedUser.get().getPhoneNumber()).isEqualTo("+32468009911");

        var privateProfile = userFacade.getCurrentUserProfile(created.id());
        assertThat(privateProfile.phoneNumber()).isEqualTo("+32468009911");
    }

    @Test
    void update_user_profile_allows_keeping_same_phone_number() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "+32468009911",
                        "UTC"
                )
        );

        userFacade.updateProfile(created.id(),
                new UpdateUserCommand(
                        "John Updated",
                        "Europe/Brussels",
                        "+32468009911")
        );

        var privateProfile = userFacade.getCurrentUserProfile(created.id());
        assertThat(privateProfile.displayName()).isEqualTo("John Updated");
        assertThat(privateProfile.timezone()).isEqualTo("Europe/Brussels");
        assertThat(privateProfile.phoneNumber()).isEqualTo("+32468009911");
    }

    @Test
    void update_user_profile_normalizes_empty_phone_number_to_null() {
        var created = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "john@example.com",
                        "hash",
                        "John",
                        "+32468009911",
                        "UTC"
                )
        );

        userFacade.updateProfile(created.id(),
                new UpdateUserCommand(
                        "John Updated",
                        "Europe/Brussels",
                        "")
        );

        var privateProfile = userFacade.getCurrentUserProfile(created.id());
        assertThat(privateProfile.phoneNumber()).isNull();
    }

    @Test
    void get_current_user_profile_fails_if_user_not_found() {
        assertThatThrownBy(() -> userFacade.getCurrentUserProfile(11L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Authenticated user not found");
    }

    @Test
    void update_user_profile_throws_conflict_error_if_phone_number_not_unique() {
        userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "user1@example.com",
                        "hash",
                        "John",
                        "+32468009911",
                        "UTC"
                )
        );

        var user2 = userRegistrationFacade.createUser(
                new CreateUserCommand(
                        "user2@example.com",
                        "hash",
                        "John",
                        "UTC"
                )
        );

        assertThatThrownBy(() -> userFacade.updateProfile(user2.id(),
                new UpdateUserCommand(
                        "new-display-name",
                        "Europe/Brussels",
                        "+32468009911")
        )).isInstanceOf(PhoneNumberAlreadyUsedException.class);
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
        assertThat(preferences.get().isPushNotificationsEnabled()).isTrue();
        assertThat(preferences.get().isSuggestionNotificationsEnabled()).isTrue();

        userPreferenceFacade.updatePreferences(
                created.id(),
                new UpdatePreferencesCommand(
                        false,
                        false,
                        LocalTime.parse("09:00:00"),
                        LocalTime.parse("11:00:00"),
                        false,
                        false
                ));

        var updatedPreferences = userPreferenceRepository.findByUserId(created.id());

        assertThat(updatedPreferences.isPresent()).isTrue();
        assertThat(updatedPreferences.get().isAllowChat()).isFalse();
        assertThat(updatedPreferences.get().isAllowCall()).isFalse();
        assertThat(updatedPreferences.get().getQuietHoursStart()).isEqualTo(LocalTime.parse("09:00:00"));
        assertThat(updatedPreferences.get().getQuietHoursEnd()).isEqualTo(LocalTime.parse("11:00:00"));
        assertThat(updatedPreferences.get().isPushNotificationsEnabled()).isFalse();
        assertThat(updatedPreferences.get().isSuggestionNotificationsEnabled()).isFalse();
    }

    @Test
    void update_user_preferences_fails_if_user_not_found() {
        assertThatThrownBy(() -> userPreferenceFacade.updatePreferences(11L,
                new UpdatePreferencesCommand(
                        true,
                        true,
                        LocalTime.parse("09:00:00"),
                        LocalTime.parse("11:00:00"),
                        true,
                        true
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
                        LocalTime.parse("11:00:00"),
                        true,
                        true
                )))
                .isInstanceOf(UserPreferencesNotFoundException.class)
                .hasMessage("No user preferences found.");
    }
}
