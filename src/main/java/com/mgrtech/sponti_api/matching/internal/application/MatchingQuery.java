package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.MatchInvitationView;
import com.mgrtech.sponti_api.matching.api.SuggestedMatchView;

import java.util.List;

public interface MatchingQuery {

    List<SuggestedMatchView> getSuggestions(Long userId);

    List<MatchInvitationView> getIncomingMatches(Long userId);

    List<MatchInvitationView> getAcceptedMatches(Long userId);
}
