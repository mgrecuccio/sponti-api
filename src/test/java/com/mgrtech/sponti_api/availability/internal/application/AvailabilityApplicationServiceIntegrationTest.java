package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.DatabaseCleaner;
import com.mgrtech.sponti_api.FullIntegrationTest;
import com.mgrtech.sponti_api.availability.internal.application.AvailabilityFacade;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityOverrideCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.UpdateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityOverrideView;
import com.mgrtech.sponti_api.availability.internal.exception.AvailabilityRuleNotFoundException;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.UserRegistrationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@FullIntegrationTest
class AvailabilityApplicationServiceIntegrationTest {

    @Autowired
    AvailabilityFacade availabilityFacade;

    @Autowired
    UserRegistrationFacade userRegistrationFacade;

    @Autowired
    DatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    void create_update_and_delete_rule_persists_changes_for_user() {
        var user = createUser("owner@example.com", "Europe/Brussels");

        var created = availabilityFacade.createRule(
                user.id(),
                new CreateAvailabilityRuleCommand(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        ChannelType.CHAT
                )
        );

        assertThat(created.id()).isNotNull();
        assertThat(created.userId()).isEqualTo(user.id());
        assertThat(created.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(created.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(created.endTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(created.channelType()).isEqualTo(ChannelType.CHAT);
        assertThat(created.enabled()).isTrue();
        assertThat(created.createdAt()).isNotNull();
        assertThat(created.updatedAt()).isNotNull();

        var updated = availabilityFacade.updateRule(
                user.id(),
                created.id(),
                new UpdateAvailabilityRuleCommand(
                        DayOfWeek.TUESDAY,
                        LocalTime.of(14, 0),
                        LocalTime.of(16, 30),
                        ChannelType.CALL,
                        false
                )
        );

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.dayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(updated.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(updated.endTime()).isEqualTo(LocalTime.of(16, 30));
        assertThat(updated.channelType()).isEqualTo(ChannelType.CALL);
        assertThat(updated.enabled()).isFalse();

        assertThat(availabilityFacade.getRules(user.id()))
                .singleElement()
                .satisfies(rule -> {
                    assertThat(rule.id()).isEqualTo(created.id());
                    assertThat(rule.enabled()).isFalse();
                });

        availabilityFacade.deleteRule(user.id(), created.id());

        assertThat(availabilityFacade.getRules(user.id())).isEmpty();
    }

    @Test
    void update_and_delete_rule_are_scoped_to_owner() {
        var owner = createUser("owner@example.com", "UTC");
        var otherUser = createUser("other@example.com", "UTC");

        var rule = availabilityFacade.createRule(
                owner.id(),
                new CreateAvailabilityRuleCommand(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        ChannelType.CHAT
                )
        );

        assertThatThrownBy(() -> availabilityFacade.updateRule(
                otherUser.id(),
                rule.id(),
                new UpdateAvailabilityRuleCommand(
                        DayOfWeek.MONDAY,
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        ChannelType.CALL,
                        true
                )
        )).isInstanceOf(AvailabilityRuleNotFoundException.class);

        assertThatThrownBy(() -> availabilityFacade.deleteRule(otherUser.id(), rule.id()))
                .isInstanceOf(AvailabilityRuleNotFoundException.class);

        assertThat(availabilityFacade.getRules(owner.id())).hasSize(1);
    }

    @Test
    void get_rules_returns_only_requested_user_ordered_by_day_and_start_time() {
        var user = createUser("owner@example.com", "UTC");
        var otherUser = createUser("other@example.com", "UTC");

        availabilityFacade.createRule(
                user.id(),
                new CreateAvailabilityRuleCommand(DayOfWeek.WEDNESDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), ChannelType.CHAT)
        );
        availabilityFacade.createRule(
                user.id(),
                new CreateAvailabilityRuleCommand(DayOfWeek.MONDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), ChannelType.CALL)
        );
        availabilityFacade.createRule(
                user.id(),
                new CreateAvailabilityRuleCommand(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0), ChannelType.CHAT)
        );
        availabilityFacade.createRule(
                otherUser.id(),
                new CreateAvailabilityRuleCommand(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(9, 0), ChannelType.CHAT)
        );

        assertThat(availabilityFacade.getRules(user.id()))
                .extracting(rule -> rule.dayOfWeek() + " " + rule.startTime())
                .containsExactly(
                        "MONDAY 09:00",
                        "MONDAY 15:00",
                        "WEDNESDAY 16:00"
                );
    }

    @Test
    void create_and_query_overrides_persists_and_filters_by_end_time() {
        var user = createUser("owner@example.com", "UTC");
        var otherUser = createUser("other@example.com", "UTC");

        availabilityFacade.createOverride(
                user.id(),
                new CreateAvailabilityOverrideCommand(
                        Instant.parse("2026-04-24T09:00:00Z"),
                        Instant.parse("2026-04-24T10:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                )
        );
        var later = availabilityFacade.createOverride(
                user.id(),
                new CreateAvailabilityOverrideCommand(
                        Instant.parse("2026-04-25T09:00:00Z"),
                        Instant.parse("2026-04-25T10:00:00Z"),
                        AvailabilityOverrideType.UNAVAILABLE
                )
        );
        availabilityFacade.createOverride(
                otherUser.id(),
                new CreateAvailabilityOverrideCommand(
                        Instant.parse("2026-04-26T09:00:00Z"),
                        Instant.parse("2026-04-26T10:00:00Z"),
                        AvailabilityOverrideType.AVAILABLE
                )
        );

        assertThat(availabilityFacade.getOverrides(user.id(), null))
                .extracting(AvailabilityOverrideView::startDateTime)
                .containsExactly(
                        Instant.parse("2026-04-24T09:00:00Z"),
                        Instant.parse("2026-04-25T09:00:00Z")
                );

        assertThat(availabilityFacade.getOverrides(user.id(), Instant.parse("2026-04-24T10:00:00Z")))
                .singleElement()
                .satisfies(override -> {
                    assertThat(override.id()).isEqualTo(later.id());
                    assertThat(override.type()).isEqualTo(AvailabilityOverrideType.UNAVAILABLE);
                    assertThat(override.createdAt()).isNotNull();
                });
    }

    @Test
    void effective_availability_uses_persisted_rules_overrides_and_user_timezone() {
        var user = createUser("owner@example.com", "Europe/Brussels");

        availabilityFacade.createRule(
                user.id(),
                new CreateAvailabilityRuleCommand(
                        DayOfWeek.MONDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(12, 0),
                        ChannelType.CHAT
                )
        );
        availabilityFacade.createOverride(
                user.id(),
                new CreateAvailabilityOverrideCommand(
                        Instant.parse("2026-03-30T08:30:00Z"),
                        Instant.parse("2026-03-30T09:00:00Z"),
                        AvailabilityOverrideType.UNAVAILABLE
                )
        );

        assertThat(availabilityFacade.getEffectiveAvailability(
                user.id(),
                Instant.parse("2026-03-30T00:00:00Z"),
                Instant.parse("2026-03-30T23:00:00Z")
        )).satisfiesExactly(
                window -> {
                    assertThat(window.startDateTime()).isEqualTo(Instant.parse("2026-03-30T07:00:00Z"));
                    assertThat(window.endDateTime()).isEqualTo(Instant.parse("2026-03-30T08:30:00Z"));
                    assertThat(window.channelType()).isNull();
                },
                window -> {
                    assertThat(window.startDateTime()).isEqualTo(Instant.parse("2026-03-30T09:00:00Z"));
                    assertThat(window.endDateTime()).isEqualTo(Instant.parse("2026-03-30T10:00:00Z"));
                    assertThat(window.channelType()).isNull();
                });
    }

    private com.mgrtech.sponti_api.user.api.view.CreatedUserView createUser(String email, String timezone) {
        return userRegistrationFacade.createUser(
                new CreateUserCommand(
                        email,
                        "password-hash",
                        email.substring(0, email.indexOf('@')),
                        timezone
                )
        );
    }
}
