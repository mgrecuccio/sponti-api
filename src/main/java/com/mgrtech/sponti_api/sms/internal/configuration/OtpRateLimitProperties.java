package com.mgrtech.sponti_api.sms.internal.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "sponti.otp.rate-limit")
@Validated
public record OtpRateLimitProperties(
        @NotNull
        Duration window,
        @Min(1)
        int maxPerUser,
        @Min(1)
        int maxPerIp
) {
}
