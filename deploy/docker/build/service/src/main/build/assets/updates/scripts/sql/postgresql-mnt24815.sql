----------------------------------------------------------------------------------------------------------------
-- Date:        December 2024
-- Author:      Tiago Salvado
-- Reviewer:    Eva Vasques
-- Description:
--
-- Fix problem introduced by MNT-24137, the CrCHelper logic has been changed in ACS 23.3.2 and 23.4.0 causing
-- duplicated records in the database tables below:
--
--    - alf_prop_class
--    - alf_prop_string_value
--    - alf_prop_value
--    - alf_prop_unique_ctx
--    - alf_prop_link
--    - alf_audit_app
--    - alf_audit_entry
--
-- The goal of this SQL script is to fix the duplicated records and update all the references accordingly.
----------------------------------------------------------------------------------------------------------------

----------------------------------------------------------
-- alf_prop_class
----------------------------------------------------------
--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_class_duplicated_temp
--
-- Description:
--
--        Contains the mapping between property classes that have been duplicated after migration. The goal
--        is to maintain the correspondence between the class id used before migration and the id created after.
--
-- Example:
--
--        Given 'alf_prop_class' table (after migration):
--
--        -------------------------------------------------------------------------------------
--        | id  | java_class_name        | java_class_name_short     | java_class_name_crc    |
--        -------------------------------------------------------------------------------------
--        | 1   | "java.lang.String"     | "java.lang.string"        | 2004016611             |
--        | 21  | "java.lang.String"     | "java.lang.String"        | 2004016611             |
--        -------------------------------------------------------------------------------------
--
--        The table 'alf_prop_class_duplicated_temp' will be created as follows:
--
--        -------------------------------------------------------------------------------
--        | java_class_name       | before_problem_type_id    | after_problem_type_id   |
--        -------------------------------------------------------------------------------
--        |  "java.lang.String"   | 1                         | 21                      |
--        -------------------------------------------------------------------------------
--
CREATE TABLE alf_prop_class_duplicated_temp AS
SELECT *
FROM (
         SELECT
             java_class_name,
             (
                 SELECT MIN(id)
                 FROM alf_prop_class p
                 WHERE
                     t.java_class_name = p.java_class_name
             ) AS before_problem_type_id,
             (
                 SELECT MAX(id)
                 FROM alf_prop_class p
                 WHERE
                     t.java_class_name = p.java_class_name
             ) AS after_problem_type_id
         FROM alf_prop_class t
         GROUP BY
             1
     ) alf_prop_class_before_after_ids
WHERE
    before_problem_type_id <> after_problem_type_id;

----------------------------------------------------------
-- alf_prop_string_value
----------------------------------------------------------

--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_string_value_duplicated_temp
--
-- Description:
--
--        Contains the mapping between property string values that have been duplicated after migration. The goal
--        is to maintain the correspondence between the property string value id used before migration and the id created after.
--
-- Example:
--
--        Given 'alf_prop_string_value' table (after migration):
--
--        ------------------------------------------------------------------
--        | id    | string_value    | string_end_lower     | string_crc    |
--        ------------------------------------------------------------------
--        | 37    | ".sharedIds"    | ".sharedids"         | 1096111251    |
--        | 918   | ".sharedIds"    | ".sharedIds"         | 1096111251    |
--        ------------------------------------------------------------------
--
--
--        The table 'alf_prop_string_value_duplicated_temp' will be created as follows:
--
--        ---------------------------------------------------------------------------------------
--        | string_value    | before_problem_sv_id    | after_problem_sv_id    | crc            |
--        ---------------------------------------------------------------------------------------
--        | ".sharedIds"    | 37                      | 918                    | 1096111251     |
--        ---------------------------------------------------------------------------------------
--
CREATE TABLE alf_prop_string_value_duplicated_temp AS
SELECT
    string_value,
    before_problem_sv_id,
    after_problem_sv_id,
    string_crc
FROM (
         SELECT
             string_value,
             string_crc,
             COUNT(id) AS duplicates,
             MIN(id) AS before_problem_sv_id,
             MAX(id) AS after_problem_sv_id
         FROM alf_prop_string_value t
         GROUP BY
             1,
             2
     ) alf_prop_string_value_before_after_ids
WHERE
    duplicates > 1;

--
-- Type:    UPDATE
--
-- Table:   alf_prop_string_value
--
-- Description:
--
--        Updates all records where 'string_end_lower' column is not lowercase
--
--
UPDATE alf_prop_string_value
SET
    string_end_lower = LOWER(string_end_lower)
