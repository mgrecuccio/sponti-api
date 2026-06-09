package com.mgrtech.sponti_api.notification.internal.delivery;

public interface PushNotificationSender {

    PushDeliveryResult send(PushMessage message);
}
