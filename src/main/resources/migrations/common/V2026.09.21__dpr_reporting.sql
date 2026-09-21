-- enable pglogical extention if it does not exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'pglogical') THEN
        CREATE EXTENSION IF NOT EXISTS pglogical;
        GRANT rds_superuser to digital_prison_reporting; -- revert this once the DPR ingestion is active/live
    END IF;
END
$$;
