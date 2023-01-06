SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;

SET search_path = public, pg_catalog;


create table edu_factual_term(
factual_term_id SERIAL,
factual_term_ident text NOT NULL,
factual_term_value text NOT NULL,
factual_term_synonyms text[],
factual_term_modified date,
PRIMARY KEY(factual_term_id)
);

CREATE OR REPLACE FUNCTION update_modified_column() 
RETURNS TRIGGER AS $$
BEGIN
NEW.factual_term_modified = now();
RETURN NEW; 
END;
$$ language 'plpgsql';

CREATE TRIGGER update_edu_factual_term_modtime BEFORE UPDATE ON edu_factual_term FOR EACH ROW EXECUTE PROCEDURE  update_modified_column();

