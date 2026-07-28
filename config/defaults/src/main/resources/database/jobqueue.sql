create table public.edu_job_queue
(
    id           bigint generated always as identity
        constraint edu_job_queue_pk
            primary key,
    is_unique    boolean             not null,
    job_group    varchar(50)         not null,
    requested    timestamp           not null,
    last_updated timestamp,
    status       smallint            not null,
    bean         varchar             not null,
    method       varchar             not null,
    params       character varying[] not null,
    user_name    varchar             not null,
    job_hash     integer             not null,
    param_types  character varying[] not null,
    ttl          interval            not null
);

create unique index edu_job_queue_job_hash_uindex
    on public.edu_job_queue (job_hash)
    where (is_unique = true);

create index edu_job_queue_job_group_status_index
    on public.edu_job_queue (job_group, status);

create index edu_job_queue_status_index
    on public.edu_job_queue (status);

create index edu_job_queue_requested_id_index
    on public.edu_job_queue (requested, id);

