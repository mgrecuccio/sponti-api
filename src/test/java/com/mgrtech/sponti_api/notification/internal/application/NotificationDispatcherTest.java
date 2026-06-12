package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.delivery.PushDeliveryResult;
import com.mgrtech.sponti_api.notification.internal.delivery.PushFailureType;
import com.mgrtech.sponti_api.notification.internal.delivery.PushMessage;
import com.mgrtech.sponti_api.notification.internal.delivery.PushNotificationSender;
import com.mgrtech.sponti_api.notification.internal.domain.DevicePlatform;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeviceTokenEntity;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationProvider;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-06-09T08:00:00Z");

    private final NotificationDeviceTokenRepository tokenRepository = mock(NotificationDeviceTokenRepository.class);
    private final PushNotificationSender sender = mock(PushNotificationSender.class);

    @Test
    void marksNotificationSentWithoutLoadingTokensWhenPushIsDisabled() {
        var history = notificationHistory();
        var dispatcher = dispatcher(notificationProperties(false, 3));

        dispatcher.dispatch(history, command());

        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(history.getProvider()).isEqualTo(NotificationProvider.FCM);
        assertThat(history.getProviderMessageId()).isEqualTo("push-disabled:" + NOW);
        assertThat(history.getAttemptCount()).isEqualTo(1);
        assertThat(history.getLastAttemptAt()).isEqualTo(NOW);
        verifyNoInteractions(tokenRepository, sender);
    }

    @Test
    void marksNotificationNoDeviceTokenWhenUserHasNoEnabledTokens() {
        var history = notificationHistory();
        var dispatcher = dispatcher(notificationProperties(true, 3));
        when(tokenRepository.findByUserIdAndEnabledTrue(42L)).thenReturn(List.of());

        dispatcher.dispatch(history, command());

        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.NO_DEVICE_TOKEN);
        assertThat(history.getFailureCode()).isEqualTo("NO_DEVICE_TOKEN");
        assertThat(history.getFailureReason()).isEqualTo("No enabled device token found for user.");
        assertThat(history.getNextRetryAt()).isNull();
        assertThat(history.getAttemptCount()).isEqualTo(1);
        verifyNoInteractions(sender);
    }

    @Test
    void sendsPushMessageAndMarksNotificationSentWhenProviderSucceeds() {
        var history = notificationHistory();
        var token = deviceToken("device-token");
        var dispatcher = dispatcher(notificationProperties(true, 3));
        when(tokenRepository.findByUserIdAndEnabledTrue(42L)).thenReturn(List.of(token));
        when(sender.send(any())).thenReturn(PushDeliveryResult.success("provider-message-1"));

        dispatcher.dispatch(history, command());

        var messageCaptor = ArgumentCaptor.forClass(PushMessage.class);
        verify(sender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isEqualTo(new PushMessage(
                "device-token",
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions",
                "You have new people to reconnect with.",
                Map.of("targetScreen", "suggestions")
        ));
        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(history.getProvider()).isEqualTo(NotificationProvider.FCM);
        assertThat(history.getProviderMessageId()).isEqualTo("provider-message-1");
        assertThat(history.getFailureCode()).isNull();
        assertThat(history.getFailureReason()).isNull();
        assertThat(history.getNextRetryAt()).isNull();
        assertThat(history.getAttemptCount()).isEqualTo(1);
        assertThat(history.getUserId()).isEqualTo(42L);
        assertThat(history.getRelatedUserId()).isEqualTo(24L);
        assertThat(history.getRelatedMatchId()).isEqualTo(51L);
    }

    @Test
    void marksRetryPendingWhenTransientFailureOccursBeforeMaxAttempts() {
        var history = notificationHistory();
        var dispatcher = dispatcher(notificationProperties(true, 3));
        when(tokenRepository.findByUserIdAndEnabledTrue(42L)).thenReturn(List.of(deviceToken("device-token")));
        when(sender.send(any())).thenReturn(PushDeliveryResult.failed(
                "UNAVAILABLE",
                "Provider temporarily unavailable.",
                PushFailureType.TRANSIENT
        ));

        dispatcher.dispatch(history, command());

        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.RETRY_PENDING);
        assertThat(history.getFailureCode()).isEqualTo("UNAVAILABLE");
        assertThat(history.getFailureReason()).isEqualTo("Provider temporarily unavailable.");
        assertThat(history.getLastAttemptAt()).isEqualTo(NOW);
        assertThat(history.getNextRetryAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
        assertThat(history.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void marksFailedWhenTransientFailureOccursAtMaxAttempts() {
        var history = notificationHistory();
        history.markRetryPending("UNAVAILABLE", "Previous failure.", NOW.minusSeconds(60), NOW);
        history.markRetryPending("UNAVAILABLE", "Previous failure.", NOW.minusSeconds(30), NOW);
        var dispatcher = dispatcher(notificationProperties(true, 2));
        when(tokenRepository.findByUserIdAndEnabledTrue(42L)).thenReturn(List.of(deviceToken("device-token")));
        when(sender.send(any())).thenReturn(PushDeliveryResult.failed(
                "UNAVAILABLE",
                "Provider temporarily unavailable.",
                PushFailureType.TRANSIENT
        ));

        dispatcher.dispatch(history, command());

        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(history.getFailureCode()).isEqualTo("UNAVAILABLE");
        assertThat(history.getFailureReason()).isEqualTo("Provider temporarily unavailable.");
        assertThat(history.getNextRetryAt()).isNull();
        assertThat(history.getAttemptCount()).isEqualTo(3);
    }

    @Test
    void disablesTokenAndMarksFailedWhenProviderReportsPermanentFailure() {
        var history = notificationHistory();
        var token = deviceToken("device-token");
        var dispatcher = dispatcher(notificationProperties(true, 3));
        when(tokenRepository.findByUserIdAndEnabledTrue(42L)).thenReturn(List.of(token));
        when(sender.send(any())).thenReturn(PushDeliveryResult.failed(
                "UNREGISTERED",
                "Token is no longer registered.",
                PushFailureType.PERMANENT
        ));

        dispatcher.dispatch(history, command());

        assertThat(token.isEnabled()).isFalse();
        assertThat(token.getUpdatedAt()).isEqualTo(NOW);
        assertThat(history.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
        assertThat(history.getFailureCode()).isEqualTo("UNREGISTERED");
        assertThat(history.getFailureReason()).isEqualTo("Token is no longer registered.");
        assertThat(history.getNextRetryAt()).isNull();
        assertThat(history.getAttemptCount()).isEqualTo(1);
    }

    private NotificationDispatcher dispatcher(NotificationProperties properties) {
        return new NotificationDispatcher(
                Clock.fixed(NOW, ZoneOffset.UTC),
                properties,
                tokenRepository,
                sender,
                new OperationalMetrics(new SimpleMeterRegistry())
        );
    }

    private NotificationProperties notificationProperties(boolean pushEnabled, int maxAttempts) {
        return new NotificationProperties(
                Duration.ofMinutes(30),
                pushEnabled,
                new NotificationProperties.Fcm(pushEnabled, null),
                new NotificationProperties.Retry(true, Duration.ofMinutes(1), maxAttempts)
        );
    }

    private NotificationHistoryEntity notificationHistory() {
        return new NotificationHistoryEntity(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                24L,
                51L,
                NOW.minus(Duration.ofMinutes(5)),
                "metadata"
        );
    }

    private NotificationDeviceTokenEntity deviceToken(String token) {
        return new NotificationDeviceTokenEntity(
                42L,
                DevicePlatform.ANDROID,
                token,
                "device-1",
                "1.0",
                NOW.minus(Duration.ofDays(1))
        );
    }

    private SendNotificationCommand command() {
        return new SendNotificationCommand(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions",
                "You have new people to reconnect with.",
                Map.of("targetScreen", "suggestions")
        );
    }
}
