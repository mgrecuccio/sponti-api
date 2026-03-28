package com.mgrtech.sponti_api.availability.internal.domain;

import com.mgrtech.sponti_api.availability.internal.exception.InvalidAvailabilityOverrideTimeRangeException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "availability_overrides",
        indexes = {
                @Index(name = "idx_availability_overrides_user_id", columnList = "user_id"),
                @Index(name = "idx_availability_overrides_user_id_start_end", columnList = "user_id,start_date_time,end_date_time")
        }
)
@NoArgsConstructor
@Getter
public class AvailabilityOverrideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_date_time", nullable = false)
    private Instant startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private Instant endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AvailabilityOverrideType type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AvailabilityOverrideEntity(
            Long userId,
            Instant startDateTime,
            Instant endDateTime,
            AvailabilityOverrideType type
    ) {
        validateTimeRange(startDateTime, endDateTime);

        this.userId = userId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.type = type;
    }

    private void validateTimeRange(Instant startDateTime, Instant endDateTime) {
        if (startDateTime == null || endDateTime == null || !startDateTime.isBefore(endDateTime)) {
            throw new InvalidAvailabilityOverrideTimeRangeException("Availability override startDateTime must be before endDateTime");
        }
    }
}
