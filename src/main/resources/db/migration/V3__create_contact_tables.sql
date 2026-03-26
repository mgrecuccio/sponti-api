create table contact_invitations (
    id bigserial primary key,
    sender_user_id bigserial not null,
    recipient_user_id bigserial not null,
    nickname varchar(100),
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    responded_at timestamp with time zone null,
    constraint chk_contact_invitations_sender_not_recipient
        check (sender_user_id <> recipient_user_id)
);

create index idx_contact_invitations_sender_user_id
    on contact_invitations (sender_user_id);

create index idx_contact_invitations_recipient_user_id
    on contact_invitations (recipient_user_id);

create index idx_contact_invitations_status
    on contact_invitations (status);


create table contact_relationships (
    id bigserial primary key,
    owner_user_id bigserial not null,
    contact_user_id bigserial not null,
    relationship_status varchar(20) not null,
    nickname varchar(100),
    favorite boolean not null default false,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint chk_contact_relationships_owner_not_contact
        check (owner_user_id <> contact_user_id),
    constraint uk_contact_relationship_owner_contact
        unique (owner_user_id, contact_user_id)
);

create index idx_contact_relationships_owner_user_id
    on contact_relationships (owner_user_id);

create index idx_contact_relationships_contact_user_id
    on contact_relationships (contact_user_id);

create index idx_contact_relationships_status
    on contact_relationships (relationship_status);