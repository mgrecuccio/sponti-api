CREATE UNIQUE INDEX uk_match_proposal_active_pair
ON match_proposals (
    LEAST(initiator_user_id, candidate_user_id),
    GREATEST(initiator_user_id, candidate_user_id)
)
WHERE status = 'PROPOSED';