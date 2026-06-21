package com.mgrtech.sponti_api.matching.internal.repository;

import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalEntity;
import com.mgrtech.sponti_api.matching.internal.domain.MatchProposalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MatchProposalRepository extends JpaRepository<MatchProposalEntity, Long> {

    Optional<MatchProposalEntity> findByIdAndCandidateUserId(Long id, Long candidateUserId);

    /**
     * Finds matches visible to a user for a specific status.
     * Incoming proposals pass includeInitiated=false and requireUnexpired=true, so only active proposals
     * where the user is the candidate are returned. Accepted matches pass includeInitiated=true and
     * requireUnexpired=false, so either participant can see accepted matches after proposal expiry.
     */
    @Query("""
            SELECT proposal
            FROM MatchProposalEntity proposal
            WHERE proposal.status = :status
              AND (
                    proposal.candidateUserId = :userId
                    OR (:includeInitiated = true AND proposal.initiatorUserId = :userId)
                  )
              AND (
                    :requireUnexpired = false
                    OR proposal.expiresAt IS NULL
                    OR proposal.expiresAt > :now
                  )
            ORDER BY proposal.createdAt DESC
            """)
    List<MatchProposalEntity> findVisibleByUserIdAndStatus(
            Long userId,
            MatchProposalStatus status,
            Instant now,
            boolean includeInitiated,
            boolean requireUnexpired
    );

    @Query("""
            SELECT CASE WHEN COUNT(proposal) > 0 THEN true ELSE false END
            FROM MatchProposalEntity proposal
            WHERE (
                    (proposal.initiatorUserId = :userId AND proposal.candidateUserId = :candidateUserId)
                    OR (proposal.initiatorUserId = :candidateUserId AND proposal.candidateUserId = :userId)
                  )
              AND (
                    proposal.status = :acceptedStatus
                    OR (
                        proposal.status = :proposedStatus
                        AND (proposal.expiresAt IS NULL OR proposal.expiresAt > :now)
                    )
                  )
            """)
    boolean existsBlockingProposalBetweenUsers(
            Long userId,
            Long candidateUserId,
            MatchProposalStatus proposedStatus,
            MatchProposalStatus acceptedStatus,
            Instant now
    );

    Optional<MatchProposalEntity> findFirstByInitiatorUserIdAndCandidateUserIdAndStatusOrderByRespondedAtDesc(
            Long initiatorUserId,
            Long candidateUserId,
            MatchProposalStatus status
    );

    Optional<MatchProposalEntity> findFirstByInitiatorUserIdAndCandidateUserIdOrderByCreatedAtDesc(
            Long initiatorUserId,
            Long candidateUserId
    );
}
