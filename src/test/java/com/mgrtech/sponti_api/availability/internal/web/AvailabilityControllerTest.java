package com.mgrtech.sponti_api.availability.internal.web;

import com.mgrtech.sponti_api.auth.internal.security.JwtProperties;
import com.mgrtech.sponti_api.auth.internal.security.JwtTokenService;
import com.mgrtech.sponti_api.availability.internal.application.AvailabilityFacade;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityOverrideCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.UpdateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityOverrideView;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityRuleView;
import com.mgrtech.sponti_api.availability.api.view.EffectiveAvailabilityView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
@Import(AvailabilityControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class AvailabilityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RecordingAvailabilityFacade availabilityFacade;

    @Test
    void returns_overrides_without_filter_when_ends_after_is_absent() throws Exception {
        mockMvc.perform(get("/api/v1/availability/overrides")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(42L))
                .andExpect(jsonPath("$[0].type").value("AVAILABLE"));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastEndsAfter()).isNull();
    }

    @Test
    void forwards_ends_after_filter_to_facade() throws Exception {
        var endsAfter = Instant.parse("2026-04-24T00:00:00Z");

        mockMvc.perform(get("/api/v1/availability/overrides")
                        .param("endsAfter", "2026-04-24T00:00:00Z")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].endDateTime").value("2026-04-25T10:00:00Z"));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastEndsAfter()).isEqualTo(endsAfter);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        RecordingAvailabilityFacade availabilityFacade() {
            return new RecordingAvailabilityFacade();
        }

        @Bean
        JwtTokenService jwtTokenService() {
            return new JwtTokenService(new JwtProperties(
                    "12345678901234567890123456789012",
                    "test",
                    15,
                    7
            ));
        }
    }

    static final class RecordingAvailabilityFacade implements AvailabilityFacade {

        private Long lastUserId;
        private Instant lastEndsAfter;

        @Override
        public List<AvailabilityRuleView> getRules(Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AvailabilityRuleView createRule(Long userId, CreateAvailabilityRuleCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public AvailabilityRuleView updateRule(Long userId, Long ruleId, UpdateAvailabilityRuleCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteRule(Long userId, Long ruleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<AvailabilityOverrideView> getOverrides(Long userId, Instant endsAfter) {
            this.lastUserId = userId;
            this.lastEndsAfter = endsAfter;

            if (endsAfter == null) {
                return List.of(new AvailabilityOverrideView(
                        1L,
                        userId,
                        Instant.parse("2026-04-24T10:00:00Z"),
                        Instant.parse("2026-04-24T11:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE,
                        Instant.parse("2026-04-20T10:00:00Z")
                ));
            }

            return List.of(new AvailabilityOverrideView(
                    2L,
                    userId,
                    Instant.parse("2026-04-25T09:00:00Z"),
                    Instant.parse("2026-04-25T10:00:00Z"),
                    AvailabilityOverrideType.UNAVAILABLE,
                    Instant.parse("2026-04-20T10:00:00Z")
            ));
        }

        @Override
        public AvailabilityOverrideView createOverride(Long userId, CreateAvailabilityOverrideCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EffectiveAvailabilityView> getEffectiveAvailability(Long userId, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EffectiveAvailabilityView> getChannelEffectiveAvailability(Long userId, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        Long lastUserId() {
            return lastUserId;
        }

        Instant lastEndsAfter() {
            return lastEndsAfter;
        }
    }
}
