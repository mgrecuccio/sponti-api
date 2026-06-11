package com.mgrtech.sponti_api.matching.api.event;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record MatchProposalCreatedEvent(
        Long matchId,
        Long initiatorUserId,
        Long candidateUserId,
        ChannelType channelType,
        Instant overlapStart,
        Instant overlapEnd
) {

    public static MatchProposalCreatedEvent from(MatchProposalEntity proposal) {
        return new MatchProposalCreatedEvent(
                proposal.getId(),
                proposal.getInitiatorUserId(),
                proposal.getCandidateUserId(),
                proposal.getChannelType(),
                proposal.getOverlapStart(),
                proposal.getOverlapEnd()
        );
    }
}
