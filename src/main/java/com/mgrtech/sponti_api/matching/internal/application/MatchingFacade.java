package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.ContactLinkView;
import com.mgrtech.sponti_api.matching.api.MatchView;
import com.mgrtech.sponti_api.matching.api.MatchingQuery;
import com.mgrtech.sponti_api.matching.internal.application.command.CreateMatchCommand;

public interface MatchingFacade extends MatchingQuery {

    MatchView createMatch(Long userId, CreateMatchCommand command);

    MatchView acceptMatch(Long candidateUserId, Long proposalId);

    MatchView declineMatch(Long candidateUserId, Long proposalId);

    ContactLinkView createContactLink(Long matchId, Long userId);
}
