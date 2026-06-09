package com.mgrtech.sponti_api.notification.internal.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.notification.internal.application.DeviceTokenApplicationService;
import com.mgrtech.sponti_api.notification.internal.domain.DevicePlatform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationDeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationDeviceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceTokenApplicationService service;

    @MockitoBean
    JwtTokenService jwtTokenService;

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void register_token_for_authenticated_user() throws Exception {
        var request = new NotificationDeviceController.RegisterDeviceTokenRequest(
                DevicePlatform.ANDROID,
                "token",
                "device-1",
                "1.0"
        );

        mockMvc.perform(post("/api/v1/notifications/devices")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void register_returns_400_if_request_invalid() throws Exception {
        var request = new NotificationDeviceController.RegisterDeviceTokenRequest(
                DevicePlatform.ANDROID,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/notifications/devices")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void delete_token_for_authenticated_user() throws Exception {
        var request = new NotificationDeviceController.DeleteDeviceTokenRequest("token");

        mockMvc.perform(delete("/api/v1/notifications/devices")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    public void delete_returns_400_if_request_invalid() throws Exception {
        var request = new NotificationDeviceController.DeleteDeviceTokenRequest("");

        mockMvc.perform(delete("/api/v1/notifications/devices")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

}