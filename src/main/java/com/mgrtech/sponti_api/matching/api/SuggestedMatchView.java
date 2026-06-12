package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.shared.api.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Suggested match candidate for the authenticated user.")
public record SuggestedMatchView(
        @Schema(description = "Internal id of the suggested candidate user.", example = "24")
        Long candidateUserId,
        @Schema(description = "Authenticated user's nickname for this contact.", example = "Gym buddy")
        String nickName,
        @Schema(description = "Whether the candidate is marked favorite by the authenticated user.", example = "true")
        boolean favorite,
        @Schema(description = "Suggested communication channel.", example = "CHAT")
        ChannelType channelType,
        @Schema(description = "Matching score used for ranking suggestions.", example = "120")
        int score,
        @Schema(description = "Start of overlapping availability.", example = "2026-06-12T12:00:00Z")
        Instant overlapStart,
        @Schema(description = "End of overlapping availability.", example = "2026-06-12T12:30:00Z")
        Instant overlapEnd
) {
}
