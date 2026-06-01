package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.matching.api.event.MatchProposalCreatedEvent;
import com.mgrtech.sponti_api.matching.api.event.MatchSuggestionsAvailableEvent;
import com.mgrtech.sponti_api.notification.api.NotificationFacade;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class MatchingNotificationListener {

    private final NotificationFacade notificationFacade;

    MatchingNotificationListener(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @EventListener
    void on(MatchProposalCreatedEvent event) {
        notificationFacade.send(new SendNotificationCommand(
                event.candidateUserId(),
                NotificationType.MATCH_PROPOSAL_CREATED,
                "You have a new match invitation",
                "Someone is available and invited you to connect.",
                Map.of(
                        "matchId", event.matchId().toString(),
                        "initiatorUserId", event.initiatorUserId().toString(),
                        "channelType", event.channelType().name(),
                        "targetScreen", "incoming_matches"
                )
        ));
    }

    @EventListener
    void on(MatchSuggestionsAvailableEvent event) {
        notificationFacade.send(new SendNotificationCommand(
                event.userId(),
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "There are people available now.",
                Map.of(
                        "targetScreen", "match_suggestions",
                        "candidateUserIds", candidateUserIds(event),
                        "suggestionFingerprint", candidateUserIds(event),
                        "bestScore", String.valueOf(event.bestScore()),
                        "overlapStart", event.overlapStart().toString(),
                        "overlapEnd", event.overlapEnd().toString()
                )
        ));
    }

    private String candidateUserIds(MatchSuggestionsAvailableEvent event) {
        return event.candidateUserIds()
                .stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
    }
}
