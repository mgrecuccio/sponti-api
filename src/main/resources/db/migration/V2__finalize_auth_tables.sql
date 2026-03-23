create unique index if not exists idx_users_email on users(email);

create table if not exists refresh_tokens (
    id bigserial primary key,
    user_id bigint not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    replaced_by_token_hash varchar(255) null,
    version bigint null
    );

create index if not exists idx_refresh_tokens_user_id on refresh_tokens(user_id);
create unique index if not exists idx_refresh_tokens_token_hash on refresh_tokens(token_hash);

alter table refresh_tokens
    add constraint fk_refresh_tokens_user
        foreign key (user_id) references users(id);