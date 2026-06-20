package com.mgrtech.sponti_api.user.internal.web;

import com.mgrtech.sponti_api.shared.validation.ValidE164PhoneNumber;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import com.mgrtech.sponti_api.shared.error.UserNotFoundException;
import com.mgrtech.sponti_api.user.api.command.UpdatePreferencesCommand;
import com.mgrtech.sponti_api.user.api.command.UpdateUserCommand;
import com.mgrtech.sponti_api.user.api.query.UserMatchingPreferencesQuery;
import com.mgrtech.sponti_api.user.api.query.UserProfileQuery;
import com.mgrtech.sponti_api.user.api.view.UserMatchingPreferencesView;
import com.mgrtech.sponti_api.user.api.view.UserProfileView;
import com.mgrtech.sponti_api.user.internal.application.UserFacade;
import com.mgrtech.sponti_api.user.internal.application.UserPreferenceFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

import static com.mgrtech.sponti_api.shared.utils.StringUtils.blankToNull;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User", description = "User endpoints")
@AllArgsConstructor
public class UserController {

    private final UserProfileQuery userProfileQuery;
    private final UserMatchingPreferencesQuery userMatchingPreferencesQuery;
    private final UserFacade userFacade;
    private final UserPreferenceFacade userPreferenceFacade;

    @PutMapping("/me")
    @Operation(summary = "Update the profile")
    UserProfileView update(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        var userId = extractUserId(authentication);
        return userFacade.updateProfile(userId, new UpdateUserCommand(
                    request.displayName,
                    request.timezone,
                    blankToNull(request.phoneNumber)
                )
        );
    }

    @GetMapping("/me")
    @Operation(summary = "Get the profile of an authenticated user")
    UserProfileView me(Authentication authentication) {
        return userProfileQuery.getProfileById(extractUserId(authentication))
                .orElseThrow(() -> new UserNotFoundException("Authenticated user not found"));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update the user preferences")
    UserMatchingPreferencesView updatePreferences(
            Authentication authentication,
            @Valid @RequestBody UpdatePreferencesRequest request
    ) {
        var userId = extractUserId(authentication);
        return userPreferenceFacade.updatePreferences(userId, new UpdatePreferencesCommand(
                request.allowChat(),
                request.allowCall(),
                request.quietHoursStart(),
                request.quietHoursEnd(),
                request.pushNotificationsEnabled(),
                request.suggestionNotificationsEnabled()
        ));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get the user preferences")
    UserMatchingPreferencesView getPreferences(Authentication authentication) {
        var userId = extractUserId(authentication);
        return userMatchingPreferencesQuery.getMatchingPreferences(userId)
                .orElseThrow(() -> new UserNotFoundException("Authenticated user preferences not found"));
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
            @Schema(example = "Europe/Brussels") @NotBlank String timezone,
            @Schema(example = "+32468009911") @ValidE164PhoneNumber String phoneNumber
    ) {
    }

    @Schema(description = "Request to update user preferences")
    record UpdatePreferencesRequest(
            @Schema(example = "true") @NotNull Boolean allowChat,
            @Schema(example = "true") @NotNull Boolean allowCall,
            @Schema(example = "09:00:00") LocalTime quietHoursStart,
            @Schema(example = "11:00:00") LocalTime quietHoursEnd,
            @Schema(example = "true") @NotNull Boolean pushNotificationsEnabled,
            @Schema(example = "true") @NotNull Boolean suggestionNotificationsEnabled
    ) {
    }
}
