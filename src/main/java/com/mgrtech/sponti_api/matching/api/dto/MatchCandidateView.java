package com.mgrtech.sponti_api.matching.api.dto;

import com.mgrtech.sponti_api.shared.ChannelType;

public record MatchCandidateView(Long userId, double score, ChannelType recommendedChannel) {
}
