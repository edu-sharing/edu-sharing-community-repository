ALTER TABLE edu_factual_term
    ADD COLUMN factual_term_url varchar(200),
    ADD COLUMN factual_term_locale_en text[],
    ADD COLUMN factual_term_locale_it text[],
    ADD COLUMN factual_term_locale_es text[],
    ADD COLUMN factual_term_locale_fr text[];