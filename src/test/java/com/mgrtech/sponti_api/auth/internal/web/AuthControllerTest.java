package com.mgrtech.sponti_api.auth.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.command.ChangePasswordCommand;
import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RecoverPasswordCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.command.VerifyRecoveryPasswordCommand;
import com.mgrtech.sponti_api.auth.api.command.VerifyRegistrationPhoneCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.InvalidRefreshTokenException;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    AuthFacade authFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @Test
    void registers_user_and_returns_auth_tokens() throws Exception {
        var request = new AuthController.RegisterRequest(
                "password",
                "nickname",
                "+32468009911",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone(),
                        "127.0.0.1"
                )))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void register_returns_bad_request_if_password_is_missing() throws Exception {
        var request = new AuthController.RegisterRequest(
                "",
                "nickname",
                "+32468009911",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone()
                )))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_returns_bad_request_when_phone_number_is_missing() throws Exception {
        var request = new AuthController.RegisterRequest(
                "password",
                "nickname",
                "",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone()
                )))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void register_returns_bad_request_if_phone_number_is_invalid() throws Exception {
        var request = new AuthController.RegisterRequest(
                "password",
                "nickname",
                "+3246",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone()
                )))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void logins_and_returns_auth_tokens() throws Exception {
        var request = new AuthController.LoginRequest(
                "+32468009911",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.phoneNumber(), request.password())))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void login_returns_bad_request_if_request_is_invalid() throws Exception {
        var request = new AuthController.LoginRequest(
                "+3246",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.phoneNumber(), request.password())))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "access",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returns_unauthorized_if_user_not_found() throws Exception {
        var request = new AuthController.LoginRequest(
                "+32468009911",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.phoneNumber(), request.password())))
                .willThrow(BadCredentialsException.class);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void refresh_returns_new_auth_tokens() throws Exception {
        var request = new AuthController.RefreshRequest("refreshToken");

        given(authFacade.refresh(request.refreshToken()))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void refresh_returns_bad_request_when_request_is_invalid() throws Exception {
        var request = new AuthController.RefreshRequest("");

        given(authFacade.refresh(request.refreshToken()))
                .willReturn(new AuthTokens(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        0
                ));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_returns_unauthorized_when_refresh_token_is_invalid() throws Exception {
        var request = new AuthController.RefreshRequest("invalid-refresh-token");

        given(authFacade.refresh(request.refreshToken()))
                .willThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.detail").value("Invalid refresh token"));
    }

    @Test
    void logout_revokes_tokens_for_authenticated_user() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());

        verify(authFacade).logout(42L);
    }

    @Test
    void change_password_updates_password_for_authenticated_user() throws Exception {
        var request = new AuthController.ChangePasswordRequest(
                "current-password",
                "new-password"
        );

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authFacade).changePassword(new ChangePasswordCommand(
                42L,
                request.currentPassword(),
                request.newPassword()
        ));
    }

    @Test
    void change_password_returns_bad_request_if_request_is_invalid() throws Exception {
        var request = new AuthController.ChangePasswordRequest(
                "",
                "new-password"
        );

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void change_password_returns_unauthorized_if_current_password_is_wrong() throws Exception {
        var request = new AuthController.ChangePasswordRequest(
                "wrong-password",
                "new-password"
        );

        willThrow(new BadCredentialsException("Bad credentials"))
                .given(authFacade)
                .changePassword(new ChangePasswordCommand(
                        42L,
                        request.currentPassword(),
                        request.newPassword()
                ));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void recover_password_sends_recovery_otp() throws Exception {
        var request = new AuthController.PasswordRecoveryRequest("+32468009911");

        given(authFacade.recoverPassword(new RecoverPasswordCommand(
                request.phoneNumber(),
                "203.0.113.10"
        ))).willReturn(new VerificationView("verification-id"));

        mockMvc.perform(post("/api/v1/auth/password-recovery")
                        .header("X-Forwarded-For", "203.0.113.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationId").value("verification-id"));

        verify(authFacade).recoverPassword(new RecoverPasswordCommand(
                request.phoneNumber(),
                "203.0.113.10"
        ));
    }

    @Test
    void recover_password_returns_bad_request_if_request_is_invalid() throws Exception {
        var request = new AuthController.PasswordRecoveryRequest("+3246");

        mockMvc.perform(post("/api/v1/auth/password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void verify_password_recovery_resets_password() throws Exception {
        var request = new AuthController.PasswordRecoveryVerificationRequest(
                "+32468009911",
                "verification-id",
                "123456",
                "new-password"
        );

        mockMvc.perform(post("/api/v1/auth/verify-password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(authFacade).verifyPasswordRecovery(new VerifyRecoveryPasswordCommand(
                request.phoneNumber(),
                request.verificationId(),
                request.otpCode(),
                request.newPassword()
        ));
    }

    @Test
    void verify_password_recovery_returns_bad_request_if_request_is_invalid() throws Exception {
        var request = new AuthController.PasswordRecoveryVerificationRequest(
                "+32468009911",
                "verification-id",
                "",
                "new-password"
        );

        mockMvc.perform(post("/api/v1/auth/verify-password-recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void verify_registration_phone_delegates_for_authenticated_user() throws Exception {
        var request = new AuthController.VerifyPhoneRequest(
                "123456",
                "verification-id"
        );

        given(authFacade.verifyRegistrationPhone(new VerifyRegistrationPhoneCommand(
                42L,
                request.verificationId(),
                request.otpCode()
        ))).willReturn(new VerificationResultView(42L, true));

        mockMvc.perform(post("/api/v1/auth/verify-registration-phone")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        verify(authFacade).verifyRegistrationPhone(new VerifyRegistrationPhoneCommand(
                42L,
                request.verificationId(),
                request.otpCode()
        ));
    }

}
