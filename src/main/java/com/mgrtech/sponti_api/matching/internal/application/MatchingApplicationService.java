package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.api.MatchingFacade;
import com.mgrtech.sponti_api.matching.api.dto.MatchCandidateView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class MatchingApplicationService implements MatchingFacade {

    @Override
    public List<MatchCandidateView> suggestMatches(Long userId) {
        return List.of();
    }
}
