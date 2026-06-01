package com.mgrtech.sponti_api.availability.api.view;

import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record EffectiveAvailabilityView(
        Instant startDateTime,
        Instant endDateTime,
        ChannelType channelType
) {
}
