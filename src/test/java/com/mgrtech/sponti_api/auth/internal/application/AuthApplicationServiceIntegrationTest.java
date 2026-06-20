package com.mgrtech.sponti_api.auth.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FullIntegrationTest;
import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.LoginCommand;
import com.mgrtech.sponti_api.auth.api.RegisterCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.EmailAlreadyUsedException;
import com.mgrtech.sponti_api.shared.error.InvalidRefreshTokenException;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FullIntegrationTest
class AuthApplicationServiceIntegrationTest {

    @Autowired
    AuthFacade authFacade;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @Autowired
    JwtTokenService jwtTokenService;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void register_creates_user_and_returns_auth_tokens() {
        var result = authFacade.register(
                getRegistrationCommand()
        );

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isPositive();
    }

    @Test
    void register_rejects_duplicate_email_after_normalization() {
        authFacade.register(
                getRegistrationCommand()
        );

        assertThatThrownBy(() -> authFacade.register(
                new RegisterCommand("  John@Example.com  ", "password2", "Johnny", "", "UTC")
        )).isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    void login_returns_auth_tokens_for_valid_credentials() {
        authFacade.register(
                getRegistrationCommand()
        );

        var result = authFacade.login(
                new LoginCommand("john@example.com", "password")
        );

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresInSeconds()).isPositive();
    }

    @Test
    void login_normalizes_email() {
        authFacade.register(
                getRegistrationCommand()
        );

        var result = authFacade.login(
                new LoginCommand("  JOHN@EXAMPLE.COM  ", "password")
        );

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotBlank();
    }

    @Test
    void login_throws_bad_credentials_when_user_does_not_exist() {
        assertThatThrownBy(() -> authFacade.login(
                new LoginCommand("missing@example.com", "password")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throws_bad_credentials_when_password_is_wrong() {
        authFacade.register(
                getRegistrationCommand()
        );

        assertThatThrownBy(() -> authFacade.login(
                new LoginCommand("john@example.com", "wrong-password")
        )).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_rotates_refresh_token_and_returns_new_access_token() {
        var registered = authFacade.register(
                getRegistrationCommand()
        );

        var refreshed = authFacade.refresh(registered.refreshToken());

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isNotBlank();
        assertThat(refreshed.tokenType()).isEqualTo("Bearer");
        assertThat(refreshed.expiresInSeconds()).isPositive();

        assertThat(refreshed.refreshToken()).isNotEqualTo(registered.refreshToken());
    }

    @Test
    void logout_revokes_all_user_refresh_tokens() {
        var registered = authFacade.register(
                getRegistrationCommand()
        );
        var loggedIn = authFacade.login(
                new LoginCommand("john@example.com", "password")
        );
        var userId = jwtTokenService.extractUserId(registered.accessToken());

        authFacade.logout(userId);

        assertThatThrownBy(() -> authFacade.refresh(registered.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authFacade.refresh(loggedIn.refreshToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    private static @NonNull RegisterCommand getRegistrationCommand() {
        return new RegisterCommand("john@example.com", "password", "John", "", "UTC");
    }
}
