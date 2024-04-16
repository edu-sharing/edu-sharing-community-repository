ALTER TABLE edu_tracking_user
ADD COLUMN authority_organization VARCHAR(100)[],
ADD COLUMN authority_mediacenter VARCHAR(100)[];

/*
CREATE INDEX ON edu_tracking_user (authority_organization);
CREATE INDEX ON edu_tracking_user (authority_mediacenter);
*/

ALTER TABLE edu_tracking_node
ADD COLUMN authority_organization VARCHAR(100)[],
ADD COLUMN authority_mediacenter VARCHAR(100)[];

/*
CREATE INDEX ON edu_tracking_node (authority_organization);
CREATE INDEX ON edu_tracking_node (authority_mediacenter);
*/