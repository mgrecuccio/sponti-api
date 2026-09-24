package com.mgrtech.sponti_api.sms.internal.configuration;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "sponti.smsbox")
@Validated
public record SmsProviderProperties(
        @NotBlank
        String baseUrl,
        @NotBlank
        String apiKey,
        @NotBlank
        String otpText
) {
}
