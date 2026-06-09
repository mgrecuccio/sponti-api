package com.mgrtech.sponti_api.notification.internal.delivery;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.mgrtech.sponti_api.notification.api.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FcmPushNotificationSenderTest {

    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final FcmPushNotificationSender sender = new FcmPushNotificationSender(messaging);

    @Test
    void sends_fcm_message_and_returns_successful_result() throws FirebaseMessagingException {
        when(messaging.send(any())).thenReturn("projects/sponti/messages/123");

        var result = sender.send(pushMessage(Map.of("matchId", "51")));

        var message = sentMessage();
        assertThat(field(message, "token")).isEqualTo("device-token");
        assertThat(field(message, "data")).isEqualTo(Map.of(
                "matchId", "51",
                "type", "MATCH_PROPOSAL_CREATED"
        ));
        assertThat(field(field(message, "notification"), "title")).isEqualTo("New match");
        assertThat(field(field(message, "notification"), "body")).isEqualTo("Someone invited you.");
        assertThat(result.success()).isTrue();
        assertThat(result.providerMessageId()).isEqualTo("projects/sponti/messages/123");
        assertThat(result.failureCode()).isNull();
        assertThat(result.failureReason()).isNull();
        assertThat(result.failureType()).isEqualTo(PushFailureType.NONE);
    }

    @Test
    void sends_fcm_message_with_only_type_data_when_message_data_is_null() throws FirebaseMessagingException {
        when(messaging.send(any())).thenReturn("provider-message-id");

        var result = sender.send(pushMessage(null));

        assertThat(field(sentMessage(), "data")).isEqualTo(Map.of("type", "MATCH_PROPOSAL_CREATED"));
        assertThat(result.success()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "UNREGISTERED,PERMANENT",
            "SENDER_ID_MISMATCH,PERMANENT",
            "INVALID_ARGUMENT,PERMANENT",
            "UNAVAILABLE,TRANSIENT",
            "INTERNAL,TRANSIENT",
            "QUOTA_EXCEEDED,TRANSIENT",
            "THIRD_PARTY_AUTH_ERROR,CONFIGURATION"
    })
    void maps_firebase_messaging_errors_to_push_failure_types(
            MessagingErrorCode messagingErrorCode,
            PushFailureType expectedFailureType
    ) throws FirebaseMessagingException {
        var exception = firebaseMessagingException(messagingErrorCode, ErrorCode.UNKNOWN, "FCM failed.");
        when(messaging.send(any())).thenThrow(exception);

        var result = sender.send(pushMessage(Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.providerMessageId()).isNull();
        assertThat(result.failureCode()).isEqualTo(messagingErrorCode.name());
        assertThat(result.failureReason()).isEqualTo("FCM failed.");
        assertThat(result.failureType()).isEqualTo(expectedFailureType);
    }

    @Test
    void uses_firebase_error_code_when_messaging_error_code_is_missing() throws FirebaseMessagingException {
        var exception = firebaseMessagingException(null, ErrorCode.UNAUTHENTICATED, "Credentials rejected.");
        when(messaging.send(any())).thenThrow(exception);

        var result = sender.send(pushMessage(Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("UNAUTHENTICATED");
        assertThat(result.failureReason()).isEqualTo("Credentials rejected.");
        assertThat(result.failureType()).isEqualTo(PushFailureType.CONFIGURATION);
    }

    @Test
    void returns_configuration_failure_when_firebase_client_throws_runtime_exception() throws FirebaseMessagingException {
        when(messaging.send(any())).thenThrow(new IllegalStateException("Firebase is not initialized."));

        var result = sender.send(pushMessage(Map.of()));

        assertThat(result.success()).isFalse();
        assertThat(result.failureCode()).isEqualTo("FCM_RUNTIME_ERROR");
        assertThat(result.failureReason()).isEqualTo("Firebase is not initialized.");
        assertThat(result.failureType()).isEqualTo(PushFailureType.CONFIGURATION);
    }

    private PushMessage pushMessage(Map<String, String> data) {
        return new PushMessage(
                "device-token",
                NotificationType.MATCH_PROPOSAL_CREATED,
                "New match",
                "Someone invited you.",
                data
        );
    }

    private Message sentMessage() throws FirebaseMessagingException {
        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messaging).send(captor.capture());
        return captor.getValue();
    }

    private FirebaseMessagingException firebaseMessagingException(
            MessagingErrorCode messagingErrorCode,
            ErrorCode errorCode,
            String message
    ) {
        var exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(messagingErrorCode);
        when(exception.getErrorCode()).thenReturn(errorCode);
        when(exception.getMessage()).thenReturn(message);
        return exception;
    }

    private Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not read field '%s' from %s".formatted(name, target.getClass()), exception);
        }
    }
}
