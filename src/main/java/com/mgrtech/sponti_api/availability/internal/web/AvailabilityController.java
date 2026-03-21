package com.mgrtech.sponti_api.availability.internal.web;

import com.mgrtech.sponti_api.availability.api.AvailabilityFacade;
import com.mgrtech.sponti_api.availability.api.command.MarkFreeNowCommand;
import com.mgrtech.sponti_api.availability.api.dto.PresenceView;
import com.mgrtech.sponti_api.shared.ChannelType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/availability")
class AvailabilityController {

    private final AvailabilityFacade availabilityFacade;

    AvailabilityController(AvailabilityFacade availabilityFacade) {
        this.availabilityFacade = availabilityFacade;
    }

    @PostMapping("/free-now")
    PresenceView freeNow(@Valid @RequestBody FreeNowRequest request) {
        return availabilityFacade.markFreeNow(new MarkFreeNowCommand(
                request.userId(),
                request.channelType(),
                request.durationMinutes()));
    }

    record FreeNowRequest(@NotNull Long userId,
                          @NotNull ChannelType channelType,
                          @Min(5) @Max(180) int durationMinutes) {
    }
}
