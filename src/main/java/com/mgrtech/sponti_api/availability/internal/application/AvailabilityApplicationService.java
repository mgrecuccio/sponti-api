package com.mgrtech.sponti_api.availability.internal.application;

import com.mgrtech.sponti_api.availability.api.AvailabilityFacade;
import com.mgrtech.sponti_api.availability.api.command.MarkFreeNowCommand;
import com.mgrtech.sponti_api.availability.api.dto.PresenceView;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
class AvailabilityApplicationService implements AvailabilityFacade {

    @Override
    public PresenceView markFreeNow(MarkFreeNowCommand command) {
        return new PresenceView(
                command.userId(),
                true,
                command.channelType(),
                OffsetDateTime.now().plusMinutes(command.durationMinutes()));
    }
}
