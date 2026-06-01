package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.api.event.MatchSuggestionsAvailableEvent;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingOpportunitySchedulerProperties;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingProperties;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MatchingOpportunityApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery = mock(UserMatchingPreferencesQuery.class);
    private final MatchSuggestionsService matchSuggestionsService = mock(MatchSuggestionsService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private MatchingOpportunityApplicationService service;

    @BeforeEach
    void setUp() {
        service = new MatchingOpportunityApplicationService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                matchingProperties(),
                new MatchingOpportunitySchedulerProperties(true, Duration.ofMinutes(5)),
                userMatchingPreferencesQuery,
                matchSuggestionsService,
                eventPublisher
        );
    }

    @Test
    void publishesSuggestionAvailableEventForCurrentStrongSuggestions() {
        when(matchSuggestionsService.getSuggestions(1L))
                .thenReturn(List.of(
                        suggestion(2L, 120, NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofMinutes(30))),
                        suggestion(3L, 90, NOW.minus(Duration.ofMinutes(1)), NOW.plus(Duration.ofMinutes(20)))
                ));

        service.checkCurrentOpportunitiesForUser(1L);

        var eventCaptor = ArgumentCaptor.forClass(MatchSuggestionsAvailableEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .satisfies(event -> {
                    assertThat(event.userId()).isEqualTo(1L);
                    assertThat(event.candidateUserIds()).containsExactly(2L, 3L);
                    assertThat(event.bestScore()).isEqualTo(120);
                    assertThat(event.overlapStart()).isEqualTo(NOW.minus(Duration.ofMinutes(5)));
                    assertThat(event.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
                });
    }

    @Test
    void publishesSuggestionAvailableEventForSuggestionStartingBeforeNextSchedulerRun() {
        when(matchSuggestionsService.getSuggestions(1L))
                .thenReturn(List.of(suggestion(2L, 120, NOW.plus(Duration.ofSeconds(30)), NOW.plus(Duration.ofMinutes(30)))));

        service.checkCurrentOpportunitiesForUser(1L);

        verify(eventPublisher).publishEvent(any(MatchSuggestionsAvailableEvent.class));
    }

    @Test
    void skipsSuggestionsStartingAfterNextSchedulerRun() {
        when(matchSuggestionsService.getSuggestions(1L))
                .thenReturn(List.of(suggestion(2L, 120, NOW.plus(Duration.ofMinutes(6)), NOW.plus(Duration.ofMinutes(30)))));

        service.checkCurrentOpportunitiesForUser(1L);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void skipsSuggestionsBelowNotificationThreshold() {
        when(matchSuggestionsService.getSuggestions(1L))
                .thenReturn(List.of(suggestion(2L, 79, NOW.minus(Duration.ofMinutes(5)), NOW.plus(Duration.ofMinutes(30)))));

        service.checkCurrentOpportunitiesForUser(1L);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void checksOnlyMatchingEnabledUsersReturnedByUserModule() {
        when(userMatchingPreferencesQuery.getMatchingEnabledUserIds()).thenReturn(List.of(1L, 2L));
        when(matchSuggestionsService.getSuggestions(anyLong())).thenReturn(List.of());

        service.checkCurrentOpportunities();

        verify(matchSuggestionsService).getSuggestions(1L);
        verify(matchSuggestionsService).getSuggestions(2L);
    }

    private SuggestedMatchView suggestion(Long candidateUserId, int score, Instant start, Instant end) {
        return new SuggestedMatchView(
                candidateUserId,
                "Candidate " + candidateUserId,
                false,
                ChannelType.CHAT,
                score,
                start,
                end
        );
    }

    private MatchingProperties matchingProperties() {
        return new MatchingProperties(
                Duration.ofHours(4),
                Duration.ofMinutes(20),
                Duration.ofHours(48),
                Duration.ofHours(24),
                Duration.ofHours(2),
                Duration.ofMinutes(30),
                new MatchingProperties.Scoring(
                        10,
                        20,
                        40,
                        30,
                        20,
                        50,
                        1,
                        80
                )
        );
    }
}
