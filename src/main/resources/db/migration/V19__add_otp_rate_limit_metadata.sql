alter table otp_verification_entity
    add column if not exists user_id bigint,
    add column if not exists client_ip varchar(45);

create index if not exists idx_otp_verification_user_rate_limit
    on otp_verification_entity (user_id, purpose, created_at);

create index if not exists idx_otp_verification_client_ip_rate_limit
    on otp_verification_entity (client_ip, purpose, created_at);
