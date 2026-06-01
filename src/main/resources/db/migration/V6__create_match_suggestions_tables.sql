CREATE TABLE match_suggestions(
    id BIGSERIAL PRIMARY KEY,
    initiator_user_id BIGINT NOT NULL,
    candidate_user_id BIGINT NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    score INTEGER NOT NULL,
    overlap_start TIMESTAMP WITH TIME ZONE NOT NULL,
    overlap_end TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    responded_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_match_suggestions_initiator_status
    ON match_suggestions (initiator_user_id, status);

CREATE INDEX idx_match_suggestions_pair_created
    ON match_suggestions (initiator_user_id, candidate_user_id, created_at DESC);

CREATE INDEX idx_match_suggestions_candidate
    ON match_suggestions (candidate_user_id);
