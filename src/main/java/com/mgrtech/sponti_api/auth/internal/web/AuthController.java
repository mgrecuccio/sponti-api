package com.mgrtech.sponti_api.auth.internal.web;

import com.mgrtech.sponti_api.auth.internal.application.AuthFacade;
import com.mgrtech.sponti_api.auth.internal.application.view.AuthTokens;
import com.mgrtech.sponti_api.auth.internal.application.command.*;
import com.mgrtech.sponti_api.shared.validation.ValidE164PhoneNumber;
import com.mgrtech.sponti_api.sms.api.view.VerificationResultView;
import com.mgrtech.sponti_api.sms.api.view.VerificationView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.mgrtech.sponti_api.shared.utils.AuthenticationUtils.extractUserId;
import static com.mgrtech.sponti_api.shared.utils.HttpRequestUtils.extractClientIp;

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
    AuthTokens register(HttpServletRequest servletRequest, @Valid @RequestBody RegisterRequest request) {
        return authFacade.register(
                new RegisterCommand(
                        request.password(),
                        request.displayName(),
                        request.phoneNumber(),
                        request.timezone(),
                        extractClientIp(servletRequest)
                )
        );
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Authenticate user and return tokens")
    AuthTokens login(@Valid @RequestBody LoginRequest request) {
        return authFacade.login(new LoginCommand(request.phoneNumber(), request.password()));
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

    @PostMapping("/change-password")
    @SecurityRequirement(name = "bearerAuth")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password")
    void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        authFacade.changePassword(new ChangePasswordCommand(
                extractUserId(authentication),
                request.currentPassword(),
                request.newPassword()
        ));
    }

    @PostMapping("/password-recovery")
    @SecurityRequirements
    @Operation(summary = "Password recovery")
    VerificationView recoverPassword(HttpServletRequest servletRequest, @Valid @RequestBody PasswordRecoveryRequest request) {
        return authFacade.recoverPassword(new RecoverPasswordCommand(
                request.phoneNumber(),
                extractClientIp(servletRequest)
        ));
    }

    @PostMapping("/verify-password-recovery")
    @SecurityRequirements
    @Operation(summary = "Verify password recovery OTP")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verifyPasswordRecoveryCode(@Valid @RequestBody PasswordRecoveryVerificationRequest request) {
         authFacade.verifyPasswordRecovery(
                new VerifyRecoveryPasswordCommand(
                        request.phoneNumber(),
                        request.verificationId(),
                        request.otpCode(),
                        request.newPassword()
                )
        );
    }

    @PostMapping("/verify-registration-phone")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify registration phone OTP")
    VerificationResultView verifyRegistrationPhone(
            Authentication authentication,
            @Valid @RequestBody VerifyPhoneRequest request
    ) {
        return authFacade.verifyRegistrationPhone(new VerifyRegistrationPhoneCommand(
                extractUserId(authentication),
                request.verificationId(),
                request.otpCode()
        ));
    }

    @DeleteMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    void deleteMe(Authentication authentication) {
        authFacade.deleteAuthenticatedUser(extractUserId(authentication));
    }

    @Schema(description = "Register request payload")
    record RegisterRequest(
            @Schema(example = "strongPassword") @NotBlank String password,
            @Schema(example = "nickname") @NotBlank String displayName,
            @Schema(example = "+32468009911") @NotBlank @ValidE164PhoneNumber String phoneNumber,
            @Schema(example = "Europe/Brussels")String timezone
    ) {
    }

    @Schema(description = "Login request payload")
    record LoginRequest(
            @Schema(example = "+32468009911") @NotBlank @ValidE164PhoneNumber String phoneNumber,
            @Schema(example = "strongPassword") @NotBlank String password) {
    }

    @Schema(description = "Password Recovery request payload")
    record PasswordRecoveryRequest(
            @Schema(example = "+32468009911") @NotBlank @ValidE164PhoneNumber String phoneNumber){
    }

    @Schema(description = "Password Recovery Verification request payload")
    record PasswordRecoveryVerificationRequest(
            @Schema(example = "+32468009911") @NotBlank @ValidE164PhoneNumber String phoneNumber,
            @Schema(example = "7bf82f3b-8767-4695-8834-c001ce56facf", nullable = true) String verificationId,
            @Schema(example = "123456") @NotBlank String otpCode,
            @Schema(example = "strongPassword") @NotBlank String newPassword){
    }

    @Schema(description = "Refresh token request payload")
    record RefreshRequest(
            @Schema(example = "opaque-refresh-token") @NotBlank String refreshToken
    ) {
    }

    @Schema(description = "Change password request payload")
    record ChangePasswordRequest(
            @Schema(example = "currentPassword") @NotBlank String currentPassword,
            @Schema(example = "newStrongPassword") @NotBlank String newPassword
    ) {
    }

    @Schema(description = "Verify phone OTP request payload")
    record VerifyPhoneRequest(
            @Schema(example = "123456") @NotBlank String otpCode,
            @Schema(example = "7bf82f3b-8767-4695-8834-c001ce56facf", nullable = true) String verificationId
    ) {
    }
}
