-- Add created_by column to notification table to track email delivery status
ALTER TABLE notification
    ADD COLUMN created_by varchar(100) DEFAULT '' NOT NULL;

-- Indexes to support email search by created_by
CREATE INDEX IF NOT EXISTS idx_notification_created_by ON notification(created_by);

-- Indexes to support email search by visit id and created_by
CREATE INDEX IF NOT EXISTS idx_notification_visit_id_created_by
    ON notification (official_visit_id, created_by DESC);
