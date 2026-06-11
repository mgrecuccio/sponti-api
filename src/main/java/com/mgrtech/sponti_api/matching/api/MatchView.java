package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record MatchView(
        Long id,
        Long candidateUserId,
        ChannelType channelType,
        String status,
        int score,
        Instant overlapStart,
        Instant overlapEnd,
        Instant createdAt,
        Instant respondedAt
) {
    public static MatchView toMatchView(MatchProposalEntity entity) {
        return new MatchView(
                entity.getId(),
                entity.getCandidateUserId(),
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
