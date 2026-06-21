package com.mgrtech.sponti_api.shared.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OperationalMetrics {

    private static final String UNKNOWN = "unknown";

    private final MeterRegistry meterRegistry;

    public OperationalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void authFailure(String reason) {
        counter("sponti.auth.failures", "reason", reason);
    }

    public void refreshRotationFailure(String reason) {
        counter("sponti.auth.refresh.rotation.failures", "reason", reason);
    }

    public void matchProposalCreated(String channelType) {
        counter("sponti.match.proposals.created", "channel", channelType);
    }

    public void matchProposalResponded(String outcome) {
        counter("sponti.match.proposals.responses", "outcome", outcome);
    }

    public void contactLinkCreated(String type) {
        counter("sponti.contact.link.created", "type", type);
    }

    public void notificationDeliveryFailure(String notificationType, String failureType) {
        meterRegistry.counter(
                "sponti.notification.delivery.failures",
                "type", value(notificationType),
                "failure_type", value(failureType)
        ).increment();
    }

    public void notificationRetryVolume(String status) {
        counter("sponti.notification.retries", "status", status);
    }

    public void schedulerDuration(String scheduler, String status, Duration duration) {
        Timer.builder("sponti.scheduler.duration")
                .description("Scheduler execution duration")
                .tag("scheduler", value(scheduler))
                .tag("status", value(status))
                .register(meterRegistry)
                .record(duration);
    }

    private void counter(String name, String tagName, String tagValue) {
        meterRegistry.counter(name, tagName, value(tagValue)).increment();
    }

    private String value(String value) {
        return value == null || value.isBlank() ? UNKNOWN : value;
    }
}
