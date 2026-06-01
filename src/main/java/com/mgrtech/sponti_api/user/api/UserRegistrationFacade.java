package com.mgrtech.sponti_api.user.api;

import com.mgrtech.sponti_api.user.api.command.CreateUserCommand;
import com.mgrtech.sponti_api.user.api.view.CreatedUserView;

public interface UserRegistrationFacade {

    CreatedUserView createUser(CreateUserCommand command);
}
