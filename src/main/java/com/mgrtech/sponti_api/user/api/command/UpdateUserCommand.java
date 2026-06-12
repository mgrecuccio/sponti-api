package com.mgrtech.sponti_api.user.api.command;

public record UpdateUserCommand(
        String displayName,
        String timezone
) {
}
