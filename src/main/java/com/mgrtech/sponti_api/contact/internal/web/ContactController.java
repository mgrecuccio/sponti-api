package com.mgrtech.sponti_api.contact.internal.web;

import com.mgrtech.sponti_api.contact.api.view.ContactView;
import com.mgrtech.sponti_api.contact.api.view.PendingContactInvitationView;
import com.mgrtech.sponti_api.contact.internal.application.ContactFacade;
import com.mgrtech.sponti_api.contact.internal.application.command.EditContactCommand;
import com.mgrtech.sponti_api.contact.internal.application.command.SendContactInvitationCommand;
import com.mgrtech.sponti_api.contact.internal.application.view.ContactInvitationView;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contact", description = "Contact endpoints")
class ContactController {

    private final ContactFacade contactFacade;

    public ContactController(ContactFacade contactFacade) {
        this.contactFacade = contactFacade;
    }

    @GetMapping
    @Operation(summary = "List accepted contacts", description = "Mobile contacts screen. Returns contacts accepted by the authenticated user.")
    public List<ContactView> getContacts(Authentication authentication) {
        var ownerUserId = extractUserId(authentication);
        return contactFacade.getAcceptedContacts(ownerUserId);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send contact invitation", description = "Invite a user by email. Conflict responses use CONTACT_INVITATION_ALREADY_EXISTS, CONTACT_ALREADY_EXISTS, or CONTACT_BLOCKED.")
    public ContactInvitationView sendInvitation(
            Authentication authentication,
            @Valid @RequestBody SendContactInvitationRequest request
    ) {
        var senderUserId = extractUserId(authentication);

        return contactFacade.sendInvitation(
                senderUserId,
                new SendContactInvitationCommand(request.email(), request.nickName())
        );
    }

    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancel sent invitation", description = "Sender cancels a pending invitation they created. Missing or non-pending invitations return CONTACT_INVITATION_NOT_FOUND.")
    public void cancelInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        var senderUserId = extractUserId(authentication);
        contactFacade.cancelInvitation(senderUserId, invitationId);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Accept incoming invitation", description = "Recipient accepts a pending incoming contact invitation and creates accepted contact relationships.")
    public void acceptInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        var recipientUserId = extractUserId(authentication);
        contactFacade.acceptInvitation(recipientUserId, invitationId);
    }

    @PostMapping("/invitations/{invitationId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reject incoming invitation", description = "Recipient rejects a pending incoming contact invitation. No contact relationship is created.")
    public void rejectInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        var recipientUserId = extractUserId(authentication);
        contactFacade.rejectInvitation(recipientUserId, invitationId);
    }

    @PostMapping("/{contactUserId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block contact", description = "Authenticated user blocks another user/contact.")
    public void blockContact(
            Authentication authentication,
            @PathVariable Long contactUserId
    ) {
        var ownerUserId = extractUserId(authentication);
        contactFacade.blockContact(ownerUserId, contactUserId);
    }

    @DeleteMapping("/{contactUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove contact", description = "Authenticated user removes a contact from their accepted contacts list.")
    public void removeContact(
            Authentication authentication,
            @PathVariable Long contactUserId
    ) {
        var ownerUserId = extractUserId(authentication);
        contactFacade.removeContact(ownerUserId, contactUserId);
    }

    @PutMapping("/{contactUserId}")
    @Operation(summary = "Edit contact", description = "Update the authenticated user's nickname and favorite flag for a contact.")
    public ContactView editContact(
            Authentication authentication,
            @PathVariable Long contactUserId,
            @Valid @RequestBody UpdateContactRequest request
    ) {
        var ownerUserId = extractUserId(authentication);
        return contactFacade.editContact(
                ownerUserId,
                contactUserId,
                new EditContactCommand(request.nickName(), request.favorite())
        );
    }

    @GetMapping("/invitations/pending")
    @Operation(summary = "List pending incoming invitations", description = "Mobile pending invitations screen. Returns only pending invitations addressed to the authenticated user.")
    public List<PendingContactInvitationView> getPendingInvitations(Authentication authentication) {
        var recipientUserId = extractUserId(authentication);
        return contactFacade.getPendingIncomingInvitations(recipientUserId);
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if(principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    @Schema(description = "Send Contact Invitation request payload")
    record SendContactInvitationRequest(
            @Schema(example = "user@example.com")
            @NotBlank
            @Email
            String email,

            @Schema(example = "nickName")
            @Size(max = 100)
            String nickName
    ) {}

    @Schema(description = "Update Contact request payload")
    record UpdateContactRequest(
            @Schema(example = "nickName")
            @NotNull
            @Size(max = 100)
            String nickName,

            @Schema(example = "true")
            Boolean favorite
    ) {}
}
