package com.mgrtech.sponti_api.matching.internal.domain;

import com.mgrtech.sponti_api.matching.internal.exception.MatchProposalExpiredException;
import com.mgrtech.sponti_api.matching.internal.exception.UserNotBelongsMatchException;
import com.mgrtech.sponti_api.shared.api.ChannelType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "match_proposals",
        indexes = {
                @Index(name = "idx_match_proposals_initiator_status", columnList = "initiator_user_id,status"),
                @Index(name = "idx_match_proposals_pair_created", columnList = "initiator_user_id,candidate_user_id,created_at"),
                @Index(name = "idx_match_proposals_candidate", columnList = "candidate_user_id")
        }
)
@NoArgsConstructor
@Getter
public class MatchProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "initiator_user_id", nullable = false)
    private Long initiatorUserId;

    @Column(name = "candidate_user_id", nullable = false)
    private Long candidateUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false, length = 20)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchProposalStatus status;

    @Column(nullable = false)
    private int score;

    @Column(name = "overlap_start", nullable = false)
    private Instant overlapStart;

    @Column(name = "overlap_end", nullable = false)
    private Instant overlapEnd;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    @UpdateTimestamp
    private Instant respondedAt;

    public MatchProposalEntity(
            Long initiatorUserId,
            Long candidateUserId,
            ChannelType channelType,
            int score,
            Instant overlapStart,
            Instant overlapEnd,
            Instant expiresAt
    ) {
        this.initiatorUserId = initiatorUserId;
        this.candidateUserId = candidateUserId;
        this.channelType = channelType;
        this.status = MatchProposalStatus.PROPOSED;
        this.score = score;
        this.overlapStart = overlapStart;
        this.overlapEnd = overlapEnd;
        this.expiresAt = expiresAt;
    }

    public MatchProposalEntity(
            Long initiatorUserId,
            Long candidateUserId,
            ChannelType channelType,
            int score,
            Instant overlapStart,
            Instant overlapEnd
    ) {
        this(initiatorUserId, candidateUserId, channelType, score, overlapStart, overlapEnd, null);
    }

    public void acceptBy(Long candidateUserId) {
        ensureCandidate(candidateUserId);
        ensureProposed();
        this.status = MatchProposalStatus.ACCEPTED;
    }

    public void declineBy(Long candidateUserId) {
        ensureCandidate(candidateUserId);
        ensureProposed();
        this.status = MatchProposalStatus.DECLINED;
    }

    public void expire(Instant now) {
        ensureProposed();
        if (expiresAt == null || expiresAt.isAfter(now)) {
            throw new IllegalStateException("Match proposal is not expired");
        }
        this.status = MatchProposalStatus.EXPIRED;
    }

    public void cancelBy(Long initiatorUserId) {
        ensureInitiator(initiatorUserId);
        ensureProposed();
        this.status = MatchProposalStatus.CANCELLED;
    }

    public boolean isProposed() {
        return status == MatchProposalStatus.PROPOSED;
    }

    public void ensureAccepted() {
        if (status != MatchProposalStatus.ACCEPTED) {
            throw new IllegalStateException("Only accepted match proposals can expose contact links.");
        }
    }

    public void ensureContactable() {
        ensureAccepted();
    }

    public void ensureNotExpired(Instant now) {
        if (expiresAt != null && !expiresAt.isAfter(now)) {
            throw new MatchProposalExpiredException("Match proposal has expired");
        }
    }

    public void ensureParticipant(Long userId) {
        if (!isParticipant(userId)) {
            throw new UserNotBelongsMatchException("UserId is not initiator nor candidate of this match.");
        }
    }

    public Long otherParticipantId(Long userId) {
        ensureParticipant(userId);
        return initiatorUserId.equals(userId) ? candidateUserId : initiatorUserId;
    }

    public boolean isParticipant(Long userId) {
        return initiatorUserId.equals(userId) || candidateUserId.equals(userId);
    }

    private void ensureProposed() {
        if (!isProposed()) {
            throw new IllegalStateException("Only proposed match proposals can be changed");
        }
    }

    private void ensureCandidate(Long candidateUserId) {
        if (!this.candidateUserId.equals(candidateUserId)) {
            throw new IllegalArgumentException("Only the candidate can change this match proposal");
        }
    }

    private void ensureInitiator(Long initiatorUserId) {
        if (!this.initiatorUserId.equals(initiatorUserId)) {
            throw new IllegalArgumentException("Only the initiator can cancel this match proposal");
        }
    }
}
