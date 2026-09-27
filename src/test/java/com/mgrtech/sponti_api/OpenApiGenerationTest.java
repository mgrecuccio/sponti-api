package com.mgrtech.sponti_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.availability.internal.application.AvailabilityFacade;
import com.mgrtech.sponti_api.contact.internal.application.ContactFacade;
import com.mgrtech.sponti_api.matching.internal.application.MatchingFacade;
import com.mgrtech.sponti_api.notification.internal.application.DeviceTokenApplicationService;
import com.mgrtech.sponti_api.shared.openapi.OpenApiConfig;
import com.mgrtech.sponti_api.sms.api.OtpFacade;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import com.mgrtech.sponti_api.user.internal.application.UserPreferenceFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@Import(OpenApiConfig.class)
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocConfigProperties.class,
        SpringDocSpecPropertiesConfiguration.class,
        SpringDocWebMvcConfiguration.class
})
@TestPropertySource(properties = {
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true",
        "app.cors.allowed-origin=http://localhost:3000"
})
class OpenApiGenerationTest {

    private static final Path OPENAPI_OUTPUT = Path.of("openapi.json");

    @Autowired
    MockMvc mockMvc;

    ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    AuthFacade authFacade;

    @MockitoBean
    JwtTokenService jwtTokenService;

    @MockitoBean
    UserFacade userFacade;

    @MockitoBean
    UserPreferenceFacade userPreferenceFacade;

    @MockitoBean
    UserMatchingPreferencesQuery userMatchingPreferencesQuery;

    @MockitoBean
    AvailabilityFacade availabilityFacade;

    @MockitoBean
    ContactFacade contactFacade;

    @MockitoBean
    MatchingFacade matchingFacade;

    @MockitoBean
    DeviceTokenApplicationService deviceTokenApplicationService;

    @MockitoBean
    OtpFacade otpFacade;

    @Test
    void generatesOpenApiContract() throws Exception {
        var response = mockMvc.perform(get("/v3/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        var openApi = objectMapper.readTree(response);
        assertThat(openApi.at("/paths/~1api~1v1~1auth~1change-password").isMissingNode()).isFalse();

        var prettyPrinter = objectMapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        Files.writeString(
                OPENAPI_OUTPUT,
                prettyPrinter.writeValueAsString(openApi) + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
    }
}
