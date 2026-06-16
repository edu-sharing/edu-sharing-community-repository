ALTER TABLE edu_factual_term
    ADD COLUMN factual_term_url varchar(200),
    ADD COLUMN factual_term_ml jsonb;