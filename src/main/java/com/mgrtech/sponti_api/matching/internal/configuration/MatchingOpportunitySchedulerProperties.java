package com.mgrtech.sponti_api.matching.internal.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sponti.matching.opportunity-scheduler")
public record MatchingOpportunitySchedulerProperties(
        boolean enabled,

        @NotNull
        Duration fixedDelay
) {
}
