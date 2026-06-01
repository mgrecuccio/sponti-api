package com.mgrtech.sponti_api.availability.internal.domain;

import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.availability.internal.exception.InvalidAvailabilityRuleTimeRangeException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(
        name = "availability_rules",
        indexes = {
                @Index(name = "idx_availability_rules_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor
@Getter
public class AvailabilityRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public AvailabilityRuleEntity(
            Long userId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            ChannelType channelType
    ) {
        validateTimeRange(startTime, endTime);

        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.channelType = channelType;
        this.enabled = true;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidAvailabilityRuleTimeRangeException("Availability rule startTime must be before endTime");
        }
    }

    public void update(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            ChannelType channelType,
            boolean enabled
    ) {
        validateTimeRange(startTime, endTime);
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.channelType = channelType;
        this.enabled = enabled;
    }
}
