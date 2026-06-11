package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record MatchInvitationView(
        Long id,
        Long initiatorUserId,
        String initiatorDisplayName,
        ChannelType channelType,
        String status,
        int score,
        Instant overlapStart,
        Instant overlapEnd,
        Instant createdAt,
        Instant respondedAt
) {
    public static MatchInvitationView toMatchInvitationView(MatchProposalEntity entity) {
        return new MatchInvitationView(
                entity.getId(),
                entity.getInitiatorUserId(),
                null,
                entity.getChannelType(),
                entity.getStatus().name(),
                entity.getScore(),
                entity.getOverlapStart(),
                entity.getOverlapEnd(),
                entity.getCreatedAt(),
                entity.getRespondedAt()
        );
    }
}
