package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Match proposal response.")
public record MatchView(
        @Schema(description = "Match proposal id.", example = "51")
        Long id,
        @Schema(description = "Internal candidate user id.", example = "24")
        Long candidateUserId,
        @Schema(description = "Requested communication channel.", example = "CHAT")
        ChannelType channelType,
        @Schema(description = "Proposal status.", example = "PROPOSED")
        String status,
        @Schema(description = "Match score used when creating the proposal.", example = "120")
        int score,
        @Schema(description = "Start of overlapping availability.", example = "2026-06-12T12:00:00Z")
        Instant overlapStart,
        @Schema(description = "End of overlapping availability.", example = "2026-06-12T12:30:00Z")
        Instant overlapEnd,
        @Schema(description = "Proposal creation timestamp.", example = "2026-06-12T11:55:00Z")
        Instant createdAt,
        @Schema(description = "Timestamp when candidate accepted or declined.", example = "2026-06-12T12:01:00Z", nullable = true)
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
