package com.mgrtech.sponti_api.availability.api;

import java.time.Instant;
import java.util.List;

public interface AvailabilityFacade {

    List<AvailabilityRuleView> getRules(Long userId);

    AvailabilityRuleView createRule(Long userId, CreateAvailabilityRuleCommand command);

    AvailabilityRuleView updateRule(Long userId, Long ruleId, UpdateAvailabilityRuleCommand command);

    void deleteRule(Long userId, Long ruleId);

    List<AvailabilityOverrideView> getOverrides(Long userId, Instant endsAfter);

    AvailabilityOverrideView createOverride(Long userId, CreateAvailabilityOverrideCommand command);

    List<EffectiveAvailabilityView> getEffectiveAvailability(Long userId, Instant from, Instant to);

}
