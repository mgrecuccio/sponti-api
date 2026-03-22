package com.mgrtech.sponti_api.matching.api;

import com.mgrtech.sponti_api.matching.api.dto.MatchCandidateView;

import java.util.List;

public interface MatchingFacade {

    List<MatchCandidateView> suggestMatches(Long userId);
}