WHERE
    id IN (
        SELECT id
        FROM alf_prop_string_value
        WHERE
            LOWER(string_end_lower) != string_end_lower
  AND string_value NOT IN (
    SELECT string_value
    FROM
    alf_prop_string_value_duplicated_temp
    )
    );

----------------------------------------------------------
-- alf_prop_value
----------------------------------------------------------

--
-- Type:    UPDATE

-- Table:   alf_prop_value
--
-- Description:
--
--        Fix duplicated string values by replacing the 'long_value' column with the
--        original 'alf_prop_string_value.id' (before migration)
--
UPDATE alf_prop_value
SET
    long_value = apsvdt.before_problem_sv_id
    FROM
    alf_prop_string_value_duplicated_temp apsvdt
WHERE
    long_value = apsvdt.after_problem_sv_id
  AND actual_type_id IN (
    SELECT MAX(id)
    FROM alf_prop_class apc
    WHERE
    java_class_name = 'java.lang.String'
    );

--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_value_duplicated_temp
--
-- Description:
--
--        Contains the records from 'alf_prop_value' table that cannot have the 'actual_type_id' column
--        updated with value from 'alf_prop_class_duplicated_temp.before_problem_type_id', otherwise, the
--         index 'idx_alf_propv_act' (actual_type_id, long_value) would be violated since it is not possible
--         to have duplicated combinations.
--
--        The goal is to have a correspondence between property values that have been duplicated after
--        migration and the correspondent prop value that existed prior to migration. This way all the tables
--        that reference 'alf_prop_value.id' column can be updated accordingly.
--
-- Example:
--
--        Given 'alf_prop_value' table (after migration):
--
--        ----------------------------------------------------------
--        | id   | actual_type_id | persisted_type | long_value    |
--        ----------------------------------------------------------
--        | 2    | 1              | 3              | 2             |
--        ----------------------------------------------------------
--        | 73   | 1              | 3              | 59            |
--        ----------------------------------------------------------
--        | 82   | 1              | 3              | 65            |
--        ----------------------------------------------------------
--        | 878  | 21             | 3              | 2             |
--        ----------------------------------------------------------
--        | 883  | 21             | 3              | 59            |
--        ----------------------------------------------------------
--        | 914  | 21             | 3              | 65            |
--        ----------------------------------------------------------
--
--        The table 'alf_prop_value_duplicated_temp' will be created as follows:
--
--        ----------------------------------------------------------------------------------------------------------------------
--        | after_problem_id  | before_problem_id   | actual_type_id    | actual_type_id_before_problem     |    long_value    |
--        ----------------------------------------------------------------------------------------------------------------------
--        | 878               | 2                   | 21                | 1                                 |                  |
--        ----------------------------------------------------------------------------------------------------------------------
--        | 883               | 73                  | 21                | 1                                 |                  |
--        ----------------------------------------------------------------------------------------------------------------------
--        | 914               | 82                  | 21                | 1                                 |                  |
--        ----------------------------------------------------------------------------------------------------------------------
--
CREATE TABLE alf_prop_value_duplicated_temp AS
SELECT
    apv.id after_problem_id,
    (
        SELECT id
        FROM alf_prop_value apv2
        WHERE
            apv2.actual_type_id = apcdt.before_problem_type_id
          AND apv2.long_value = apv.long_value
    ) before_problem_id,
    apv.actual_type_id,
    apcdt.before_problem_type_id actual_type_id_before_problem,
    apv.long_value
FROM
    alf_prop_value apv,
    alf_prop_class_duplicated_temp apcdt
WHERE
    apv.actual_type_id = apcdt.after_problem_type_id
  AND (
       apcdt.before_problem_type_id,
       apv.long_value
          ) IN (
          SELECT actual_type_id, long_value
          FROM alf_prop_value
      );

--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_value_not_duplicated_temp
--
-- Description:
--
--        Contains the records from 'alf_prop_value' table that can have the 'actual_type_id' column updated with
--        value from 'alf_prop_class_duplicated_temp.before_problem_type_id' which means these records were not
--        duplicated after migration.
--
CREATE TABLE alf_prop_value_not_duplicated_temp AS
SELECT
    apv.id after_problem_id,
    apv.actual_type_id,
    apcdt.before_problem_type_id actual_type_id_before_problem,
    apv.long_value
FROM
    alf_prop_value apv,
    alf_prop_class_duplicated_temp apcdt
