SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;

-- Registry of managed contributors (authors/organizations) with persistent ids.
-- Independent of the media nodes: deleting a row here does NOT touch any media.
CREATE TABLE edu_contributor
(
    id           bigint generated always as identity primary key,
    kind         varchar(20)  not null, -- PERSON / ORGANIZATION
    title        varchar(255),
    givenname    varchar(255),
    surname      varchar(255),
    org          varchar(255),
    email        varchar(255),
    url          varchar(255),
    uid          varchar(255),
    orcid        varchar(255), -- X-ORCID
    gnduri       varchar(255), -- X-GND-URI
    ror          varchar(255), -- X-ROR
    wikidata     varchar(255), -- X-Wikidata
    vcard        text         not null, -- canonical vcard string
    created      timestamp    not null,
    last_updated timestamp    not null,
    -- at least one persistent id / mail must be set, otherwise the entry is not part of a person record
    CONSTRAINT edu_contributor_at_least_one_id CHECK (
        orcid IS NOT NULL OR gnduri IS NOT NULL OR ror IS NOT NULL OR wikidata IS NOT NULL OR email IS NOT NULL
    )
);

CREATE INDEX idx_edu_contributor_orcid ON edu_contributor (orcid);
CREATE INDEX idx_edu_contributor_gnduri ON edu_contributor (gnduri);
CREATE INDEX idx_edu_contributor_ror ON edu_contributor (ror);
CREATE INDEX idx_edu_contributor_wikidata ON edu_contributor (wikidata);
CREATE INDEX idx_edu_contributor_email ON edu_contributor (email);
CREATE INDEX idx_edu_contributor_kind ON edu_contributor (kind);
