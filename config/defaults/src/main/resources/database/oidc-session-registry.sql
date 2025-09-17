CREATE TABLE oidc_session_registry (
    session_id VARCHAR(255) PRIMARY KEY,
    session_information JSONB NOT NULL
);