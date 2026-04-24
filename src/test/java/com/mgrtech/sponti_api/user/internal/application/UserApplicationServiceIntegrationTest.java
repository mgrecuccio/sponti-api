package com.mgrtech.sponti_api.user.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.user.api.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.UserCredentialsQuery;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ModuleIntegrationTest
class UserApplicationServiceIntegrationTest {

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    UserQueryFacade userQueryFacade;

    @Autowired
    UserCredentialsQuery userCredentialsQuery;

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

        var profile = userQueryFacade.getProfileById(result.id());
        assertThat(profile).isPresent();
        assertThat(profile.get().email()).isEqualTo("test@email.com");
        assertThat(profile.get().displayName()).isEqualTo("nickname");
        assertThat(profile.get().timezone()).isEqualTo("UTC");
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
}