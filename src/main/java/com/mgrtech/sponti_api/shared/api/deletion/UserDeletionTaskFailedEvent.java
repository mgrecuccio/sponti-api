package com.mgrtech.sponti_api.shared.api.deletion;

public record UserDeletionTaskFailedEvent(
        Long userId,
        UserDeletionModule module,
        String reason
) {
}
