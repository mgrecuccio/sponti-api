package com.mgrtech.sponti_api.availability.api;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityChannelType;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateAvailabilityRuleCommand(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        AvailabilityChannelType channelType
) {
}
