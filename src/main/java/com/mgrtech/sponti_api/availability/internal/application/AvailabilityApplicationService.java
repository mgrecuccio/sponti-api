package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.api.*;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideEntity;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityRuleEntity;
import com.mgrtech.sponti_api.availability.internal.exception.AvailabilityRuleNotFoundException;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityOverrideRepository;
import com.mgrtech.sponti_api.availability.internal.repository.AvailabilityRuleRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class AvailabilityApplicationService implements AvailabilityFacade {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityApplicationService.class);
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AvailabilityOverrideRepository availabilityOverrideRepository;
    private final EffectiveAvailabilityService effectiveAvailabilityService;

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityRuleView> getRules(Long userId) {
        log.info("Availability rules requested: userId={}", userId);
        return availabilityRuleRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public AvailabilityRuleView createRule(Long userId, CreateAvailabilityRuleCommand command) {
        log.info("Availability rule creation requested: userId={}", userId);
        var entity = new AvailabilityRuleEntity(
                userId,
                command.dayOfWeek(),
                command.startTime(),
                command.endTime(),
                command.channelType()
        );

        var persistedEntity = availabilityRuleRepository.save(entity);
        log.info("Availability rules created: userId={} ruleId={}", userId, persistedEntity.getId());
        return toView(persistedEntity);
    }

    @Override
    public AvailabilityRuleView updateRule(Long userId, Long ruleId, UpdateAvailabilityRuleCommand command) {
        log.info("Availability rule update requested: userId={} ruleId={}", userId, ruleId);
        var entity = availabilityRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new AvailabilityRuleNotFoundException("Availability rule not found"));

        entity.update(
                command.dayOfWeek(),
                command.startTime(),
                command.endTime(),
                command.channelType(),
                command.enabled()
        );
        log.info("Availability rule updated: userId={} ruleId={}", userId, ruleId);
        return toView(entity);
    }

    @Override
    public void deleteRule(Long userId, Long ruleId) {
        log.info("Availability rule delete requested: userId={} ruleId={}", userId, ruleId);
        var entity = availabilityRuleRepository.findByIdAndUserId(ruleId, userId)
                .orElseThrow(() -> new AvailabilityRuleNotFoundException("Availability rule not found"));

        log.info("Availability rule deleted: userId={} ruleId={}", userId, ruleId);
        availabilityRuleRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailabilityOverrideView> getOverrides(Long userId) {
        log.info("Availability overrides requested: userId={}", userId);
        return availabilityOverrideRepository.findByUserIdOrderByStartDateTimeAsc(userId)
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public AvailabilityOverrideView createOverride(Long userId, CreateAvailabilityOverrideCommand command) {
        log.info("UserId={} is creating an availability override", userId);
        var entity = new AvailabilityOverrideEntity(
                userId,
                command.startDateTime(),
                command.endDateTime(),
                command.type()
        );

        log.info("Availability override created: userId={} overrideId={}", userId, entity.getId());
        return toView(availabilityOverrideRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EffectiveAvailabilityView> getEffectiveAvailability(Long userId, Instant from, Instant to) {
        log.info("Effective availabilities requested: userId={}", userId);
        return effectiveAvailabilityService.compute(userId, from, to)
                .stream()
                .map(window -> new EffectiveAvailabilityView(window.start(), window.end()))
                .toList();
    }

    private AvailabilityRuleView toView(AvailabilityRuleEntity entity) {
        return new AvailabilityRuleView(
                entity.getId(),
                entity.getUserId(),
                entity.getDayOfWeek(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getChannelType(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AvailabilityOverrideView toView(AvailabilityOverrideEntity entity) {
        return new AvailabilityOverrideView(
                entity.getId(),
                entity.getUserId(),
                entity.getStartDateTime(),
                entity.getEndDateTime(),
                entity.getType(),
                entity.getCreatedAt()
        );
    }
}