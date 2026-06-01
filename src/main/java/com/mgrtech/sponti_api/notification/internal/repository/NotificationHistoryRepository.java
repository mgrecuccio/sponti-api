package com.mgrtech.sponti_api.notification.internal.repository;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {

    Optional<NotificationHistoryEntity> findFirstByUserIdAndTypeOrderBySentAtDesc(
            Long userId,
            NotificationType type
    );
}
