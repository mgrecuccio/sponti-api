package com.mgrtech.sponti_api.matching.internal.application;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalStatus;
import com.mgrtech.sponti_api.matching.internal.exception.MatchProposalExpiredException;
import com.mgrtech.sponti_api.matching.internal.repository.MatchProposalRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
class MatchProposalExpirationService {

    private final MatchProposalRepository repository;

    @Transactional
    public void expireDueProposals(Instant now) {
        repository.expireDueProposals(
                MatchProposalStatus.PROPOSED,
                MatchProposalStatus.EXPIRED,
                now
        );
    }

    public void ensureNotExpired(MatchProposalEntity proposal, Instant now) {
        try {
            proposal.ensureNotExpired(now);
        } catch (MatchProposalExpiredException ex) {
            if (proposal.isProposed()) {
                proposal.expire(now);
            }
            throw ex;
        }
    }
}
