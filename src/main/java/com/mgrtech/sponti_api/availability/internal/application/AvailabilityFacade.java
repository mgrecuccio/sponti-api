package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityOverrideCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.CreateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.internal.application.command.UpdateAvailabilityRuleCommand;
import com.mgrtech.sponti_api.availability.api.query.EffectiveAvailabilityQuery;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityOverrideView;
import com.mgrtech.sponti_api.availability.internal.application.view.AvailabilityRuleView;

import java.time.Instant;
import java.util.List;

public interface AvailabilityFacade extends EffectiveAvailabilityQuery {

    List<AvailabilityRuleView> getRules(Long userId);

    AvailabilityRuleView createRule(Long userId, CreateAvailabilityRuleCommand command);

    AvailabilityRuleView updateRule(Long userId, Long ruleId, UpdateAvailabilityRuleCommand command);

    void deleteRule(Long userId, Long ruleId);

    List<AvailabilityOverrideView> getOverrides(Long userId, Instant endsAfter);

    AvailabilityOverrideView createOverride(Long userId, CreateAvailabilityOverrideCommand command);

}
