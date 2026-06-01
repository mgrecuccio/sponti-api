package com.mgrtech.sponti_api.notification.internal.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "sponti.notification")
public record NotificationProperties(
        @NotNull
        Duration matchingSuggestionsCooldown
) {
}
