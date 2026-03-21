create table if not exists users (
    id bigserial primary key,
    email varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(225) not null,
    status varchar(50) not null,
    created_at timestamptz not null,
    last_updated_at timestamptz not null
);

create table if not exists user_preferences (
    id bigserial primary key,
    user_id bigint not null unique references users(id),
    allow_chat boolean not null default true,
    allow_call boolean not null default true,
    quiet_hours_start time,
    quiet_hours_end time
);

create table if not exists contact_invitations (
    id bigserial primary key,
    sender_user_id bigint not null,
    recipient_user_id bigint not null,
    status varchar(50),
    created_at timestamptz not null
);

create table if not exists contact_relationships (
    id bigserial primary key,
    owner_user_id bigint not null,
    contact_user_id bigint not null,
    nickname varchar(255),
    favorite boolean not null default false,
    status varchar(50) not null,
    created_at timestamptz not null,
    last_updated_at timestamptz not null,
    constraint uk_contact_owner_contact unique (owner_user_id, contact_user_id)
);

create table if not exists availability_rules (
    id bigserial primary key,
    user_id bigint not null,
    day_of_week varchar(20) not null,
    start_time time not null,
    end_time time not null
);

create table if not exists availability_overrides (
    id bigserial primary key,
    user_id bigint not null,
    starts_at timestamptz not null,
    ends_at timestamptz not null,
    available boolean not null
);

create table if not exists event_publication (
    id UUID primary key,
    event_type varchar(512) not null,
    listener_id varchar(512) not null,
    publication_date timestamp not null,
    completion_date timestamp not null,
    serialized_event TEXT not null,
    status VARCHAR(50) not null,
    last_resubmission_date timestamp not null,
    completion_attempts integer not null
);