package com.mgrtech.sponti_api.matching.api.event;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;

public record MatchProposalAcceptedEvent(
        Long matchId,
        Long initiatorUserId,
        Long candidateUserId
) {

    public static MatchProposalAcceptedEvent from(MatchProposalEntity proposal) {
        return new MatchProposalAcceptedEvent(
                proposal.getId(),
                proposal.getInitiatorUserId(),
                proposal.getCandidateUserId()
        );
    }
}
