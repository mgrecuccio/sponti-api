package com.mgrtech.sponti_api.matching.internal.web;

import com.mgrtech.sponti_api.matching.internal.application.MatchingFacade;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;
import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import com.mgrtech.sponti_api.matching.api.MatchView;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.shared.error.UnsupportedAuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/matches")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Matching", description = "Matching endpoints")
class MatchController {

    private final MatchingFacade matchingFacade;

    public MatchController(MatchingFacade matchingFacade) {
        this.matchingFacade = matchingFacade;
    }

    @GetMapping("/suggestions")
    @Operation(summary = "List match suggestions", description = "Mobile suggestions screen. Returns ranked currently available contacts for the authenticated user.")
    public List<SuggestedMatchView> suggest(Authentication authentication) {
        var userId = extractUserId(authentication);
        return matchingFacade.getSuggestions(userId);
    }

    @GetMapping("/incoming")
    @Operation(summary = "List incoming match invitations", description = "Mobile incoming matches screen. Returns active match proposals where the authenticated user is the candidate.")
    public List<MatchInvitationView> incoming(Authentication authentication) {
        var userId = extractUserId(authentication);
        return matchingFacade.getIncomingMatches(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create match proposal", description = "Create a match proposal for an accepted contact and communication channel.")
    public MatchView createMatch(
            Authentication authentication,
            @Valid @RequestBody CreateMatchRequest request
    ) {
        var initiatorUserId = extractUserId(authentication);
        return matchingFacade.createMatch(initiatorUserId,
                new CreateMatchCommand(request.candidateUserId(), request.channelType()));
    }

    @PatchMapping("/{id}/accept")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Accept match proposal", description = "Candidate accepts an incoming match proposal.")
    public MatchView acceptMatch(Authentication authentication, @PathVariable("id") Long proposalId) {
        var candidateUserId = extractUserId(authentication);
        return matchingFacade.acceptMatch(candidateUserId, proposalId);
    }

    @PatchMapping("/{id}/decline")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Decline match proposal", description = "Candidate declines an incoming match proposal.")
    public MatchView declineMatch(Authentication authentication, @PathVariable("id") Long proposalId) {
        var candidateUserId = extractUserId(authentication);
        return matchingFacade.declineMatch(candidateUserId, proposalId);
    }

    private Long extractUserId(Authentication authentication) {
        var principal = authentication.getPrincipal();

        if(principal instanceof String value) {
            return Long.valueOf(value);
        }

        throw new UnsupportedAuthenticationException("Unsupported authentication principal");
    }

    record CreateMatchRequest(
            @Schema(example = "1")
            @NotNull
            Long candidateUserId,

            @Schema(example = "CHAT")
            @NotNull
            ChannelType channelType
    ) { }
}
