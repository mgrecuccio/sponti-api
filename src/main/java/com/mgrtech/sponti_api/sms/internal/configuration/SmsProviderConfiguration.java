package com.mgrtech.sponti_api.sms.internal.configuration;

import org.apache.http.HttpHeaders;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({SmsProviderProperties.class, OtpRateLimitProperties.class})
public class SmsProviderConfiguration {

    @Bean
    public RestClient smsBoxRestClient(SmsProviderProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(
                        "X-Api-Key",
                        properties.apiKey()
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }
}
