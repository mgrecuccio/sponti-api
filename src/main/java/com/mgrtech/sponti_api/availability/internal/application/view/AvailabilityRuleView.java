package com.mgrtech.sponti_api.availability.internal.application.view;

import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record AvailabilityRuleView(
        Long id,
        Long userId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        ChannelType channelType,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
