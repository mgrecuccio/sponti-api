package com.mgrtech.sponti_api.matching.internal.domain;

import com.mgrtech.sponti_api.shared.api.ChannelType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchProposalEntityTest {

    private static final Long INITIATOR_USER_ID = 1L;
    private static final Long CANDIDATE_USER_ID = 2L;
    private static final Instant NOW = Instant.parse("2026-06-07T08:00:00Z");

    @Test
    void acceptBy_accepts_proposed_match_proposal_when_actor_is_candidate() {
        var proposal = proposed();

        proposal.acceptBy(CANDIDATE_USER_ID);

        assertThat(proposal.getStatus()).isEqualTo(MatchProposalStatus.ACCEPTED);
    }

    @Test
    void acceptBy_rejects_actor_that_is_not_candidate() {
        var proposal = proposed();

        assertThatThrownBy(() -> proposal.acceptBy(INITIATOR_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the candidate can change this match proposal");
    }

    @Test
    void declineBy_declines_proposed_match_proposal_when_actor_is_candidate() {
        var proposal = proposed();

        proposal.declineBy(CANDIDATE_USER_ID);

        assertThat(proposal.getStatus()).isEqualTo(MatchProposalStatus.DECLINED);
    }

    @Test
    void declineBy_rejects_actor_that_is_not_candidate() {
        var proposal = proposed();

        assertThatThrownBy(() -> proposal.declineBy(INITIATOR_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the candidate can change this match proposal");
    }

    @Test
    void cancelBy_cancels_proposed_match_proposal_when_actor_is_initiator() {
        var proposal = proposed();

        proposal.cancelBy(INITIATOR_USER_ID);

        assertThat(proposal.getStatus()).isEqualTo(MatchProposalStatus.CANCELLED);
    }

    @Test
    void cancelBy_rejects_actor_that_is_not_initiator() {
        var proposal = proposed();

        assertThatThrownBy(() -> proposal.cancelBy(CANDIDATE_USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only the initiator can cancel this match proposal");
    }

    @Test
    void expire_marks_due_proposed_match_proposal_as_expired() {
        var proposal = proposed(NOW.minus(Duration.ofSeconds(1)));

        proposal.expire(NOW);

        assertThat(proposal.getStatus()).isEqualTo(MatchProposalStatus.EXPIRED);
    }

    @Test
    void expire_rejects_match_proposal_that_is_not_due() {
        var proposal = proposed(NOW.plus(Duration.ofMinutes(1)));

        assertThatThrownBy(() -> proposal.expire(NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Match proposal is not expired");
    }

    @Test
    void state_transitions_reject_match_proposal_that_is_not_proposed() {
        var proposal = proposed();
        proposal.acceptBy(CANDIDATE_USER_ID);

        assertThatThrownBy(() -> proposal.declineBy(CANDIDATE_USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only proposed match proposals can be changed");
    }

    private MatchProposalEntity proposed() {
        return proposed(NOW.plus(Duration.ofMinutes(30)));
    }

    private MatchProposalEntity proposed(Instant expiresAt) {
        return new MatchProposalEntity(
                INITIATOR_USER_ID,
                CANDIDATE_USER_ID,
                ChannelType.CHAT,
                90,
                NOW,
                NOW.plus(Duration.ofHours(1)),
                expiresAt
        );
    }
}
