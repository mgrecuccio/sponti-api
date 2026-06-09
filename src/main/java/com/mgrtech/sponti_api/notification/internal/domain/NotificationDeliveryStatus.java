package com.mgrtech.sponti_api.notification.internal.domain;

public enum NotificationDeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    RETRY_PENDING,
    SUPPRESSED,
    NO_DEVICE_TOKEN
}
