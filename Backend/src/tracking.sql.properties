create table edu_tracking_node(
 node_id bigint,
 node_uuid varchar(36),
 authority varchar(100),
 time timestamp,
 type varchar(32),
 data json
);

create table edu_tracking_user(
 authority varchar(100),
 time timestamp,
 type varchar(32),
 data json
);

CREATE INDEX idxnode_id ON edu_tracking_node (node_id);
CREATE INDEX idxtype ON edu_tracking_node (type);
