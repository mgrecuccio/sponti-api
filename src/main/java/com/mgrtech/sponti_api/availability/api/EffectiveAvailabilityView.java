package com.mgrtech.sponti_api.availability.api;

import java.time.Instant;

public record EffectiveAvailabilityView(
        Instant startDateTime,
        Instant endDateTime
) {
}
