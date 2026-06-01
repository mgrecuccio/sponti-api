package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record SuggestedMatchView(
        Long candidateUserId,
        String nickName,
        boolean favorite,
        ChannelType channelType,
        int score,
        Instant overlapStart,
        Instant overlapEnd
) {
}
