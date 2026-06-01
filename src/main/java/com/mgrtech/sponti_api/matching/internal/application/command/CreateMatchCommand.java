package com.mgrtech.sponti_api.matching.internal.application.command;

import com.mgrtech.sponti_api.shared.api.ChannelType;

public record CreateMatchCommand(Long candidateUserId, ChannelType channelType) {
}
