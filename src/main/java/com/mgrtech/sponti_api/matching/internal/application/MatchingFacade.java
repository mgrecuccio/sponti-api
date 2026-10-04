package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.ContactLinkView;
import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.matching.api.MatchView;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;

import java.util.List;

public interface MatchingFacade {

    List<SuggestedMatchView> getSuggestions(Long userId);

    List<MatchInvitationView> getIncomingMatches(Long userId);

    List<MatchInvitationView> getAcceptedMatches(Long userId);

    MatchView createMatch(Long userId, CreateMatchCommand command);

    MatchView acceptMatch(Long candidateUserId, Long proposalId);

    MatchView declineMatch(Long candidateUserId, Long proposalId);

    ContactLinkView createContactLink(Long matchId, Long userId);
}
