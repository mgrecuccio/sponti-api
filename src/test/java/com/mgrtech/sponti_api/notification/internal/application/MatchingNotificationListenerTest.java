package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.matching.api.event.MatchProposalAcceptedEvent;
import com.mgrtech.sponti_api.matching.api.event.MatchProposalCreatedEvent;
import com.mgrtech.sponti_api.matching.api.event.MatchSuggestionsAvailableEvent;
import com.mgrtech.sponti_api.notification.api.NotificationFacade;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MatchingNotificationListenerTest {

    private final NotificationFacade notificationFacade = mock(NotificationFacade.class);
    private final MatchingNotificationListener listener = new MatchingNotificationListener(notificationFacade);

    @Test
    void sendsMatchProposalCreatedNotificationToCandidate() {
        listener.on(new MatchProposalCreatedEvent(
                99L,
                1L,
                2L,
                ChannelType.CHAT,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        var commandCaptor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationFacade).send(commandCaptor.capture());

        assertThat(commandCaptor.getValue())
                .satisfies(command -> {
                    assertThat(command.userId()).isEqualTo(2L);
                    assertThat(command.type()).isEqualTo(NotificationType.MATCH_PROPOSAL_CREATED);
                    assertThat(command.title()).isEqualTo("You have a new match invitation");
                    assertThat(command.data())
                            .containsEntry("matchId", "99")
                            .containsEntry("initiatorUserId", "1")
                            .containsEntry("channelType", "CHAT")
                            .containsEntry("targetScreen", "incoming_matches");
                });
    }

    @Test
    void sendsMatchProposalAcceptedNotificationToInitiator() {
        listener.on(new MatchProposalAcceptedEvent(
                99L,
                1L,
                2L
        ));

        var commandCaptor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationFacade).send(commandCaptor.capture());

        assertThat(commandCaptor.getValue())
                .satisfies(command -> {
                    assertThat(command.userId()).isEqualTo(1L);
                    assertThat(command.type()).isEqualTo(NotificationType.MATCH_PROPOSAL_ACCEPTED);
                    assertThat(command.title()).isEqualTo("It's a match");
                    assertThat(command.body()).isEqualTo("Open WhatsApp to start chatting.");
                    assertThat(command.data())
                            .containsEntry("matchId", "99")
                            .containsEntry("candidateUserId", "2")
                            .containsEntry("targetScreen", "match_detail")
                            .containsEntry("action", "match_accepted");
                });
    }

    @Test
    void sendsMatchSuggestionsAvailableNotificationToUser() {
        listener.on(new MatchSuggestionsAvailableEvent(
                2L,
                List.of(3L, 4L),
                120,
                Instant.parse("2026-03-30T09:00:00Z"),
                Instant.parse("2026-03-30T10:00:00Z")
        ));

        var commandCaptor = ArgumentCaptor.forClass(SendNotificationCommand.class);
        verify(notificationFacade).send(commandCaptor.capture());

        assertThat(commandCaptor.getValue())
                .satisfies(command -> {
                    assertThat(command.userId()).isEqualTo(2L);
                    assertThat(command.type()).isEqualTo(NotificationType.MATCH_SUGGESTIONS_AVAILABLE);
                    assertThat(command.title()).isEqualTo("New suggestions available");
                    assertThat(command.data())
                            .containsEntry("targetScreen", "match_suggestions")
                            .containsEntry("candidateUserIds", "3,4")
                            .containsEntry("suggestionFingerprint", "3,4")
                            .containsEntry("bestScore", "120");
                });
    }
}
