package com.mgrtech.sponti_api.availability.api;

import com.mgrtech.sponti_api.availability.api.command.MarkFreeNowCommand;
import com.mgrtech.sponti_api.availability.api.dto.PresenceView;

public interface AvailabilityFacade {

    PresenceView markFreeNow(MarkFreeNowCommand command);
}
