package com.mgrtech.sponti_api.notification.internal.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "sponti.notification.fcm", name = "enabled", havingValue = "true")
class FirebaseConfiguration {

    @Bean
    FirebaseApp firebaseApp(NotificationProperties properties) throws IOException {
        var credentialsPath = properties.fcm().credentialsPath();
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("sponti.notification.fcm.credentials-path must be configured when FCM is enabled.");
        }

        try (var inputStream = new FileInputStream(credentialsPath)) {
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(inputStream))
                    .build();

            return FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
