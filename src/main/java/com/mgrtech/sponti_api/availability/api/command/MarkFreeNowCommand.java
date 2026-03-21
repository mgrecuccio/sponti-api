package com.mgrtech.sponti_api.availability.api.command;

import com.mgrtech.sponti_api.shared.ChannelType;

public record MarkFreeNowCommand(Long userId, ChannelType channelType, int durationMinutes) {
}
