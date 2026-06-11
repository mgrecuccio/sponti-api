package com.mgrtech.sponti_api.auth.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.LoginCommand;
import com.mgrtech.sponti_api.auth.api.RegisterCommand;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.shared.error.BadCredentialsException;
import com.mgrtech.sponti_api.shared.error.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
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
                "john@example.com",
                "password",
                "nickname",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.email(),
                        request.password(),
                        request.displayName(),
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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void register_returns_bad_request_if_request_is_invalid() throws Exception {
        var request = new AuthController.RegisterRequest(
                "invalid-email",
                "password",
                "nickname",
                "UTC"
        );

        given(authFacade.register(
                new RegisterCommand(
                        request.email(),
                        request.password(),
                        request.displayName(),
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
                "john@example.com",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.email(), request.password())))
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
                "invalid-email",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.email(), request.password())))
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
                "john@example.com",
                "password"
        );

        given(authFacade.login(new LoginCommand(request.email(), request.password())))
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

}
