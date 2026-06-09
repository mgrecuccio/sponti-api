package com.mgrtech.sponti_api.notification.internal.repository;

import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceTokenEntity, Long> {

    Optional<NotificationDeviceTokenEntity> findByToken(String token);

    List<NotificationDeviceTokenEntity> findByUserIdAndEnabledTrue(Long userId);

    Optional<NotificationDeviceTokenEntity> findByUserIdAndToken(Long userId, String token);
}
