package com.mgrtech.sponti_api.user.internal.web;

import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.UserProfileView;
import com.mgrtech.sponti_api.user.api.UserQueryFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "User endpoints")
public class UserController {

    private final UserQueryFacade userQueryFacade;

    UserController(UserQueryFacade userQueryFacade) {
        this.userQueryFacade = userQueryFacade;
    }

    @GetMapping("/me")
    @Operation(summary = "Get the profile of an authenticated user")
    UserProfileView me(Authentication authentication) {
        return userQueryFacade.getProfileById(extractUserId(authentication))
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if(principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }
}
