package com.mgrtech.sponti_api.availability.api.query;

import com.mgrtech.sponti_api.availability.api.view.EffectiveAvailabilityView;

import java.time.Instant;
import java.util.List;

public interface EffectiveAvailabilityQuery {

    List<EffectiveAvailabilityView> getEffectiveAvailability(Long userId, Instant from, Instant to);

    List<EffectiveAvailabilityView> getChannelEffectiveAvailability(Long userId, Instant from, Instant to);
}