WHERE
    apv.actual_type_id = apcdt.after_problem_type_id
  AND (
       apcdt.before_problem_type_id,
       apv.long_value
          ) NOT IN (
          SELECT actual_type_id, long_value
          FROM alf_prop_value
      );

--
-- Type:    UPDATE
--
-- Table:   alf_prop_value
--
-- Description:
--
--        Fix the 'alf_prop_value.actual_type_id' column of non-duplicated records by using
--        'alf_prop_value_not_duplicated_temp.actual_type_id_before_problem' column
--
UPDATE alf_prop_value apv
SET
    actual_type_id = apvndt.actual_type_id_before_problem
    FROM
    alf_prop_value_not_duplicated_temp apvndt
WHERE
    apv.id = apvndt.after_problem_id;

----------------------------------------------------------
-- alf_prop_unique_ctx
----------------------------------------------------------

--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_unique_ctx_map_values_temp
--
-- Description:
--
--        Creates a mapping for columns value1_prop_id, value2_prop_id and value3_prop_id columns before and after migration.
--
-- Example:
--
--        Given 'alf_prop_unique_ctx' table:
--
--        -----------------------------------------------------------------------------------------------
--        | id    | version  | value1_prop_id    | value2_prop_id    | value3_prop_id  | prop1_id       |
--        -----------------------------------------------------------------------------------------------
--        | 38    | 0        | 81                | 86                | 3               | 49             |
--        -----------------------------------------------------------------------------------------------
--        | 71    | 0        | 913               | 916               | 879             | 685            |
--        -----------------------------------------------------------------------------------------------
--
--        We will have 'alf_prop_unique_ctx_map_values_temp' table as follows:
--
--        -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
--        | id    | value1_after_problem | value2_after_problem | value3_after_problem  | value1_before_problem   | value2_before_problem | value3_before_problem | prop1_id    |
--        -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
--        | 38    | 81                   | 86                   | 3                     | null                    | null                  | null                  | 49          |
--        -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
--        | 71    | 913                  | 916                  | 879                   | 81                      | 86                    | 3                     | 685         |
--        -----------------------------------------------------------------------------------------------------------------------------------------------------------------------
--
CREATE TABLE alf_prop_unique_ctx_map_values_temp AS
SELECT
    apuc.id,
    apuc.value1_prop_id value1_after_problem,
    apuc.value2_prop_id value2_after_problem,
    apuc.value3_prop_id value3_after_problem,
    (
        SELECT t1.before_problem_id
        FROM
            alf_prop_value_duplicated_temp t1
        WHERE
            t1.after_problem_id = apuc.value1_prop_id
    ) value1_before_problem,
    (
        SELECT t1.before_problem_id
        FROM
            alf_prop_value_duplicated_temp t1
        WHERE
            t1.after_problem_id = apuc.value2_prop_id
    ) value2_before_problem,
    (
        SELECT t1.before_problem_id
        FROM
            alf_prop_value_duplicated_temp t1
        WHERE
            t1.after_problem_id = apuc.value3_prop_id
    ) value3_before_problem,
    prop1_id prop1_id_after_problem
FROM alf_prop_unique_ctx apuc;

--
-- Type:    TEMP TABLE
--
-- Table:   alf_prop_unique_ctx_duplicated_values_temp
--
-- Description:
--
--        Contains only the records that cannot have both value1_prop_id, value2_prop_id and value3_prop_id columns
--        updated at once, otherwise, the index 'idx_alf_propuctx' would be violated.
--
-- Example:
--
--        Given 'alf_prop_unique_ctx' table:
--
--        -------------------------------------------------------------------------------------------
--        | id    | version  | value1_prop_id    | value2_prop_id | value3_prop_id    | prop1_id    |
--        -------------------------------------------------------------------------------------------
--        | 38    | 0        | 81                | 86             | 3                 | 49          |
--        -------------------------------------------------------------------------------------------
--        | 71    | 0        | 913               | 916            | 879               | 685         |
--        -------------------------------------------------------------------------------------------
--
--        We will have 'alf_prop_unique_ctx_duplicated_values_temp' table as follows:
--
--        --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--        | after_problem_id  | before_problem_id | value1_after_problem | value2_after_problem | value3_after_problem | value1_before_problem | value2_before_problem | value3_before_problem |
--        --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--        | 71                | 38                | 913                  | 916                  | 879                  | 81                    | 86                    | 3                     |
--        --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--
CREATE TABLE alf_prop_unique_ctx_duplicated_values_temp AS
SELECT
    apucmvt.id after_problem_id,
    apuc.id before_problem_id,
    apucmvt.value1_after_problem,
    apucmvt.value2_after_problem,
    apucmvt.value3_after_problem,
    apucmvt.value1_before_problem,
    apucmvt.value2_before_problem,
    apucmvt.value3_before_problem,
    apucmvt.prop1_id_after_problem
