package com.mgrtech.sponti_api.matching.api.event;

import java.time.Instant;
import java.util.List;

public record MatchSuggestionsAvailableEvent(
        Long userId,
        List<Long> candidateUserIds,
        int bestScore,
        Instant overlapStart,
        Instant overlapEnd
) {
}
