package com.mgrtech.sponti_api.auth.internal.web;

import com.mgrtech.sponti_api.auth.api.AuthFacade;
import com.mgrtech.sponti_api.auth.api.command.LoginCommand;
import com.mgrtech.sponti_api.auth.api.command.RegisterCommand;
import com.mgrtech.sponti_api.auth.api.dto.AuthTokens;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;

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
        return authFacade.register(new RegisterCommand(request.email(), request.password(), request.displayName()));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate user and return tokens")
    AuthTokens login(@Valid @RequestBody LoginRequest request) {
        return authFacade.login(new LoginCommand(request.email(), request.password()));
    }

    record RegisterRequest(
            @Schema(example = "user@example.com") @NotBlank @Email String email,
            @Schema(example = "strongPassword") @NotBlank String password,
            @Schema(example = "nickname") @NotBlank String displayName) {
    }

    @Schema(description = "Login request payload")
    record LoginRequest(
            @Schema(example = "user@example.com") @NotBlank @Email String email,
            @Schema(example = "strongPassword") @NotBlank String password) {
    }
}