FROM
    alf_prop_unique_ctx_map_values_temp apucmvt,
    alf_prop_unique_ctx apuc
WHERE
    apucmvt.value1_before_problem IS NOT NULL
  AND apucmvt.value2_before_problem IS NOT NULL
  AND apucmvt.value3_before_problem IS NOT NULL
  AND apuc.value1_prop_id = apucmvt.value1_before_problem
  AND apuc.value2_prop_id = apucmvt.value2_before_problem
  AND apuc.value3_prop_id = apucmvt.value3_before_problem;

--
-- Type:    UPDATE
--
-- Table:   alf_prop_unique_ctx
--
-- Description:
--
--        Fix 'value1_prop_id' column with id used before problem
--
UPDATE alf_prop_unique_ctx apuc
SET
    value1_prop_id = apucmvt.value1_before_problem
    FROM
    alf_prop_unique_ctx_map_values_temp apucmvt
WHERE
    apuc.value1_prop_id = apucmvt.value1_after_problem
  AND apucmvt.value1_before_problem IS NOT NULL
  AND apucmvt.prop1_id_after_problem = apuc.prop1_id
  AND (
    apucmvt.value1_before_problem,
    apuc.value2_prop_id,
    apuc.value3_prop_id
    ) NOT IN (
    SELECT
    value1_before_problem,
    value2_before_problem,
    value3_before_problem
    FROM
    alf_prop_unique_ctx_duplicated_values_temp
    );

--
-- Type:    UPDATE
--
-- Table:   alf_prop_unique_ctx
--
-- Description:
--
--        Fix 'value2_prop_id' column with id used before problem
--
UPDATE alf_prop_unique_ctx apuc
SET
    value2_prop_id = apucmvt.value2_before_problem
    FROM
    alf_prop_unique_ctx_map_values_temp apucmvt
WHERE
    apuc.value2_prop_id = apucmvt.value2_after_problem
  AND apucmvt.value2_before_problem IS NOT NULL
  AND apucmvt.prop1_id_after_problem = apuc.prop1_id
  AND (
    apuc.value1_prop_id,
    apucmvt.value2_before_problem,
    apuc.value3_prop_id
    ) NOT IN (
    SELECT
    value1_before_problem,
    value2_before_problem,
    value3_before_problem
    FROM
    alf_prop_unique_ctx_duplicated_values_temp
    );

--
-- Type:    UPDATE
--
-- Tabl:    alf_prop_unique_ctx
--
-- Description:
--
--        Fix 'value3_prop_id' column with id used before problem
--
UPDATE alf_prop_unique_ctx apuc
SET
    value3_prop_id = apucmvt.value3_before_problem
    FROM
    alf_prop_unique_ctx_map_values_temp apucmvt
WHERE
    apuc.value3_prop_id = apucmvt.value3_after_problem
  AND apucmvt.value3_before_problem IS NOT NULL
  AND apucmvt.prop1_id_after_problem = apuc.prop1_id
  AND (
    apuc.value1_prop_id,
    apuc.value2_prop_id,
    apucmvt.value3_before_problem
    ) NOT IN (
    SELECT
    value1_before_problem,
    value2_before_problem,
    value3_before_problem
    FROM
    alf_prop_unique_ctx_duplicated_values_temp
    );

----------------------------------------------------------
-- alf_prop_link
----------------------------------------------------------

--
-- Type:    UPDATE
--
-- Table:   alf_prop_link
--
-- Description:
--
--        Fix 'alf_prop_link.key_prop_id' column with id before migration.
--
UPDATE alf_prop_link apl
SET
    key_prop_id = apvdt.before_problem_id
    FROM
    alf_prop_value_duplicated_temp apvdt
WHERE
    apl.key_prop_id = apvdt.after_problem_id;

--
-- Type:    UPDATE
--
-- Table: alf_prop_link
--
-- Description:
--
--        Fix 'alf_prop_link.key_prop_id' column with id before migration.
--
UPDATE alf_prop_link apl
SET
    value_prop_id = apvdt.before_problem_id
    FROM
    alf_prop_value_duplicated_temp apvdt
WHERE
    apl.value_prop_id = apvdt.after_problem_id;

