package com.mgrtech.sponti_api.user.api.event;

public record UserPhoneNumberChangedEvent(
        Long userId,
        String phoneNumber
) {
}
