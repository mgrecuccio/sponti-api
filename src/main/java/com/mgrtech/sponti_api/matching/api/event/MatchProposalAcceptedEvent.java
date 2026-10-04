package com.mgrtech.sponti_api.matching.api.event;

public record MatchProposalAcceptedEvent(
        Long matchId,
        Long initiatorUserId,
        Long candidateUserId
) {
}
