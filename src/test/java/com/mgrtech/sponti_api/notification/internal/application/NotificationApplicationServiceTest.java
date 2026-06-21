package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationApplicationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-06T10:00:00Z");

    private final NotificationHistoryRepository repository = mock(NotificationHistoryRepository.class);
    private final NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery = mock(UserMatchingPreferencesQuery.class);
    private final NotificationApplicationService service = new NotificationApplicationService(
            Clock.fixed(NOW, ZoneOffset.UTC),
            notificationProperties(),
            repository,
            dispatcher,
            userMatchingPreferencesQuery
    );

    NotificationApplicationServiceTest() {
        when(userMatchingPreferencesQuery.getMatchingPreferences(anyLong()))
                .thenReturn(Optional.of(preferences(true, true)));
    }

    @Test
    void recordsMatchProposalNotificationWithoutCooldown() {
        when(repository.existsByUserIdAndTypeAndRelatedMatchId(
                2L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                99L
        )).thenReturn(false);

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                "You have a new match invitation",
                "Someone is available and invited you to connect.",
                Map.of(
                        "matchId", "99",
                        "initiatorUserId", "1"
                )
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        verify(dispatcher).dispatch(eq(captor.getValue()), any());
        assertThat(captor.getValue())
                .satisfies(history -> {
                    assertThat(history.getUserId()).isEqualTo(2L);
                    assertThat(history.getType()).isEqualTo(NotificationType.MATCH_PROPOSAL_CREATED);
                    assertThat(history.getRelatedUserId()).isEqualTo(1L);
                    assertThat(history.getRelatedMatchId()).isEqualTo(99L);
                    assertThat(history.getSentAt()).isEqualTo(NOW);
                });
    }

    @Test
    void doesNotRecordDuplicateMatchProposalNotificationForSameMatch() {
        when(repository.existsByUserIdAndTypeAndRelatedMatchId(
                2L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                99L
        )).thenReturn(true);

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                "You have a new match invitation",
                "Someone is available and invited you to connect.",
                Map.of(
                        "matchId", "99",
                        "initiatorUserId", "1"
                )
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void recordsMatchAcceptedNotificationWithoutCooldown() {
        when(repository.existsByUserIdAndTypeAndRelatedMatchId(
                1L,
                NotificationType.MATCH_PROPOSAL_ACCEPTED,
                99L
        )).thenReturn(false);

        service.send(new SendNotificationCommand(
                1L,
                NotificationType.MATCH_PROPOSAL_ACCEPTED,
                "It's a match",
                "Open WhatsApp to start chatting.",
                Map.of(
                        "matchId", "99",
                        "candidateUserId", "2",
                        "targetScreen", "match_detail",
                        "action", "match_accepted"
                )
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        verify(dispatcher).dispatch(eq(captor.getValue()), any());
        assertThat(captor.getValue())
                .satisfies(history -> {
                    assertThat(history.getUserId()).isEqualTo(1L);
                    assertThat(history.getType()).isEqualTo(NotificationType.MATCH_PROPOSAL_ACCEPTED);
                    assertThat(history.getRelatedUserId()).isNull();
                    assertThat(history.getRelatedMatchId()).isEqualTo(99L);
                    assertThat(history.getSentAt()).isEqualTo(NOW);
                    assertThat(history.getMetadata()).contains("action=match_accepted");
                    assertThat(history.getMetadata()).contains("targetScreen=match_detail");
                });
    }

    @Test
    void doesNotRecordDuplicateMatchAcceptedNotificationForSameMatch() {
        when(repository.existsByUserIdAndTypeAndRelatedMatchId(
                1L,
                NotificationType.MATCH_PROPOSAL_ACCEPTED,
                99L
        )).thenReturn(true);

        service.send(new SendNotificationCommand(
                1L,
                NotificationType.MATCH_PROPOSAL_ACCEPTED,
                "It's a match",
                "Open WhatsApp to start chatting.",
                Map.of(
                        "matchId", "99",
                        "targetScreen", "match_detail",
                        "action", "match_accepted"
                )
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void doesNotRecordNotificationWhenPushDisabledByUserPreferences() {
        when(userMatchingPreferencesQuery.getMatchingPreferences(2L))
                .thenReturn(Optional.of(preferences(false, true)));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                "You have a new match invitation",
                "Someone is available and invited you to connect.",
                Map.of(
                        "matchId", "99",
                        "initiatorUserId", "1"
                )
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void doesNotRecordSuggestionNotificationWhenDisabledByUserPreferences() {
        when(userMatchingPreferencesQuery.getMatchingPreferences(2L))
                .thenReturn(Optional.of(preferences(true, false)));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of("targetScreen", "suggestions")
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void doesNotRecordMatchingSuggestionsNotificationInsideCooldown() {
        whenLatestSentMatchingSuggestionsReturns(new NotificationHistoryEntity(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                null,
                null,
                NOW.minus(Duration.ofMinutes(10)),
                "bestScore=100;suggestionFingerprint=2,3"
        ));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of("targetScreen", "suggestions")
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void recordsMatchingSuggestionsNotificationAfterCooldownExpires() {
        whenLatestSentMatchingSuggestionsReturns(new NotificationHistoryEntity(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                null,
                null,
                NOW.minus(Duration.ofMinutes(31)),
                "bestScore=80;suggestionFingerprint=2,3"
        ));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of("targetScreen", "suggestions")
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        verify(dispatcher).dispatch(eq(captor.getValue()), any());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.MATCH_SUGGESTIONS_AVAILABLE);
        assertThat(captor.getValue().getSentAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotRecordMatchingSuggestionsNotificationWhenSameCandidatesAndScoreDidNotImprove() {
        whenLatestSentMatchingSuggestionsReturns(new NotificationHistoryEntity(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                null,
                null,
                NOW.minus(Duration.ofMinutes(31)),
                "bestScore=100;suggestionFingerprint=2,3"
        ));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of(
                        "targetScreen", "suggestions",
                        "suggestionFingerprint", "2,3",
                        "bestScore", "100"
                )
        ));

        verify(repository, never()).save(any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    void recordsMatchingSuggestionsNotificationWhenSameCandidatesButScoreImproves() {
        whenLatestSentMatchingSuggestionsReturns(new NotificationHistoryEntity(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                null,
                null,
                NOW.minus(Duration.ofMinutes(31)),
                "bestScore=100;suggestionFingerprint=2,3"
        ));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of(
                        "targetScreen", "suggestions",
                        "suggestionFingerprint", "2,3",
                        "bestScore", "120"
                )
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        verify(dispatcher).dispatch(eq(captor.getValue()), any());
        assertThat(captor.getValue().getMetadata()).contains("bestScore=120");
        assertThat(captor.getValue().getMetadata()).contains("suggestionFingerprint=2,3");
    }

    @Test
    void ignoresSuppressedNotificationsWhenCheckingCooldown() {
        when(repository.findFirstByUserIdAndTypeAndStatusOrderBySentAtDesc(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                NotificationDeliveryStatus.SENT
        )).thenReturn(Optional.empty());

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of(
                        "targetScreen", "suggestions",
                        "suggestionFingerprint", "2,3",
                        "bestScore", "100"
                )
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        verify(dispatcher).dispatch(eq(captor.getValue()), any());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
    }

    private void whenLatestSentMatchingSuggestionsReturns(NotificationHistoryEntity history) {
        when(repository.findFirstByUserIdAndTypeAndStatusOrderBySentAtDesc(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                NotificationDeliveryStatus.SENT
        )).thenReturn(Optional.of(history));
    }

    private NotificationProperties notificationProperties() {
        return new NotificationProperties(
                Duration.ofMinutes(30),
                false,
                new NotificationProperties.Fcm(false, null),
                new NotificationProperties.Retry(true, Duration.ofMinutes(1), 3)
        );
    }

    private UserMatchingPreferencesView preferences(
            boolean pushNotificationsEnabled,
            boolean suggestionNotificationsEnabled
    ) {
        return new UserMatchingPreferencesView(
                2L,
                "UTC",
                true,
                true,
                null,
                null,
                pushNotificationsEnabled,
                suggestionNotificationsEnabled
        );
    }
}
