package com.mgrtech.sponti_api.notification.internal.delivery;

public record PushDeliveryResult(
        boolean success,
        String providerMessageId,
        String failureCode,
        String failureReason,
        PushFailureType failureType
) {
    public static PushDeliveryResult success(String providerMessageId) {
        return new PushDeliveryResult(true, providerMessageId, null, null, PushFailureType.NONE);
    }

    public static PushDeliveryResult failed(
            String failureCode,
            String failureReason,
            PushFailureType failureType
    ) {
        return new PushDeliveryResult(false, null, failureCode, failureReason, failureType);
    }
}
