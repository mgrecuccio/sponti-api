package com.mgrtech.sponti_api.availability.internal.application.view;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;

import java.time.Instant;

public record AvailabilityOverrideView(
        Long id,
        Long userId,
        Instant startDateTime,
        Instant endDateTime,
        AvailabilityOverrideType type,
        Instant createdAt
) {
}
