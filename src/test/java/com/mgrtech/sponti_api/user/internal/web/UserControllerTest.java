package com.mgrtech.sponti_api.user.internal.web;

import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.user.api.UserProfileView;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserQueryFacade userQueryFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @Test
    void returns_profile_for_authenticated_user() throws Exception {
        given(userQueryFacade.getProfileById(42L))
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
        given(userQueryFacade.getProfileById(42L))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/me")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns_5xx_for_unsupported_principal() throws Exception {
        var principal = new Object();
        var authentication = new TestingAuthenticationToken(principal, null);

        mockMvc.perform(get("/api/v1/users/me").principal(authentication))
                .andExpect(status().is5xxServerError());
    }
}