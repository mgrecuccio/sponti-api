package com.mgrtech.sponti_api.availability.api.dto;

import com.mgrtech.sponti_api.shared.ChannelType;

import java.time.OffsetDateTime;

public record PresenceView(Long userId, boolean freeNow, ChannelType channelType, OffsetDateTime expiresAt) {
}
