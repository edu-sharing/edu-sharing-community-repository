CREATE TABLE edu_share_info
(
    id           bigint generated always as identity primary key,
    node_id      varchar(36)  not null,
    shared_by    varchar(100),
    shared_with  varchar(100) not null,
    share_status smallint     not null,
    share_type   smallint     not null,
    timestamp    timestamp    not null
);

CREATE INDEX idx_edu_share_node_id ON edu_share_info (node_id);
CREATE INDEX idx_edu_share_timestamp_sharedWith ON edu_share_info (timestamp, shared_with);
CREATE INDEX idx_edu_share_shared_with_shared_by ON edu_share_info (shared_with, shared_by);


CREATE TABLE edu_share_info_oplog
(
    id        bigint generated always as identity primary key,
    share_id  bigint    not null,
    action    smallint  not null,
    timestamp timestamp not null
);

CREATE INDEX idx_edu_share_oplog_timestamp ON edu_share_info_oplog (timestamp);

CREATE UNIQUE INDEX edu_share_info_node_id_shared_by_shared_with_share_status_share
    ON edu_share_info (node_id, shared_by, shared_with, share_status, share_type)
    NULLS NOT DISTINCT;