----------------------------------------------------------
-- alf_audit_app
----------------------------------------------------------

--
-- Type:    TEMP TABLE
--
-- Table:   alf_audit_app_duplicated_temp
--
-- Description:
--
--        Contains the relationship between audit app ids that have been duplicated and the
--        id that was used before migration.
--
-- Example:
--
--        Given 'alf_audit_app' table:
--
--        ----------------------------------------------------------------------
--        | id   | version  | app_name_id | audit_model_id | disabled_paths_id |
--        ----------------------------------------------------------------------
--        | 1    | 0        | 5           | 3              | 2                 |
--        ----------------------------------------------------------------------
--        | 2    | 0        | 72          | 1              | 34                |
--        ----------------------------------------------------------------------
--        | 3    | 0        | 73          | 2              | 35                |
--        ----------------------------------------------------------------------
--        | 4    | 0        | 74          | 4              | 36                |
--        ----------------------------------------------------------------------
--        | 5    | 0        | 881         | 1              | 669               |
--        ----------------------------------------------------------------------
--        | 6    | 0        | 883         | 2              | 670               |
--        ----------------------------------------------------------------------
--        | 7    | 0        | 884         | 3              | 671               |
--        ----------------------------------------------------------------------
--        | 8    | 0        | 885         | 4              | 672               |
--        ----------------------------------------------------------------------
--
--        We will have 'alf_audit_app_duplicated_temp' table as follows:
--
--        ----------------------------------------------
--        | after_problem_aa_id | before_problem_aa_id |
--        ----------------------------------------------
--        | 5                   | 2                    |
--        ----------------------------------------------
--        | 6                   | 3                    |
--        ----------------------------------------------
--        | 7                   | 1                    |
--        ----------------------------------------------
--        | 8                   | 4                    |
--        ----------------------------------------------
--
CREATE TABLE alf_audit_app_duplicated_temp AS
SELECT
    aaa.id after_problem_aa_id,
    (
        SELECT id
        FROM alf_audit_app aaa2
        WHERE
            aaa2.app_name_id = apvdt.before_problem_id
    ) before_problem_aa_id
FROM
    alf_audit_app aaa,
    alf_prop_value_duplicated_temp apvdt
WHERE
    aaa.app_name_id = apvdt.after_problem_id;

select count(1) from alf_audit_app_duplicated_temp;

----------------------------------------------------------
-- alf_audit_entry
----------------------------------------------------------

--
-- Type:    UPDATE
--
-- Table:   alf_audit_entry
--
-- Description:
--
--        Fix 'alf_audit_entry.audit_app_id' with 'alf_audit_app.id' that was used before the migration.
--
UPDATE alf_audit_entry aae
SET
    audit_app_id = aaadt.before_problem_aa_id
    FROM
    alf_audit_app_duplicated_temp aaadt
WHERE
    aae.audit_app_id = aaadt.after_problem_aa_id;

--
-- Type:    UPDATE
--
-- Table:   alf_audit_entry
--
-- Description:
--
--        Fix 'alf_audit_entry.audit_user_id' with id from 'alf_prop_value' table that was used before migration.
--
UPDATE alf_audit_entry aae
SET
    audit_user_id = apvdt.before_problem_id
    FROM
    alf_prop_value_duplicated_temp apvdt
WHERE
    aae.audit_user_id = apvdt.after_problem_id;

----------------------------------------------------------
-- DELETE DUPLICATES
----------------------------------------------------------

DELETE FROM alf_prop_class
WHERE
    id IN (
        SELECT after_problem_type_id
        FROM alf_prop_class_duplicated_temp
    );

DELETE FROM alf_audit_app
WHERE
    id IN (
        SELECT after_problem_aa_id
        FROM alf_audit_app_duplicated_temp
    );

DELETE FROM alf_prop_value
WHERE
    id IN (
        SELECT after_problem_id
        FROM alf_prop_value_duplicated_temp
    );

----------------------------------------------------------
-- REMOVE TEMP TABLES
----------------------------------------------------------
DROP TABLE alf_prop_class_duplicated_temp;

DROP TABLE alf_prop_string_value_duplicated_temp;

DROP TABLE alf_prop_value_duplicated_temp;

DROP TABLE alf_prop_value_not_duplicated_temp;

DROP TABLE alf_prop_unique_ctx_map_values_temp;

DROP TABLE alf_prop_unique_ctx_duplicated_values_temp;

DROP TABLE alf_audit_app_duplicated_temp;
