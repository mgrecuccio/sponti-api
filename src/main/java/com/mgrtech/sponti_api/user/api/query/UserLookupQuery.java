package com.mgrtech.sponti_api.user.api.query;

import com.mgrtech.sponti_api.user.api.view.UserLookupView;

import java.util.Optional;

public interface UserLookupQuery {

    Optional<UserLookupView> findByPhoneNumberForLookup(String phoneNumber);
}
