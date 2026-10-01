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
import com.mgrtech.sponti_api.shared.api.ChannelType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    void returns_rules_for_authenticated_user() throws Exception {
        mockMvc.perform(get("/api/v1/availability/rules")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L))
                .andExpect(jsonPath("$[0].userId").value(42L))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("12:00:00"))
                .andExpect(jsonPath("$[0].channelType").value("CHAT"))
                .andExpect(jsonPath("$[0].enabled").value(true));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
    }

    @Test
    void creates_rule_for_authenticated_user() throws Exception {
        mockMvc.perform(post("/api/v1/availability/rules")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": "TUESDAY",
                                  "startTime": "10:00:00",
                                  "endTime": "11:30:00",
                                  "channelType": "CALL"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11L))
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.endTime").value("11:30:00"))
                .andExpect(jsonPath("$.channelType").value("CALL"))
                .andExpect(jsonPath("$.enabled").value(true));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastCreateRuleCommand())
                .isEqualTo(new CreateAvailabilityRuleCommand(
                        DayOfWeek.TUESDAY,
                        LocalTime.parse("10:00:00"),
                        LocalTime.parse("11:30:00"),
                        ChannelType.CALL
                ));
    }

    @Test
    void updates_rule_for_authenticated_user() throws Exception {
        mockMvc.perform(put("/api/v1/availability/rules/99")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dayOfWeek": "WEDNESDAY",
                                  "startTime": "08:00:00",
                                  "endTime": "09:00:00",
                                  "channelType": "CHAT",
                                  "enabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99L))
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.enabled").value(false));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastRuleId()).isEqualTo(99L);
        assertThat(availabilityFacade.lastUpdateRuleCommand())
                .isEqualTo(new UpdateAvailabilityRuleCommand(
                        DayOfWeek.WEDNESDAY,
                        LocalTime.parse("08:00:00"),
                        LocalTime.parse("09:00:00"),
                        ChannelType.CHAT,
                        false
                ));
    }

    @Test
    void deletes_rule_for_authenticated_user() throws Exception {
        mockMvc.perform(delete("/api/v1/availability/rules/99")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isNoContent());

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastRuleId()).isEqualTo(99L);
        assertThat(availabilityFacade.deleteRuleCalled()).isTrue();
    }

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

    @Test
    void creates_override_for_authenticated_user() throws Exception {
        mockMvc.perform(post("/api/v1/availability/overrides")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startDateTime": "2026-04-24T10:00:00Z",
                                  "endDateTime": "2026-04-24T11:00:00Z",
                                  "type": "UNAVAILABLE"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.userId").value(42L))
                .andExpect(jsonPath("$.startDateTime").value("2026-04-24T10:00:00Z"))
                .andExpect(jsonPath("$.endDateTime").value("2026-04-24T11:00:00Z"))
                .andExpect(jsonPath("$.type").value("UNAVAILABLE"));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastCreateOverrideCommand())
                .isEqualTo(new CreateAvailabilityOverrideCommand(
                        Instant.parse("2026-04-24T10:00:00Z"),
                        Instant.parse("2026-04-24T11:00:00Z"),
                        AvailabilityOverrideType.UNAVAILABLE
                ));
    }

    @Test
    void returns_effective_availability_for_authenticated_user() throws Exception {
        mockMvc.perform(get("/api/v1/availability/effective")
                        .param("from", "2026-04-24T10:00:00Z")
                        .param("to", "2026-04-24T12:00:00Z")
                        .principal(new TestingAuthenticationToken("42", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDateTime").value("2026-04-24T10:00:00Z"))
                .andExpect(jsonPath("$[0].endDateTime").value("2026-04-24T10:30:00Z"))
                .andExpect(jsonPath("$[0].channelType").value("CHAT"));

        assertThat(availabilityFacade.lastUserId()).isEqualTo(42L);
        assertThat(availabilityFacade.lastFrom()).isEqualTo(Instant.parse("2026-04-24T10:00:00Z"));
        assertThat(availabilityFacade.lastTo()).isEqualTo(Instant.parse("2026-04-24T12:00:00Z"));
    }

    @Test
    void rejects_create_rule_when_required_fields_are_missing() throws Exception {
        mockMvc.perform(post("/api/v1/availability/rules")
                        .principal(new TestingAuthenticationToken("42", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
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
            ), java.time.Clock.systemUTC());
        }
    }

    static final class RecordingAvailabilityFacade implements AvailabilityFacade {

        private Long lastUserId;
        private Long lastRuleId;
        private Instant lastEndsAfter;
        private Instant lastFrom;
        private Instant lastTo;
        private boolean deleteRuleCalled;
        private CreateAvailabilityRuleCommand lastCreateRuleCommand;
        private UpdateAvailabilityRuleCommand lastUpdateRuleCommand;
        private CreateAvailabilityOverrideCommand lastCreateOverrideCommand;

        @Override
        public List<AvailabilityRuleView> getRules(Long userId) {
            this.lastUserId = userId;
            return List.of(new AvailabilityRuleView(
                    10L,
                    userId,
                    DayOfWeek.MONDAY,
                    LocalTime.parse("09:00:00"),
                    LocalTime.parse("12:00:00"),
                    ChannelType.CHAT,
                    true,
                    Instant.parse("2026-04-20T10:00:00Z"),
                    Instant.parse("2026-04-20T10:00:00Z")
            ));
        }

        @Override
        public AvailabilityRuleView createRule(Long userId, CreateAvailabilityRuleCommand command) {
            this.lastUserId = userId;
            this.lastCreateRuleCommand = command;
            return new AvailabilityRuleView(
                    11L,
                    userId,
                    command.dayOfWeek(),
                    command.startTime(),
                    command.endTime(),
                    command.channelType(),
                    true,
                    Instant.parse("2026-04-20T10:00:00Z"),
                    Instant.parse("2026-04-20T10:00:00Z")
            );
        }

        @Override
        public AvailabilityRuleView updateRule(Long userId, Long ruleId, UpdateAvailabilityRuleCommand command) {
            this.lastUserId = userId;
            this.lastRuleId = ruleId;
            this.lastUpdateRuleCommand = command;
            return new AvailabilityRuleView(
                    ruleId,
                    userId,
                    command.dayOfWeek(),
                    command.startTime(),
                    command.endTime(),
                    command.channelType(),
                    command.enabled(),
                    Instant.parse("2026-04-20T10:00:00Z"),
                    Instant.parse("2026-04-21T10:00:00Z")
            );
        }

        @Override
        public void deleteRule(Long userId, Long ruleId) {
            this.lastUserId = userId;
            this.lastRuleId = ruleId;
            this.deleteRuleCalled = true;
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
            this.lastUserId = userId;
            this.lastCreateOverrideCommand = command;
            return new AvailabilityOverrideView(
                    3L,
                    userId,
                    command.startDateTime(),
                    command.endDateTime(),
                    command.type(),
                    Instant.parse("2026-04-20T10:00:00Z")
            );
        }

        @Override
        public List<EffectiveAvailabilityView> getEffectiveAvailability(Long userId, Instant from, Instant to) {
            this.lastUserId = userId;
            this.lastFrom = from;
            this.lastTo = to;
            return List.of(new EffectiveAvailabilityView(
                    Instant.parse("2026-04-24T10:00:00Z"),
                    Instant.parse("2026-04-24T10:30:00Z"),
                    ChannelType.CHAT
            ));
        }

        @Override
        public List<EffectiveAvailabilityView> getChannelEffectiveAvailability(Long userId, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        Long lastUserId() {
            return lastUserId;
        }

        Long lastRuleId() {
            return lastRuleId;
        }

        Instant lastEndsAfter() {
            return lastEndsAfter;
        }

        Instant lastFrom() {
            return lastFrom;
        }

        Instant lastTo() {
            return lastTo;
        }

        boolean deleteRuleCalled() {
            return deleteRuleCalled;
        }

        CreateAvailabilityRuleCommand lastCreateRuleCommand() {
            return lastCreateRuleCommand;
        }

        UpdateAvailabilityRuleCommand lastUpdateRuleCommand() {
            return lastUpdateRuleCommand;
        }

        CreateAvailabilityOverrideCommand lastCreateOverrideCommand() {
            return lastCreateOverrideCommand;
        }
    }
}
