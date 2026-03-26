package com.mgrtech.sponti_api.contact.internal.web;

import com.mgrtech.sponti_api.contact.api.*;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Contact", description = "Contact endpoints")
public class ContactController {

    private final ContactFacade contactFacade;

    public ContactController(ContactFacade contactFacade) {
        this.contactFacade = contactFacade;
    }

    @GetMapping
    public List<ContactView> getContacts(Authentication authentication) {
        var ownerUserId = extractUserId(authentication);
        return contactFacade.getAcceptedContacts(ownerUserId);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
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

    @PostMapping("/invitations/{invitationId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptInvitation(
            Authentication authentication,
            @PathVariable Long invitationId
    ) {
        var recipientUserId = extractUserId(authentication);
        contactFacade.acceptInvitation(recipientUserId, invitationId);
    }

    @PostMapping("/{contactUserId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockContact(
            Authentication authentication,
            @PathVariable Long contactUserId
    ) {
        var ownerUserId = extractUserId(authentication);
        contactFacade.blockContact(ownerUserId, contactUserId);
    }

    @DeleteMapping("/{contactUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeContact(
            Authentication authentication,
            @PathVariable Long contactUserId
    ) {
        var ownerUserId = extractUserId(authentication);
        contactFacade.removeContact(ownerUserId, contactUserId);
    }

    @GetMapping("/invitations/pending")
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
}
