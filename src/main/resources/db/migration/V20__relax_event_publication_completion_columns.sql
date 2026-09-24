alter table event_publication
    alter column completion_date drop not null;

alter table event_publication
    alter column last_resubmission_date drop not null;

alter table event_publication
    alter column completion_attempts set default 0;
