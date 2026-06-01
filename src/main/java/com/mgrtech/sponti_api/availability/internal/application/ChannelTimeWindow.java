package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.shared.api.ChannelType;

import java.time.Instant;

public record ChannelTimeWindow(
        Instant start,
        Instant end,
        ChannelType channelType
) {

    boolean isValid() {
        return start != null && end != null && start.isBefore(end);
    }

    boolean overlaps(TimeWindow other) {
        return start.isBefore(other.end()) && other.start().isBefore(end);
    }
}
