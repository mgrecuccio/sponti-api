package com.mgrtech.sponti_api.notification.internal.application;

import com.mgrtech.sponti_api.notification.internal.application.command.RegisterDeviceTokenCommand;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeviceTokenEntity;
import com.mgrtech.sponti_api.notification.internal.repository.NotificationDeviceTokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@AllArgsConstructor
public class DeviceTokenApplicationService {

    private final Clock clock;
    private final NotificationDeviceTokenRepository repository;

    @Transactional
    public void register(Long userId, RegisterDeviceTokenCommand command) {
        var now = Instant.now(clock);
        repository.findByToken(command.token())
                .ifPresentOrElse(
                        token -> token.refresh(
                                userId,
                                command.platform(),
                                command.deviceId(),
                                command.appVersion(),
                                now
                        ),
                        () -> repository.save(new NotificationDeviceTokenEntity(
                                userId,
                                command.platform(),
                                command.token(),
                                command.deviceId(),
                                command.appVersion(),
                                now
                        ))
                );
    }

    @Transactional
    public void delete(Long userId, String token) {
        var now = Instant.now(clock);
        repository.findByUserIdAndToken(userId, token)
                .ifPresent(deviceToken -> deviceToken.disable(now));
    }
}
