package com.mgrtech.sponti_api.availability.internal.application.command;

import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdateAvailabilityRuleCommand(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        ChannelType channelType,
        boolean enabled
) {
}
