package com.mgrtech.sponti_api;

import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@TestConfiguration(proxyBeanMethods = false)
public class NotificationTestConfigurationProperties {


    @Bean
    @Primary
    NotificationProperties testNotificationProperties() {
        return new NotificationProperties(
                Duration.ofMinutes(1),
                true,
                new NotificationProperties.Fcm(false, null),
                new NotificationProperties.Retry(true, Duration.ofMinutes(1), 3)
        );
    }
}
