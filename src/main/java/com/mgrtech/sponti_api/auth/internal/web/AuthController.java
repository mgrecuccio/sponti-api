package com.mgrtech.sponti_api.auth.internal.web;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.AuthTokens;
import com.mgrtech.sponti_api.auth.api.LoginCommand;
import com.mgrtech.sponti_api.auth.api.RegisterCommand;
import com.mgrtech.sponti_api.shared.validation.ValidE164PhoneNumber;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
class AuthController {

    private final AuthFacade authFacade;

    AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @PostMapping("/register")
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    AuthTokens register(@Valid @RequestBody RegisterRequest request) {
        return authFacade.register(
                new RegisterCommand(
                        request.email(),
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone()
                )
        );
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate user and return tokens")
    AuthTokens login(@Valid @RequestBody LoginRequest request) {
        return authFacade.login(new LoginCommand(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token")
    AuthTokens refresh(@Valid @RequestBody RefreshRequest request) {
        return authFacade.refresh(request.refreshToken);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout and revoke refresh tokens")
    void logout(Authentication authentication) {
        authFacade.logout(extractUserId(authentication));
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if (principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    @Schema(description = "Register request payload")
    record RegisterRequest(
            @Schema(example = "user@example.com") @NotBlank @Email String email,
            @Schema(example = "strongPassword") @NotBlank String password,
            @Schema(example = "nickname") @NotBlank String displayName,
            @Schema(example = "+32468009911") @ValidE164PhoneNumber String phoneNumber,
            @Schema(example = "Europe/Brussels")String timezone
    ) {
    }

    @Schema(description = "Login request payload")
    record LoginRequest(
            @Schema(example = "user@example.com") @NotBlank @Email String email,
            @Schema(example = "strongPassword") @NotBlank String password) {
    }

    @Schema(description = "Refresh token request payload")
    record RefreshRequest(
            @Schema(example = "opaque-refresh-token") @NotBlank String refreshToken
    ) {
    }
}
