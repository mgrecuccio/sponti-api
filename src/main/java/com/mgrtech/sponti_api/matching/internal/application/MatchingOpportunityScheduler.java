package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.internal.configuration.MatchingOpportunitySchedulerProperties;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@AllArgsConstructor
class MatchingOpportunityScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchingOpportunityScheduler.class);

    private final MatchingOpportunitySchedulerProperties properties;
    private final MatchingOpportunityApplicationService service;
    private final OperationalMetrics metrics;

    @Scheduled(
            fixedDelayString = "${sponti.matching.opportunity-scheduler.fixed-delay}",
            initialDelayString = "${sponti.matching.opportunity-scheduler.fixed-delay}"
    )
    void checkCurrentOpportunities() {
        var startedAt = Instant.now();
        if (!properties.enabled()) {
            metrics.schedulerDuration("matching_opportunities", "skipped", Duration.between(startedAt, Instant.now()));
            log.debug("Matching opportunity scheduler skipped: reason=disabled");
            return;
        }

        try {
            service.checkCurrentOpportunities();
            metrics.schedulerDuration("matching_opportunities", "success", Duration.between(startedAt, Instant.now()));
        } catch (RuntimeException ex) {
            metrics.schedulerDuration("matching_opportunities", "failure", Duration.between(startedAt, Instant.now()));
            log.error("Matching opportunity scheduler failed", ex);
            throw ex;
        }
    }
}
