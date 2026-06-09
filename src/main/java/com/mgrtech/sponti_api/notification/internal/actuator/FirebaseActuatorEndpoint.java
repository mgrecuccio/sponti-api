package com.mgrtech.sponti_api.notification.internal.actuator;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.mgrtech.sponti_api.notification.internal.configuration.NotificationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@Endpoint(id = "firebase")
class FirebaseActuatorEndpoint {

    private static final String HEALTHCHECK_TOPIC = "firebase-actuator-healthcheck";

    private final Clock clock;
    private final NotificationProperties properties;
    private final ObjectProvider<FirebaseApp> firebaseAppProvider;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    FirebaseActuatorEndpoint(
            Clock clock,
            NotificationProperties properties,
            ObjectProvider<FirebaseApp> firebaseAppProvider,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider
    ) {
        this.clock = clock;
        this.properties = properties;
        this.firebaseAppProvider = firebaseAppProvider;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    @ReadOperation
    FirebaseVerification firebase() {
        var checkedAt = Instant.now(clock);

        if (!properties.fcm().enabled()) {
            return FirebaseVerification.skipped(
                    false,
                    false,
                    null,
                    "FCM_DISABLED",
                    "sponti.notification.fcm.enabled is false.",
                    checkedAt
            );
        }

        var firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            return FirebaseVerification.skipped(
                    true,
                    false,
                    projectId(),
                    "FIREBASE_MESSAGING_UNAVAILABLE",
                    "FirebaseMessaging bean is not available.",
                    checkedAt
            );
        }

        try {
            var providerMessageId = firebaseMessaging.send(Message.builder()
                    .setTopic(HEALTHCHECK_TOPIC)
                    .putData("check", "true")
                    .build(), true);

            return FirebaseVerification.ok(projectId(), providerMessageId, checkedAt);
        } catch (FirebaseMessagingException exception) {
            return FirebaseVerification.failed(
                    true,
                    projectId(),
                    failureCode(exception),
                    exception.getMessage(),
                    checkedAt
            );
        } catch (RuntimeException exception) {
            return FirebaseVerification.failed(
                    true,
                    projectId(),
                    "FIREBASE_RUNTIME_ERROR",
                    exception.getMessage(),
                    checkedAt
            );
        }
    }

    private String projectId() {
        var firebaseApp = firebaseAppProvider.getIfAvailable();
        if (firebaseApp == null || firebaseApp.getOptions() == null) {
            return null;
        }

        return firebaseApp.getOptions().getProjectId();
    }

    private String failureCode(FirebaseMessagingException exception) {
        var messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode != null) {
            return messagingErrorCode.name();
        }

        return exception.getErrorCode() == null ? "FCM_ERROR" : exception.getErrorCode().name();
    }

    record FirebaseVerification(
            String status,
            boolean enabled,
            boolean initialized,
            String dryRun,
            String projectId,
            String providerMessageId,
            String failureCode,
            String failureReason,
            Instant checkedAt
    ) {

        static FirebaseVerification ok(String projectId, String providerMessageId, Instant checkedAt) {
            return new FirebaseVerification(
                    "UP",
                    true,
                    true,
                    "OK",
                    projectId,
                    providerMessageId,
                    null,
                    null,
                    checkedAt
            );
        }

        static FirebaseVerification skipped(
                boolean enabled,
                boolean initialized,
                String projectId,
                String failureCode,
                String failureReason,
                Instant checkedAt
        ) {
            return new FirebaseVerification(
                    "UNKNOWN",
                    enabled,
                    initialized,
                    "SKIPPED",
                    projectId,
                    null,
                    failureCode,
                    failureReason,
                    checkedAt
            );
        }

        static FirebaseVerification failed(
                boolean initialized,
                String projectId,
                String failureCode,
                String failureReason,
                Instant checkedAt
        ) {
            return new FirebaseVerification(
                    "DOWN",
                    true,
                    initialized,
                    "FAILED",
                    projectId,
                    null,
                    failureCode,
                    failureReason,
                    checkedAt
            );
        }
    }
}
