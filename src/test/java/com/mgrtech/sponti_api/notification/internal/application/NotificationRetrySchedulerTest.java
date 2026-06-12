package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.api.command.SendNotificationCommand;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationHistoryRepository;
import com.mgrtech.sponti_api.shared.observability.OperationalMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationRetrySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-06-09T08:00:00Z");

    private final NotificationHistoryRepository historyRepository = mock(NotificationHistoryRepository.class);
    private final NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);

    @Test
    public void skip_if_retry_disabled() {
        scheduler(notificationProperties(true, 3, false)).retryDueNotifications();

        verify(historyRepository, never()).findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(any(), any());
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    public void skip_if_no_notification_history_found() {
        scheduler(notificationProperties(true, 3, true)).retryDueNotifications();

        verify(historyRepository).findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(NotificationDeliveryStatus.RETRY_PENDING, NOW);
        verify(dispatcher, never()).dispatch(any(), any());
    }

    @Test
    public void mark_failed_if__notification_attempt_count_exceeded() {
        var notification = notificationHistory(NotificationType.MATCH_SUGGESTIONS_AVAILABLE);
        notification.markRetryPending("FAILURE_CODE", "FAILURE_REASON",  NOW, NOW.plus(Duration.ofMinutes(1)));

        when(historyRepository.findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(NotificationDeliveryStatus.RETRY_PENDING, NOW))
                .thenReturn(List.of(notification));

        scheduler(notificationProperties(true, 1, true)).retryDueNotifications();

        verify(historyRepository).findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(NotificationDeliveryStatus.RETRY_PENDING, NOW);
        verify(dispatcher, never()).dispatch(any(), any());

        assertThat(notification.getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);
    }

    @Test
    public void dispatch_match_proposal_suggestion_notification() {
        var notification = notificationHistory(NotificationType.MATCH_SUGGESTIONS_AVAILABLE);
        when(historyRepository.findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(NotificationDeliveryStatus.RETRY_PENDING, NOW))
                .thenReturn(List.of(notification));

        scheduler(notificationProperties(true, 3, true)).retryDueNotifications();


        verify(dispatcher).dispatch(notification, new SendNotificationCommand(
                42L,
                NotificationType.MATCH_SUGGESTIONS_AVAILABLE,
                "New suggestions available",
                "There may be useful suggestions available now.",
                Map.of()
        ));
    }

    @Test
    public void dispatch_match_proposal_creation_notification() {
        var notification = notificationHistory(NotificationType.MATCH_PROPOSAL_CREATED);
        when(historyRepository.findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(NotificationDeliveryStatus.RETRY_PENDING, NOW))
                .thenReturn(List.of(notification));

        scheduler(notificationProperties(true, 3, true)).retryDueNotifications();


        verify(dispatcher).dispatch(notification, new SendNotificationCommand(
                42L,
                NotificationType.MATCH_PROPOSAL_CREATED,
                "New match invitation",
                "Someone wants to connect now.",
                Map.of()
        ));
    }

    private NotificationRetryScheduler scheduler(NotificationProperties properties) {
        return new NotificationRetryScheduler(
                Clock.fixed(NOW, ZoneOffset.UTC),
                properties,
                historyRepository,
                dispatcher,
                new OperationalMetrics(new SimpleMeterRegistry())
        );
    }


    private NotificationProperties notificationProperties(boolean pushEnabled, int maxAttempts, boolean retryEnabled) {
        return new NotificationProperties(
                Duration.ofMinutes(30),
                pushEnabled,
                new NotificationProperties.Fcm(pushEnabled, null),
                new NotificationProperties.Retry(retryEnabled, Duration.ofMinutes(1), maxAttempts)
        );
    }

    private NotificationHistoryEntity notificationHistory(NotificationType type) {
        return new NotificationHistoryEntity(
                42L,
                type,
                24L,
                51L,
                NOW.minus(Duration.ofMinutes(5)),
                "metadata"
        );
    }

}
