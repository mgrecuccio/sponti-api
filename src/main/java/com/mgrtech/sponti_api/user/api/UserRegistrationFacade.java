package com.mgrtech.sponti_api.user.api;

public interface UserRegistrationFacade {

    CreatedUserView createUser(CreateUserCommand command);
}
