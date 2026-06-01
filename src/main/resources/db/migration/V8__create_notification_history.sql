CREATE TABLE notification_history(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(80) NOT NULL,
    related_user_id BIGINT,
    related_match_id BIGINT,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notification_history_user_type_sent
    ON notification_history (user_id, type, sent_at DESC);
