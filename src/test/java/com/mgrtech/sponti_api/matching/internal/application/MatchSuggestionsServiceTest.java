package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.availability.api.query.EffectiveAvailabilityQuery;
import com.mgrtech.sponti_api.availability.api.view.EffectiveAvailabilityView;
import com.mgrtech.sponti_api.contact.api.query.ContactQuery;
import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.matching.internal.configuration.MatchingProperties;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;
import com.mgrtech.sponti_api.matching.api.event.MatchProposalCreatedEvent;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalStatus;
import com.mgrtech.sponti_api.matching.internal.exception.AcceptedContactNotFoundException;
import com.mgrtech.sponti_api.matching.internal.exception.AvailabilityOverlapNotFoundException;
import com.mgrtech.sponti_api.matching.internal.exception.ChannelNotAllowedException;
import com.mgrtech.sponti_api.matching.internal.exception.MatchAlreadyExistsException;
import com.mgrtech.sponti_api.matching.internal.exception.MatchNotFoundException;
import com.mgrtech.sponti_api.matching.internal.exception.MatchProposalExpiredException;
import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MatchSuggestionsServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CANDIDATE_ID = 2L;
    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private final EffectiveAvailabilityQuery effectiveAvailabilityQuery = mock(EffectiveAvailabilityQuery.class);
    private final ContactQuery contactQuery = mock(ContactQuery.class);
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery = mock(UserMatchingPreferencesQuery.class);
    private final MatchProposalRepository repository = mock(MatchProposalRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private MatchSuggestionsService service;

    @BeforeEach
    void setUp() {
        service = new MatchSuggestionsService(
                Clock.fixed(NOW, ZoneOffset.UTC),
                matchingProperties(),
                effectiveAvailabilityQuery,
                contactQuery,
                userMatchingPreferencesQuery,
                repository,
                eventPublisher
        );

        when(userMatchingPreferencesQuery.getMatchingPreferences(USER_ID))
                .thenReturn(Optional.of(preferences(USER_ID, true, true, null, null)));
        when(userMatchingPreferencesQuery.getMatchingPreferences(CANDIDATE_ID))
                .thenReturn(Optional.of(preferences(CANDIDATE_ID, true, true, null, null)));
        when(contactQuery.getAcceptedContacts(USER_ID))
                .thenReturn(List.of(new ContactView(CANDIDATE_ID, "Marco", true, NOW.minus(Duration.ofDays(10)))));
    }

    @Test
    void createMatchPersistsProposedMatchWithRecomputedScoreAndOverlap() {
        var to = NOW.plus(Duration.ofHours(4));
        var contact = new ContactView(CANDIDATE_ID, "Marco", true, NOW.minus(Duration.ofDays(10)));
        when(contactQuery.findAcceptedContact(USER_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(contact));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(repository.save(any(MatchProposalEntity.class)))
                .thenAnswer(invocation -> persistedSuggestion(invocation.getArgument(0)));

        var match = service.createMatch(USER_ID, new CreateMatchCommand(CANDIDATE_ID, ChannelType.CHAT));

        assertThat(match.id()).isEqualTo(10L);
        assertThat(match.candidateUserId()).isEqualTo(CANDIDATE_ID);
        assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.PROPOSED.name());
        assertThat(match.score()).isEqualTo(90);
        assertThat(match.overlapStart()).isEqualTo(NOW);
        assertThat(match.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
        assertThat(match.createdAt()).isEqualTo(NOW);

        var entityCaptor = ArgumentCaptor.forClass(MatchProposalEntity.class);
        verify(repository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue())
                .satisfies(entity -> {
                    assertThat(entity.getInitiatorUserId()).isEqualTo(USER_ID);
                    assertThat(entity.getCandidateUserId()).isEqualTo(CANDIDATE_ID);
                    assertThat(entity.getChannelType()).isEqualTo(ChannelType.CHAT);
                    assertThat(entity.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED);
                    assertThat(entity.getScore()).isEqualTo(90);
                    assertThat(entity.getOverlapStart()).isEqualTo(NOW);
                    assertThat(entity.getOverlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
                    assertThat(entity.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
                });

        var eventCaptor = ArgumentCaptor.forClass(MatchProposalCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .satisfies(event -> {
                    assertThat(event.matchId()).isEqualTo(10L);
                    assertThat(event.initiatorUserId()).isEqualTo(USER_ID);
                    assertThat(event.candidateUserId()).isEqualTo(CANDIDATE_ID);
                    assertThat(event.channelType()).isEqualTo(ChannelType.CHAT);
                    assertThat(event.overlapStart()).isEqualTo(NOW);
                    assertThat(event.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
                });
    }

    @Test
    void acceptMatchMarksCandidateIncomingSuggestionAsAccepted() {
        var suggestion = persistedSuggestion(new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CHAT,
                90,
                NOW,
                NOW.plus(Duration.ofMinutes(60))
        ));
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.of(suggestion));

        var match = service.acceptMatch(CANDIDATE_ID, 10L);

        assertThat(suggestion.getStatus()).isEqualTo(MatchProposalStatus.ACCEPTED);
        assertThat(match.id()).isEqualTo(10L);
        assertThat(match.candidateUserId()).isEqualTo(CANDIDATE_ID);
        assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.ACCEPTED.name());
        assertThat(match.score()).isEqualTo(90);
        assertThat(match.overlapStart()).isEqualTo(NOW);
        assertThat(match.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
    }

    @Test
    void acceptMatchThrowsWhenSuggestionDoesNotBelongToCandidate() {
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acceptMatch(CANDIDATE_ID, 10L))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void acceptMatchThrowsWhenSuggestionIsExpired() {
        var suggestion = persistedSuggestion(new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CHAT,
                90,
                NOW.minus(Duration.ofMinutes(60)),
                NOW.plus(Duration.ofMinutes(60)),
                NOW
        ));
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> service.acceptMatch(CANDIDATE_ID, 10L))
                .isInstanceOf(MatchProposalExpiredException.class)
                .hasMessage("Match proposal has expired");

        assertThat(suggestion.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED);
    }

    @Test
    void declineMatchMarksCandidateIncomingSuggestionAsDeclined() {
        var suggestion = persistedSuggestion(new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CALL,
                75,
                NOW,
                NOW.plus(Duration.ofMinutes(45))
        ));
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.of(suggestion));

        var match = service.declineMatch(CANDIDATE_ID, 10L);

        assertThat(suggestion.getStatus()).isEqualTo(MatchProposalStatus.DECLINED);
        assertThat(match.id()).isEqualTo(10L);
        assertThat(match.candidateUserId()).isEqualTo(CANDIDATE_ID);
        assertThat(match.channelType()).isEqualTo(ChannelType.CALL);
        assertThat(match.status()).isEqualTo(MatchProposalStatus.DECLINED.name());
        assertThat(match.score()).isEqualTo(75);
        assertThat(match.overlapStart()).isEqualTo(NOW);
        assertThat(match.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(45)));
    }

    @Test
    void declineMatchThrowsWhenSuggestionDoesNotBelongToCandidate() {
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.declineMatch(CANDIDATE_ID, 10L))
                .isInstanceOf(MatchNotFoundException.class);
    }

    @Test
    void declineMatchThrowsWhenSuggestionIsExpired() {
        var suggestion = persistedSuggestion(new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CALL,
                75,
                NOW.minus(Duration.ofMinutes(60)),
                NOW.plus(Duration.ofMinutes(45)),
                NOW.minus(Duration.ofSeconds(1))
        ));
        when(repository.findByIdAndCandidateUserId(10L, CANDIDATE_ID))
                .thenReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> service.declineMatch(CANDIDATE_ID, 10L))
                .isInstanceOf(MatchProposalExpiredException.class)
                .hasMessage("Match proposal has expired");

        assertThat(suggestion.getStatus()).isEqualTo(MatchProposalStatus.PROPOSED);
    }

    @Test
    void getIncomingMatchesReturnsActiveProposedInvitationsForCandidate() {
        var suggestion = persistedSuggestion(new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CHAT,
                90,
                NOW,
                NOW.plus(Duration.ofMinutes(60)),
                NOW.plus(Duration.ofMinutes(30))
        ));
        when(repository.findActiveIncoming(CANDIDATE_ID, MatchProposalStatus.PROPOSED, NOW))
                .thenReturn(List.of(suggestion));

        var incoming = service.getIncomingMatches(CANDIDATE_ID);

        assertThat(incoming).hasSize(1);
        assertThat(incoming.getFirst())
                .satisfies(match -> {
                    assertThat(match.id()).isEqualTo(10L);
                    assertThat(match.initiatorUserId()).isEqualTo(USER_ID);
                    assertThat(match.initiatorDisplayName()).isNull();
                    assertThat(match.channelType()).isEqualTo(ChannelType.CHAT);
                    assertThat(match.status()).isEqualTo(MatchProposalStatus.PROPOSED.name());
                    assertThat(match.score()).isEqualTo(90);
                    assertThat(match.overlapStart()).isEqualTo(NOW);
                    assertThat(match.overlapEnd()).isEqualTo(NOW.plus(Duration.ofMinutes(60)));
                });
    }

    @Test
    void createMatchThrowsWhenCandidateIsNotAcceptedContact() {
        when(contactQuery.findAcceptedContact(USER_ID, CANDIDATE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMatch(USER_ID, new CreateMatchCommand(CANDIDATE_ID, ChannelType.CHAT)))
                .isInstanceOf(AcceptedContactNotFoundException.class);

        verifyNoInteractions(effectiveAvailabilityQuery);
    }

    @Test
    void createMatchThrowsWhenActiveProposalAlreadyExists() {
        when(contactQuery.findAcceptedContact(USER_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(new ContactView(CANDIDATE_ID, "Marco", true, NOW.minus(Duration.ofDays(10)))));
        when(repository.existsByInitiatorUserIdAndCandidateUserIdAndStatus(
                USER_ID,
                CANDIDATE_ID,
                MatchProposalStatus.PROPOSED
        )).thenReturn(true);

        assertThatThrownBy(() -> service.createMatch(USER_ID, new CreateMatchCommand(CANDIDATE_ID, ChannelType.CHAT)))
                .isInstanceOf(MatchAlreadyExistsException.class);

        verifyNoInteractions(effectiveAvailabilityQuery);
    }

    @Test
    void createMatchThrowsWhenRequestedChannelIsNotAllowed() {
        when(contactQuery.findAcceptedContact(USER_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(new ContactView(CANDIDATE_ID, "Marco", true, NOW.minus(Duration.ofDays(10)))));
        when(userMatchingPreferencesQuery.getMatchingPreferences(USER_ID))
                .thenReturn(Optional.of(preferences(USER_ID, false, true, null, null)));

        assertThatThrownBy(() -> service.createMatch(USER_ID, new CreateMatchCommand(CANDIDATE_ID, ChannelType.CHAT)))
                .isInstanceOf(ChannelNotAllowedException.class);

        verifyNoInteractions(effectiveAvailabilityQuery);
    }

    @Test
    void createMatchThrowsWhenNoOverlapExistsForRequestedChannel() {
        var to = NOW.plus(Duration.ofHours(4));
        when(contactQuery.findAcceptedContact(USER_ID, CANDIDATE_ID))
                .thenReturn(Optional.of(new ContactView(CANDIDATE_ID, "Marco", true, NOW.minus(Duration.ofDays(10)))));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW.plus(Duration.ofHours(2)), NOW.plus(Duration.ofHours(3)), ChannelType.CHAT)));

        assertThatThrownBy(() -> service.createMatch(USER_ID, new CreateMatchCommand(CANDIDATE_ID, ChannelType.CHAT)))
                .isInstanceOf(AvailabilityOverlapNotFoundException.class);
    }

    @Test
    void scoresCandidateWithConfigurableSoftPenaltiesInsteadOfFilteringItOut() {
        var to = NOW.plus(Duration.ofHours(4));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(repository.findFirstByInitiatorUserIdAndCandidateUserIdAndStatusOrderByRespondedAtDesc(
                USER_ID,
                CANDIDATE_ID,
                MatchProposalStatus.DECLINED
        )).thenReturn(Optional.of(suggestion(NOW.minus(Duration.ofHours(1)), NOW.minus(Duration.ofHours(1)))));

        var suggestions = service.getSuggestions(USER_ID);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().score()).isEqualTo(60);
    }

    @Test
    void filtersCandidateWhenOverlapChannelIsDisabledByPreferences() {
        var to = NOW.plus(Duration.ofHours(4));
        when(userMatchingPreferencesQuery.getMatchingPreferences(USER_ID))
                .thenReturn(Optional.of(preferences(USER_ID, true, false, null, null)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CALL)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CALL)));

        var suggestions = service.getSuggestions(USER_ID);

        assertThat(suggestions).isEmpty();
    }

    @Test
    void matchesChannelAgnosticAvailabilityAgainstAllowedSpecificChannel() {
        var to = NOW.plus(Duration.ofHours(4));
        when(userMatchingPreferencesQuery.getMatchingPreferences(USER_ID))
                .thenReturn(Optional.of(preferences(USER_ID, true, false, null, null)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), null)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));

        var suggestions = service.getSuggestions(USER_ID);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().channelType()).isEqualTo(ChannelType.CHAT);
    }

    @Test
    void appliesQuietHoursPenaltyWhenEitherUserIsInsideQuietHours() {
        var to = NOW.plus(Duration.ofHours(4));
        when(userMatchingPreferencesQuery.getMatchingPreferences(CANDIDATE_ID))
                .thenReturn(Optional.of(preferences(
                        CANDIDATE_ID,
                        true,
                        true,
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0)
                )));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(USER_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));
        when(effectiveAvailabilityQuery.getChannelEffectiveAvailability(CANDIDATE_ID, NOW, to))
                .thenReturn(List.of(new EffectiveAvailabilityView(NOW, NOW.plus(Duration.ofMinutes(60)), ChannelType.CHAT)));

        var suggestions = service.getSuggestions(USER_ID);

        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.getFirst().score()).isEqualTo(40);
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

    private UserMatchingPreferencesView preferences(
            Long userId,
            boolean allowChat,
            boolean allowCall,
            LocalTime quietHoursStart,
            LocalTime quietHoursEnd
    ) {
        return new UserMatchingPreferencesView(
                userId,
                "UTC",
                allowChat,
                allowCall,
                quietHoursStart,
                quietHoursEnd
        );
    }

    private MatchProposalEntity suggestion(Instant createdAt, Instant respondedAt) {
        var suggestion = new MatchProposalEntity(
                USER_ID,
                CANDIDATE_ID,
                ChannelType.CHAT,
                100,
                NOW,
                NOW.plus(Duration.ofMinutes(60))
        );
        ReflectionTestUtils.setField(suggestion, "createdAt", createdAt);
        ReflectionTestUtils.setField(suggestion, "respondedAt", respondedAt);
        return suggestion;
    }

    private MatchProposalEntity persistedSuggestion(MatchProposalEntity suggestion) {
        ReflectionTestUtils.setField(suggestion, "id", 10L);
        ReflectionTestUtils.setField(suggestion, "createdAt", NOW);
        return suggestion;
    }
}
