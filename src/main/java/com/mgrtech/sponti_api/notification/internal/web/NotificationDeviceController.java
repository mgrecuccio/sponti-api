package com.mgrtech.sponti_api.notification.internal.web;

import com.mgrtech.sponti_api.notification.internal.application.DeviceTokenApplicationService;
import com.mgrtech.sponti_api.notification.internal.application.command.RegisterDeviceTokenCommand;
import com.mgrtech.sponti_api.notification.internal.domain.DevicePlatform;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications/devices")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notifications", description = "Notification device token endpoints")
class NotificationDeviceController {

    private final DeviceTokenApplicationService service;

    NotificationDeviceController(DeviceTokenApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Register notification device token", description = "Mobile app registers or refreshes the current push notification token for the authenticated user.")
    void register(
            Authentication authentication,
            @Valid @RequestBody RegisterDeviceTokenRequest request
    ) {
        service.register(
                extractUserId(authentication),
                new RegisterDeviceTokenCommand(
                        request.platform(),
                        request.token(),
                        request.deviceId(),
                        request.appVersion()
                )
        );
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete notification device token", description = "Mobile app deletes the current push notification token during logout or token invalidation.")
    void delete(
            Authentication authentication,
            @Valid @RequestBody DeleteDeviceTokenRequest request
    ) {
        service.delete(extractUserId(authentication), request.token());
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if (principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    @Schema(description = "Register notification device token request payload")
    record RegisterDeviceTokenRequest(
            @NotNull @Schema(example = "ANDROID") DevicePlatform platform,
            @NotBlank @Schema(example = "token") String token,
            @Size(max = 120) @Schema(example = "device-1") String deviceId,
            @Size(max = 60) @Schema(example = "1.0") String appVersion
    ) {
    }

    @Schema(description = "Delete notification device token request payload")
    record DeleteDeviceTokenRequest(
            @NotBlank @Schema(example = "token") String token
    ) {
    }
}
