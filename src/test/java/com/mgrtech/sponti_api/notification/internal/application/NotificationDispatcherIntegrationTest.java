package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.FixedClockTestConfiguration;
import com.mgrtech.sponti_api.ModuleIntegrationTest;
import com.mgrtech.sponti_api.NotificationTestConfigurationProperties;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.delivery.PushNotificationSender;
import com.mgrtech.sponti_api.notification.internal.domain.*;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ModuleIntegrationTest
@Import({FixedClockTestConfiguration.class, NotificationTestConfigurationProperties.class})
class NotificationDispatcherIntegrationTest {

    @Autowired
    NotificationDeviceTokenRepository tokenRepository;

    @Autowired
    PushNotificationSender sender;

    @Autowired
    NotificationDispatcher dispatcher;

    @Test
    public void dispatch_notification_skips_deliver_if_token_not_found() {
        var notification = new NotificationHistoryEntity(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                24L,
                51L,
                Instant.parse("2026-03-30T08:00:00Z"),
                "metadata"
        );

        var command = new SendNotificationCommand(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestion",
                "There is a new suggestion",
                Map.of("data", "data")

        );

        dispatcher.dispatch(notification, command);

        assertThat(notification.getStatus()).isEqualTo(NotificationDeliveryStatus.NO_DEVICE_TOKEN);
        assertThat(notification.getFailureCode()).isEqualTo("NO_DEVICE_TOKEN");
        assertThat(notification.getFailureReason()).isEqualTo("No enabled device token found for user.");
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getAttemptCount()).isEqualTo(1);
    }

    @Test
    public void dispatch_notification_when_push_succeeds() {
        var notification = new NotificationHistoryEntity(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                24L,
                51L,
                Instant.parse("2026-06-09T08:00:00Z"),
                "metadata"
        );

        var token = new NotificationDeviceTokenEntity(
                42L,
                DevicePlatform.ANDROID,
                "token",
                "device-1",
                "1.O",
                Instant.now()
        );

        tokenRepository.save(token);

        var command = new SendNotificationCommand(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestion",
                "There is a new suggestion",
                Map.of("data", "data")

        );
        dispatcher.dispatch(notification, command);

        assertThat(notification.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(notification.getFailureCode()).isNull();
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notification.getNextRetryAt()).isNull();
        assertThat(notification.getProvider()).isEqualTo(NotificationProvider.FCM);
        assertThat(notification.getAttemptCount()).isEqualTo(1);
        assertThat(notification.getRelatedMatchId()).isEqualTo(51L);
        assertThat(notification.getUserId()).isEqualTo(42L);
        assertThat(notification.getRelatedUserId()).isEqualTo(24L);
        assertThat(notification.getProviderMessageId()).contains("noop:");
    }

}