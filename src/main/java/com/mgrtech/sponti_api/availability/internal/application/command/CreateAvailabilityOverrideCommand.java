package com.mgrtech.sponti_api.availability.internal.application.command;

import com.mgrtech.sponti_api.availability.api.AvailabilityOverrideType;

import java.time.Instant;

public record CreateAvailabilityOverrideCommand(
        Instant startDateTime,
        Instant endDateTime,
        AvailabilityOverrideType type
) {
}
