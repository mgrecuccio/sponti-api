package com.mgrtech.sponti_api.user.internal.domain;

public enum UserStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    SUSPENDED,
    DELETION_REQUESTED,
    DELETING,
    DELETED
}
