package com.mgrtech.sponti_api.user.api.query;

import java.util.Optional;

public interface UserContactInfoQuery {

    boolean hasPhoneNumber(Long userId);

    Optional<String> getPhoneNumber(Long userId);
}
