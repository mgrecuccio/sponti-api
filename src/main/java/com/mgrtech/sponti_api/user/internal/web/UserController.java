package com.mgrtech.sponti_api.user.internal.web;

import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "User endpoints")
public class UserController {

    private final UserProfileQuery userProfileQuery;
    private final UserFacade userFacade;

    UserController(
            UserProfileQuery userProfileQuery,
            UserFacade userFacade
    ) {
        this.userProfileQuery = userProfileQuery;
        this.userFacade = userFacade;
    }

    @PutMapping("/me")
    @Operation(summary = "Update the profile")
    UserProfileView update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        var userId = extractUserId(authentication);
        return userFacade.update(userId, new UpdateUserCommand(request.displayName, request.timezone));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the profile of an authenticated user")
    UserProfileView me(Authentication authentication) {
        return userProfileQuery.getProfileById(extractUserId(authentication))
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if(principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    @Schema(description = "Request to update the user profile")
    record UpdateProfileRequest(
            @Schema(example = "New display name") @NotBlank String displayName,
            @Schema(example = "Europe/Brussels") @NotBlank String timezone) {
    }
}
