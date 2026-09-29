-- the cleanup needs the age of an entry. reading it out of the jsonb would depend on the way jackson
-- serializes the instant and would have to detoast the whole document for every scanned row, so it gets
-- an own column. the value is written once with the row and never changes afterwards.
--
-- existing rows get 'epoch', meaning "creation time unknown". they are deliberately not dated back from
-- session_information->>'issuedAt': that would reintroduce the dependency on the json format this column
-- exists to avoid. 'epoch' only turns them into candidates for the next cleanup run - whether they are
-- really removed is decided by the session store, so entries of sessions that are still alive survive.
--
-- adding the column with a constant default does not rewrite the table (postgres 11+), the value for
-- existing rows is kept in the catalog. changing the default afterwards only affects new rows.
ALTER TABLE oidc_session_registry
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT 'epoch';

ALTER TABLE oidc_session_registry
    ALTER COLUMN created_at SET DEFAULT now();

CREATE INDEX IF NOT EXISTS idx_oidc_session_registry_created_at
    ON oidc_session_registry (created_at);

-- back channel logout looks the entries up by the idp session id (sid) or, if the logout token has none,
-- by the subject. without these indexes both are sequential scans over the whole table.
CREATE INDEX IF NOT EXISTS idx_oidc_session_registry_sid
    ON oidc_session_registry ((session_information->'claims'->>'sid'));

CREATE INDEX IF NOT EXISTS idx_oidc_session_registry_subject
    ON oidc_session_registry ((session_information->>'subject'));
