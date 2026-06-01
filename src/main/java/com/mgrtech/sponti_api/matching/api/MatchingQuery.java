package com.mgrtech.sponti_api.matching.api;

import java.util.List;

public interface MatchingQuery {

    List<SuggestedMatchView> getSuggestions(Long userId);

    List<MatchInvitationView> getIncomingMatches(Long userId);
}
