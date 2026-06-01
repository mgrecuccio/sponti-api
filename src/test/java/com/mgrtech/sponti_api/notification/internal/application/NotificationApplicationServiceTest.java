package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
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
    private final NotificationApplicationService service = new NotificationApplicationService(
            Clock.fixed(NOW, ZoneOffset.UTC),
            new NotificationProperties(Duration.ofMinutes(30)),
            repository
    );

    @Test
    void recordsMatchProposalNotificationWithoutCooldown() {
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
    void suppressesMatchingSuggestionsNotificationInsideCooldown() {
        when(repository.findFirstByUserIdAndTypeOrderBySentAtDesc(2L, NotificationType.MATCH_SUGGESTIONS_AVAILABLE))
                .thenReturn(Optional.of(new NotificationHistoryEntity(
                        2L,
                        NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                        null,
                        null,
                        NOW.minus(Duration.ofMinutes(10)),
                        "bestScore=100;suggestionFingerprint=2,3"
                )));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of("targetScreen", "suggestions")
        ));

        verify(repository, never()).save(any());
    }

    @Test
    void recordsMatchingSuggestionsNotificationAfterCooldownExpires() {
        when(repository.findFirstByUserIdAndTypeOrderBySentAtDesc(2L, NotificationType.MATCH_SUGGESTIONS_AVAILABLE))
                .thenReturn(Optional.of(new NotificationHistoryEntity(
                        2L,
                        NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                        null,
                        null,
                        NOW.minus(Duration.ofMinutes(31)),
                        "bestScore=80;suggestionFingerprint=2,3"
                )));

        service.send(new SendNotificationCommand(
                2L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "You may have new opportunities to reconnect.",
                Map.of("targetScreen", "suggestions")
        ));

        var captor = ArgumentCaptor.forClass(NotificationHistoryEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.MATCH_SUGGESTIONS_AVAILABLE);
        assertThat(captor.getValue().getSentAt()).isEqualTo(NOW);
    }

    @Test
    void suppressesMatchingSuggestionsNotificationWhenSameCandidatesAndScoreDidNotImprove() {
        when(repository.findFirstByUserIdAndTypeOrderBySentAtDesc(2L, NotificationType.MATCH_SUGGESTIONS_AVAILABLE))
                .thenReturn(Optional.of(new NotificationHistoryEntity(
                        2L,
                        NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                        null,
                        null,
                        NOW.minus(Duration.ofMinutes(31)),
                        "bestScore=100;suggestionFingerprint=2,3"
                )));

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
    }

    @Test
    void recordsMatchingSuggestionsNotificationWhenSameCandidatesButScoreImproves() {
        when(repository.findFirstByUserIdAndTypeOrderBySentAtDesc(2L, NotificationType.MATCH_SUGGESTIONS_AVAILABLE))
                .thenReturn(Optional.of(new NotificationHistoryEntity(
                        2L,
                        NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                        null,
                        null,
                        NOW.minus(Duration.ofMinutes(31)),
                        "bestScore=100;suggestionFingerprint=2,3"
                )));

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
        assertThat(captor.getValue().getMetadata()).contains("bestScore=120");
        assertThat(captor.getValue().getMetadata()).contains("suggestionFingerprint=2,3");
    }
}
