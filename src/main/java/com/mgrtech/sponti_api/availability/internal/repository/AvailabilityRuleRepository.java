package com.mgrtech.sponti_api.availability.internal.repository;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRuleEntity, Long> {

    List<AvailabilityRuleEntity> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    List<AvailabilityRuleEntity> findByUserIdAndEnabledTrue(Long userId);

    Optional<AvailabilityRuleEntity> findByIdAndUserId(Long id, Long userId);
}
