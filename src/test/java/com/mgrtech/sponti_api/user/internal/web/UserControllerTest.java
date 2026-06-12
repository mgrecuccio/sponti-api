package com.mgrtech.sponti_api.user.internal.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    UserFacade userFacade;

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
                "UTC"
        );

        when(userFacade.update(42L, new UpdateUserCommand(request.displayName(), request.timezone())))
                .thenReturn(new UserProfileView(
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
}