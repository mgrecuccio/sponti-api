package com.mgrtech.sponti_api.availability.api;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;

import java.time.Instant;

public record CreateAvailabilityOverrideCommand(
        Instant startDateTime,
        Instant endDateTime,
        AvailabilityOverrideType type
) {
}
