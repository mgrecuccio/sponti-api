package com.mgrtech.sponti_api.availability.internal.web;

import com.mgrtech.sponti_api.availability.api.*;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityChannelType;
import com.mgrtech.sponti_api.availability.internal.domain.AvailabilityOverrideType;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;


@RestController
@RequestMapping("/api/v1/availability")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Availability", description = "Availability endpoints")
class AvailabilityController {

    private final AvailabilityFacade availabilityFacade;

    AvailabilityController(AvailabilityFacade availabilityFacade) {
        this.availabilityFacade = availabilityFacade;
    }

    @GetMapping("/rules")
    public List<AvailabilityRuleView> getRules(Authentication authentication) {
        var userId = extractUserId(authentication);
        return availabilityFacade.getRules(userId);
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityRuleView createRule(
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilityRuleRequest request
    ) {
        var userId = extractUserId(authentication);

        return availabilityFacade.createRule(
                userId,
                new CreateAvailabilityRuleCommand(
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime(),
                        request.channelType()
                )
        );
    }

    @PutMapping("/rules/{id}")
    public AvailabilityRuleView updateRule(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAvailabilityRuleRequest request
    ) {
        var userId = extractUserId(authentication);

        return availabilityFacade.updateRule(
                userId,
                id,
                new UpdateAvailabilityRuleCommand(
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime(),
                        request.channelType(),
                        request.enabled()
                )
        );
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(Authentication authentication, @PathVariable Long id) {
        var userId = extractUserId(authentication);
        availabilityFacade.deleteRule(userId, id);
    }

    @GetMapping("/overrides")
    public List<AvailabilityOverrideView> getOverrides(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endsAfter
    ) {
        var userId = extractUserId(authentication);
        return availabilityFacade.getOverrides(userId, endsAfter);
    }

    @PostMapping("/overrides")
    @ResponseStatus(HttpStatus.CREATED)
    public AvailabilityOverrideView createOverride(
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilityOverrideRequest request
    ) {
        var userId = extractUserId(authentication);

        return availabilityFacade.createOverride(
                userId,
                new CreateAvailabilityOverrideCommand(
                        request.startDateTime(),
                        request.endDateTime(),
                        request.type()
                )
        );
    }

    @GetMapping("/effective")
    public List<EffectiveAvailabilityView> getEffectiveAvailability(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        var userId = extractUserId(authentication);
        return availabilityFacade.getEffectiveAvailability(userId, from, to);
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if (principal instanceof String value) {
            return Long.valueOf(value);
        }
        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    @Schema(description = "Create Availability Rule request payload")
    record CreateAvailabilityRuleRequest(
            @Schema(example = "MONDAY") @NotNull DayOfWeek dayOfWeek,
            @Schema(example = "09:00:00") @NotNull LocalTime startTime,
            @Schema(example = "12:00:00") @NotNull LocalTime endTime,
            @Schema(example = "CHAT") @NotNull AvailabilityChannelType channelType
    ) {}

    @Schema(description = "Update Availability Rule request payload")
    record UpdateAvailabilityRuleRequest(
            @Schema(example = "MONDAY") @NotNull DayOfWeek dayOfWeek,
            @Schema(example = "09:00:00") @NotNull LocalTime startTime,
            @Schema(example = "12:00:00") @NotNull LocalTime endTime,
            @Schema(example = "CHAT") @NotNull AvailabilityChannelType channelType,
            @NotNull Boolean enabled
    ) {}

    @Schema(description = "Create Availability Override request payload")
    record CreateAvailabilityOverrideRequest(
            @Schema(example = "2026-03-28T13:00:00Z") @NotNull Instant startDateTime,
            @Schema(example = "2026-03-28T15:00:00Z") @NotNull Instant endDateTime,
            @Schema(example = "AVAILABLE") @NotNull AvailabilityOverrideType type
    ) {}

}
