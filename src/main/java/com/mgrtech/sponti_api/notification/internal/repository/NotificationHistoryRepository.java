package com.mgrtech.sponti_api.notification.internal.repository;

import com.mgrtech.sponti_api.notification.api.NotificationType;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationDeliveryStatus;
import com.mgrtech.sponti_api.notification.internal.domain.NotificationHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistoryEntity, Long> {

    Optional<NotificationHistoryEntity> findFirstByUserIdAndTypeAndStatusOrderBySentAtDesc(
            Long userId,
            NotificationType type,
            NotificationDeliveryStatus status
    );

    List<NotificationHistoryEntity> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
            NotificationDeliveryStatus status,
            Instant now
    );
}
