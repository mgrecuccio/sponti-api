alter table users
    add column deleted_at timestamp with time zone;

alter table users
    alter column phone_number drop not null;

create table user_deletion_tasks (
    id bigserial primary key,
    user_id bigint not null references users(id),
    module varchar(50) not null,
    status varchar(50) not null,
    error_message text,
    created_at timestamp with time zone not null,
    last_updated_at timestamp with time zone not null,
    completed_at timestamp with time zone,
    unique (user_id, module)
);

create index idx_user_deletion_tasks_user_status
    on user_deletion_tasks(user_id, status);
