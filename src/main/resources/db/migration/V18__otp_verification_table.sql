create table if not exists otp_verification_entity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone_number varchar(16) not null,
    purpose varchar(50) not null,
    attempts bigint null,
    status varchar(50) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    last_updated_at timestamp with time zone not null
);
