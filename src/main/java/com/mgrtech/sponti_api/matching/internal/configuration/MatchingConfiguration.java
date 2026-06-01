package com.mgrtech.sponti_api.matching.internal.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
        MatchingProperties.class,
        MatchingOpportunitySchedulerProperties.class
})
public class MatchingConfiguration {
}
