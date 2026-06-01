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

    @Query("""
            SELECT proposal
            FROM MatchProposalEntity proposal
            WHERE proposal.candidateUserId = :candidateUserId
              AND proposal.status = :status
              AND (proposal.expiresAt IS NULL OR proposal.expiresAt > :now)
            ORDER BY proposal.createdAt DESC
            """)
    List<MatchProposalEntity> findActiveIncoming(
            Long candidateUserId,
            MatchProposalStatus status,
            Instant now
    );

    boolean existsByInitiatorUserIdAndCandidateUserIdAndStatus(
            Long initiatorUserId,
            Long candidateUserId,
            MatchProposalStatus status
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
