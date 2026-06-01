ALTER TABLE match_suggestions
    ADD COLUMN expires_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_match_suggestions_candidate_status_expires
    ON match_suggestions (candidate_user_id, status, expires_at);
