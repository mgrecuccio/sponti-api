package com.mgrtech.sponti_api.availability.internal.domain;

import com.mgrtech.sponti_api.shared.ChannelType;

import java.time.OffsetDateTime;

public record PresenceState(Long userId, boolean freeNow, ChannelType channelType, OffsetDateTime expiresAt) {
}
