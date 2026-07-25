package com.mgrtech.sponti_api.auth.internal.security;

import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import com.mgrtech.sponti_api.user.internal.application.UserPreferenceFacade;
import com.mgrtech.sponti_api.user.internal.web.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "app.cors.allowed-origin=https://localhost, http://localhost, capacitor://localhost, http://localhost:8100")
class SecurityErrorResponseTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    UserProfileQuery userProfileQuery;

    @MockitoBean
    UserFacade userFacade;

    @MockitoBean
    UserPreferenceFacade userPreferenceFacade;

    @MockitoBean
    UserMatchingPreferencesQuery userMatchingPreferencesQuery;

    @Test
    void unauthenticated_request_returns_stable_error_code() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(jsonPath("$.detail").value("Authentication required"));
    }

    @Test
    void cors_preflight_allows_configured_android_capacitor_8_origin() throws Exception {
        mockMvc.perform(options("https://api.sponti.uk/api/v1/users/me")
                        .header(ORIGIN, "https://localhost")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "https://localhost"));
    }

    @Test
    void cors_preflight_allows_configured_older_android_capacitor_origin() throws Exception {
        mockMvc.perform(options("https://api.sponti.uk/api/v1/users/me")
                        .header(ORIGIN, "http://localhost")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost"));
    }

    @Test
    void cors_preflight_allows_configured_ionic_dev_server_origin() throws Exception {
        mockMvc.perform(options("https://api.sponti.uk/api/v1/users/me")
                        .header(ORIGIN, "http://localhost:8100")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8100"));
    }

    @Test
    void cors_preflight_allows_configured_ios_capacitor_origin() throws Exception {
        mockMvc.perform(options("https://api.sponti.uk/api/v1/users/me")
                        .header(ORIGIN, "capacitor://localhost")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "capacitor://localhost"));
    }
}
