CREATE TABLE edu_dataprotection_queue
(
    "user"  varchar(100) not null UNIQUE,
    status  varchar(100) not null,
    requested  timestamp not null,
    node_id    varchar(36),
    finished  timestamp
);