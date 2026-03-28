CREATE TABLE availability_rules (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    day_of_week VARCHAR(20) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    channel_type VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_availability_rules_user_id
    ON availability_rules(user_id);

CREATE TABLE availability_overrides (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_date_time TIMESTAMP WITH TIME ZONE NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_availability_overrides_user_id
    ON availability_overrides(user_id);

CREATE INDEX idx_availability_overrides_user_id_start_end
    ON availability_overrides(user_id, start_date_time, end_date_time);