package com.mgrtech.sponti_api.availability.internal.repository;

import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverrideEntity, Long> {

    List<AvailabilityOverrideEntity> findByUserIdOrderByStartDateTimeAsc(Long userId);

    /*
      It retrieves all overrides for a user that overlap with a given time range, ordered by start time.
        SELECT *
            FROM availability_overrides
        WHERE user_id = :userId
            AND start_date_time < :to
            AND end_date_time > :from
        ORDER BY start_date_time ASC;
     */
    List<AvailabilityOverrideEntity> findByUserIdAndStartDateTimeLessThanAndEndDateTimeGreaterThanOrderByStartDateTimeAsc(
            Long userId,
            Instant rangeEndExclusive,
            Instant rangeStartExclusive
    );
}
