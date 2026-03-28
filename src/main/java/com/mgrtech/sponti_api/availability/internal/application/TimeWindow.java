package com.mgrtech.sponti_api.availability.internal.application;

import java.time.Instant;

public record TimeWindow(Instant start, Instant end) {

    boolean overlaps(TimeWindow other) {
        return start.isBefore(other.end) && end.isAfter(other.start);
    }

    boolean isValid() {
        return start!= null && end != null && start.isBefore(end);
    }
}
