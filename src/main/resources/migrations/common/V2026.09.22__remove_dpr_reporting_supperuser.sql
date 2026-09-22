--remove rds_superuser from digital_prison_reporting
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'pglogical') THEN
        REVOKE rds_superuser from digital_prison_reporting; --GRANT rds_superuser to digital_prison_reporting;
    END IF;
END
$$;