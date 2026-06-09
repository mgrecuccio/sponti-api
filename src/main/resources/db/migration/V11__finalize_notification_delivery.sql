CREATE TABLE notification_device_tokens(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    token TEXT NOT NULL,
    device_id VARCHAR(120),
    app_version VARCHAR(60),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_notification_device_tokens_token UNIQUE (token),
    CONSTRAINT chk_notification_device_token_platform
        CHECK (platform IN ('IOS', 'ANDROID'))
);

CREATE INDEX idx_notification_device_tokens_user_enabled
    ON notification_device_tokens(user_id, enabled);

ALTER TABLE notification_history
    ADD COLUMN status VARCHAR(40) NOT NULL DEFAULT 'SENT',
    ADD COLUMN provider VARCHAR(40),
    ADD COLUMN provider_message_id VARCHAR(255),
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_attempt_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN next_retry_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN failure_code VARCHAR(120),
    ADD COLUMN failure_reason TEXT;

CREATE INDEX idx_notification_history_retry
    ON notification_history(status, next_retry_at);
