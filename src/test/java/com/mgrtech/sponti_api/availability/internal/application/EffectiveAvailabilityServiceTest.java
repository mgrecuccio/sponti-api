package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityChannelType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideEntity;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityRuleEntity;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
import com.mgrtech.sponti_api.user.api.UserProfileView;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class EffectiveAvailabilityServiceTest {

    private AvailabilityRuleRepository ruleRepository;
    private AvailabilityOverrideRepository overrideRepository;
    private UserQueryFacade userQueryFacade;

    private EffectiveAvailabilityService service;

    @BeforeEach
    void setUp() {
        ruleRepository = Mockito.mock(AvailabilityRuleRepository.class);
        overrideRepository = Mockito.mock(AvailabilityOverrideRepository.class);
        userQueryFacade = Mockito.mock(UserQueryFacade.class);

        service = new EffectiveAvailabilityService(
                ruleRepository,
                overrideRepository,
                userQueryFacade
        );
    }

    @Test
    void should_return_rule_window_when_no_overrides() {
        Long userId = 1L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User","ACTIVE", "UTC")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of(
                        new AvailabilityRuleEntity(
                                userId,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(12, 0),
                                AvailabilityChannelType.CHAT
                        )
                ));

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of());

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        assertThat(result).containsExactly(
                new TimeWindow(
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T12:00:00Z")
                )
        );
    }

    @Test
    void should_subtract_unavailable_override_from_rule_window() {
        Long userId = 1L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User", "ACTIVE", "UTC")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of(
                        new AvailabilityRuleEntity(
                                userId,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(12, 0),
                                AvailabilityChannelType.CHAT
                        )
                ));

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of(
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T10:00:00Z"),
                        Instant.parse("2026-03-30T11:00:00Z"),
                        AvailabilityOverrideType.UNAVAILABLE
                )
        ));

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        assertThat(result).containsExactly(
                new TimeWindow(
                        Instant.parse("2026-03-30T09:00:00Z"),
                        Instant.parse("2026-03-30T10:00:00Z")
                ),
                new TimeWindow(
                        Instant.parse("2026-03-30T11:00:00Z"),
                        Instant.parse("2026-03-30T12:00:00Z")
                )
        );
    }

    @Test
    void should_add_available_override_even_without_rule() {
        Long userId = 1L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User", "ACTIVE", "UTC")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of());

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of(
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T14:00:00Z"),
                        Instant.parse("2026-03-30T16:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                )
        ));

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        assertThat(result).containsExactly(
                new TimeWindow(
                        Instant.parse("2026-03-30T14:00:00Z"),
                        Instant.parse("2026-03-30T16:00:00Z")
                )
        );
    }

    @Test
    void should_merge_overlapping_available_overrides() {
        Long userId = 1L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User", "ACTIVE", "UTC")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of());

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of(
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T14:00:00Z"),
                        Instant.parse("2026-03-30T15:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                ),
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T14:30:00Z"),
                        Instant.parse("2026-03-30T16:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                )
        ));

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        assertThat(result).containsExactly(
                new TimeWindow(
                        Instant.parse("2026-03-30T14:00:00Z"),
                        Instant.parse("2026-03-30T16:00:00Z")
                )
        );
    }

    @Test
    void should_apply_timezone_when_building_recurring_windows() {
        Long userId = 1L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User", "ACTIVE","Europe/Paris")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of(
                        new AvailabilityRuleEntity(
                                userId,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(11, 0),
                                AvailabilityChannelType.CHAT
                        )
                ));

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of());

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        assertThat(result).containsExactly(
                new TimeWindow(
                        Instant.parse("2026-03-30T07:00:00Z"),
                        Instant.parse("2026-03-30T09:00:00Z")
                )
        );
    }

    @Test
    void compute_shouldApplyUnavailableOverAvailable_forOverlappingOverrides() throws Exception {
        // given
        Long userId = 42L;

        when(userQueryFacade.getProfileById(userId))
                .thenReturn(Optional.of(new UserProfileView(userId, "user@example.com", "User", "ACTIVE","UTC")));

        when(ruleRepository.findByUserIdAndEnabledTrue(userId))
                .thenReturn(List.of(
                        new AvailabilityRuleEntity(
                                userId,
                                DayOfWeek.MONDAY,
                                LocalTime.of(9, 0),
                                LocalTime.of(12, 0),
                                AvailabilityChannelType.CHAT
                        )
                ));

        when(overrideRepository.findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
                userId,
                Instant.parse("2026-03-30T23:00:00Z"),
                Instant.parse("2026-03-30T00:00:00Z")
        )).thenReturn(List.of(
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T16:00:00Z"),
                        Instant.parse("2026-03-30T18:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                ),
                new AvailabilityOverrideEntity(
                        userId,
                        Instant.parse("2026-03-30T15:00:00Z"),
                        Instant.parse("2026-03-30T17:00:00Z"),
                        AvailabilityOverrideType.UNAVAILABLE
                )
        ));

        var result = service.compute(
                userId,
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        );

        // then
        assertEquals(2, result.size());

        assertEquals(Instant.parse("2026-03-30T09:00:00Z"), result.get(0).start());
        assertEquals(Instant.parse("2026-03-30T12:00:00Z"), result.get(0).end());

        assertEquals(Instant.parse("2026-03-30T17:00:00Z"), result.get(1).start());
        assertEquals(Instant.parse("2026-03-30T18:00:00Z"), result.get(1).end());
    }
}