package com.mgrtech.sponti_api.user.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import com.mgrtech.sponti_api.user.internal.application.UserPreferenceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserProfileQuery userProfileQuery;

    @MockitoBean
    UserMatchingPreferencesQuery userMatchingPreferencesQuery;

    @MockitoBean
    UserFacade userFacade;

    @MockitoBean
    UserPreferenceFacade userPreferenceFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    ObjectMapper mapper = new ObjectMapper();

    @Test
    void returns_profile_for_authenticated_user() throws Exception {
        given(userProfileQuery.getProfileById(42L))
                .willReturn(Optional.of(new UserProfileView(
                        42L, "john@example.com", "John", "ACTIVE", "utc"
                )));

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void returns_not_found_when_facade_returns_empty() throws Exception {
        given(userProfileQuery.getProfileById(42L))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returns_user_profile() throws Exception {
        var request = new UserController.UpdateProfileRequest(
                "new displayName",
                "UTC",
                "+32468009911"
        );

        when(userFacade.updateProfile(42L, new UpdateUserCommand(
                request.displayName(),
                request.timezone(),
                request.phoneNumber()
        ))).thenReturn(new UserProfileView(
                        42L,
                        "email@test.com",
                        request.displayName(),
                        "ACTIVE",
                        request.timezone()
            ));

        mockMvc.perform(put("/api/v1/users/me")
                .principal(new TestingAuthenticationToken("42", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.email").value("email@test.com"))
                .andExpect(jsonPath("$.displayName").value(request.displayName()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timezone").value(request.timezone()));
    }

    @Test
    void update_returns_user_profile_even_when_phone_number_missing() throws Exception {
        var request = new UserController.UpdateProfileRequest(
                "new displayName",
                "UTC",
                ""
        );

        when(userFacade.updateProfile(42L, new UpdateUserCommand(
                request.displayName(),
                request.timezone(),
                null
        ))).thenReturn(new UserProfileView(
                42L,
                "email@test.com",
                request.displayName(),
                "ACTIVE",
                request.timezone()
        ));

        mockMvc.perform(put("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.id").value(42L))
                .andExpect(jsonPath("$.email").value("email@test.com"))
                .andExpect(jsonPath("$.displayName").value(request.displayName()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.timezone").value(request.timezone()));
    }

    @Test
    void update_returns_400_when_invalid_request() throws Exception {
        var request = new UserController.UpdateProfileRequest(
                "",
                "",
                ""
        );

        mockMvc.perform(put("/api/v1/users/me")
                .principal(new TestingAuthenticationToken("42", null))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns_5xx_for_unsupported_principal() throws Exception {
        var principal = new Object();
        var authentication = new TestingAuthenticationToken(principal, null);

        mockMvc.perform(get("/api/v1/users/me").principal(authentication))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void update_preferences_returns_preferences_view() throws Exception {
        mapper.registerModule(new JavaTimeModule());

        var request = new UserController.UpdatePreferencesRequest(
                true,
                true,
                LocalTime.parse("09:00:00"),
                LocalTime.parse("11:00:00"),
                false,
                true
        );

        when(userPreferenceFacade.updatePreferences(42L,
                new UpdatePreferencesCommand(
                        request.allowChat(),
                        request.allowCall(),
                        request.quietHoursStart(),
                        request.quietHoursEnd(),
                        request.pushNotificationsEnabled(),
                        request.suggestionNotificationsEnabled()
                )))
                .thenReturn(new UserMatchingPreferencesView(
                        42L,
                        "UTC",
                        request.allowChat(),
                        request.allowCall(),
                        request.quietHoursStart(),
                        request.quietHoursEnd(),
                        request.pushNotificationsEnabled(),
                        request.suggestionNotificationsEnabled()
                ));

        mockMvc.perform(put("/api/v1/users/preferences")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.allowChat").value(request.allowChat()))
                .andExpect(jsonPath("$.allowCall").value(request.allowCall()))
                .andExpect(jsonPath("$.quietHoursStart").value("09:00:00"))
                .andExpect(jsonPath("$.quietHoursEnd").value("11:00:00"))
                .andExpect(jsonPath("$.pushNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.suggestionNotificationsEnabled").value(true));
    }

    @Test
    void get_preferences_returns_preferences_view() throws Exception {
        given(userMatchingPreferencesQuery.getMatchingPreferences(42L))
                .willReturn(Optional.of(new UserMatchingPreferencesView(
                        42L,
                        "UTC",
                        true,
                        true,
                        null,
                        null,
                        false,
                        true
                )));

        mockMvc.perform(get("/api/v1/users/preferences")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.pushNotificationsEnabled").value(false))
                .andExpect(jsonPath("$.suggestionNotificationsEnabled").value(true));
    }

    @Test
    void update_preferences_returns_400_when_required_flags_are_missing() throws Exception {
        mapper.registerModule(new JavaTimeModule());

        var request = new UserController.UpdatePreferencesRequest(
                true,
                true,
                null,
                null,
                null,
                true
        );

        mockMvc.perform(put("/api/v1/users/preferences")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
