package com.mgrtech.sponti_api.notification.internal.delivery;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
class FcmPushNotificationSender implements PushNotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public PushDeliveryResult send(PushMessage message) {
        try {
            var providerMessageId = firebaseMessaging.send(Message.builder()
                    .setToken(message.token())
                    .setNotification(Notification.builder()
                            .setTitle(message.title())
                            .setBody(message.body())
                            .build())
                    .putAllData(message.data() == null ? java.util.Map.of() : message.data())
                    .putData("type", message.type().name())
                    .build());
            return PushDeliveryResult.success(providerMessageId);
        } catch (FirebaseMessagingException exception) {
            return PushDeliveryResult.failed(
                    failureCode(exception),
                    exception.getMessage(),
                    failureType(exception)
            );
        } catch (RuntimeException exception) {
            return PushDeliveryResult.failed(
                    "FCM_RUNTIME_ERROR",
                    exception.getMessage(),
                    PushFailureType.CONFIGURATION
            );
        }
    }

    private String failureCode(FirebaseMessagingException exception) {
        var messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode != null) {
            return messagingErrorCode.name();
        }

        return exception.getErrorCode() == null ? "FCM_ERROR" : exception.getErrorCode().name();
    }

    private PushFailureType failureType(FirebaseMessagingException exception) {
        var code = exception.getMessagingErrorCode();
        if (code == null) {
            return PushFailureType.CONFIGURATION;
        }

        return switch (code) {
            case UNREGISTERED, SENDER_ID_MISMATCH, INVALID_ARGUMENT -> PushFailureType.PERMANENT;
            case UNAVAILABLE, INTERNAL, QUOTA_EXCEEDED -> PushFailureType.TRANSIENT;
            case THIRD_PARTY_AUTH_ERROR -> PushFailureType.CONFIGURATION;
        };
    }
}
