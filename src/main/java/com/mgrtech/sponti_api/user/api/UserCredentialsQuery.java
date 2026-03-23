package com.mgrtech.sponti_api.user.api;

import java.util.Optional;

public interface UserCredentialsQuery {

    Optional<UserCredentialsView> findByEmail(String email);

    Optional<UserCredentialsView> findById(Long id);
}
