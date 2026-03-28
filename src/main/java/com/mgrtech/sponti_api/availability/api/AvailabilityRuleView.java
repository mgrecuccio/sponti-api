package com.mgrtech.sponti_api.availability.api;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityChannelType;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record AvailabilityRuleView(
        Long id,
        Long userId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        AvailabilityChannelType channelType,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
