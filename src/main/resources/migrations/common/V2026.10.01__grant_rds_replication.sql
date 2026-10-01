--for DPS reporting user need replication access
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'pglogical') THEN
        GRANT rds_replication to digital_prison_reporting;
    END IF;
END
$$;