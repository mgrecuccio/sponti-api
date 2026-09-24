package com.mgrtech.sponti_api.user.api.event;

public record UserCreatedEvent(
        Long userId,
        String phoneNumber,
        String clientIp
) {
    public UserCreatedEvent(
            Long userId,
            String phoneNumber
    ) {
        this(userId, phoneNumber, null);
    }
}
