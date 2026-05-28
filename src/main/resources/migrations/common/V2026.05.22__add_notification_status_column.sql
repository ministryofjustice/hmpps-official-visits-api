-- Add status column to notification table to track email delivery status
ALTER TABLE notification
ADD COLUMN email_status varchar(20) DEFAULT 'PENDING' NOT NULL;

-- Indexes to support email search by status
CREATE INDEX IF NOT EXISTS idx_notification_email_status ON notification(email_status);

-- Indexes to support email search by visit id and created date-time
CREATE INDEX IF NOT EXISTS idx_notification_visit_id_created_time
    ON notification (official_visit_id, created_time DESC);

-- Indexes to support sent email search by prison and sent date-time
CREATE INDEX IF NOT EXISTS idx_official_visit_prison_code_visit_id
    ON official_visit (prison_code, official_visit_id);



