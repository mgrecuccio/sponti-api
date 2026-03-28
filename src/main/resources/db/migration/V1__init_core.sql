create table if not exists users (
    id bigserial primary key,
    email varchar(255) not null unique,
    phone_number varchar(255) unique,
    phone_number_verified boolean not null default false,
    whats_app_opt_in boolean not null default false,
    phone_number_verified_at timestamp with time zone,
    password_hash varchar(255) not null,
    display_name varchar(225) not null,
    status varchar(50) not null,
    created_at timestamp with time zone not null,
    last_updated_at timestamp with time zone not null
);

create table if not exists user_preferences (
    id bigserial primary key,
    user_id bigint not null unique references users(id),
    allow_chat boolean not null default true,
    allow_call boolean not null default true,
    quiet_hours_start time,
    quiet_hours_end time
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