package com.mgrtech.sponti_api.user.api.query;

import com.mgrtech.sponti_api.user.api.view.UserCredentialsView;

import java.util.Optional;

public interface UserCredentialsQuery {

    Optional<UserCredentialsView> findByPhoneNumber(String phoneNumber);

    Optional<UserCredentialsView> findById(Long id);
}
