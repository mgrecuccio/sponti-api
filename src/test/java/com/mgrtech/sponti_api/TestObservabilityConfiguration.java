package com.mgrtech.sponti_api;

import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration(proxyBeanMethods = false)
public class TestObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    MeterRegistry testMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    OperationalMetrics testOperationalMetrics(MeterRegistry meterRegistry) {
        return new OperationalMetrics(meterRegistry);
    }
}
