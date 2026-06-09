package com.mgrtech.sponti_api.notification.internal.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "sponti.notification.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
class NoOpPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushNotificationSender.class);

    @Override
    public PushDeliveryResult send(PushMessage message) {
        log.info("No-op push notification: token={} type={} title={}", message.token(), message.type(), message.title());
        return PushDeliveryResult.success("noop:" + Instant.now());
    }
}
