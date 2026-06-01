package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.internal.configuration.MatchingOpportunitySchedulerProperties;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
class MatchingOpportunityScheduler {

    private final MatchingOpportunitySchedulerProperties properties;
    private final MatchingOpportunityApplicationService service;

    @Scheduled(
            fixedDelayString = "${sponti.matching.opportunity-scheduler.fixed-delay}",
            initialDelayString = "${sponti.matching.opportunity-scheduler.fixed-delay}"
    )
    void checkCurrentOpportunities() {
        if (!properties.enabled()) {
            return;
        }

        service.checkCurrentOpportunities();
    }
}
