package com.mgrtech.sponti_api.contact.api.view;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Pending contact invitations for the authenticated user.")
public record PendingContactInvitationsView(
        @Schema(description = "Pending invitations addressed to the authenticated user.")
        List<PendingContactInvitationView> incoming,
        @Schema(description = "Pending invitations created by the authenticated user.")
        List<PendingSentContactInvitationView> sent
) {
}
