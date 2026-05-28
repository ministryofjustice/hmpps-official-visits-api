ALTER TABLE notification
    ADD COLUMN email_status varchar(20) DEFAULT 'PENDING' NOT NULL,
    ADD COLUMN status_updated_time timestamp;

