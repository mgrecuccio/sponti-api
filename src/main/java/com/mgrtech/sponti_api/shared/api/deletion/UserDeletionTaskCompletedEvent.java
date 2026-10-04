package com.mgrtech.sponti_api.shared.api.deletion;

public record UserDeletionTaskCompletedEvent(
        Long userId,
        UserDeletionModule module
) {
}
