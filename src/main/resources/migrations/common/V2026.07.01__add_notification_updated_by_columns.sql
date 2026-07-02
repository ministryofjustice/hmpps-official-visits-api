-- Add created_by column to notification table to track email delivery status
ALTER TABLE notification
    ADD COLUMN created_by varchar(100) DEFAULT '' NOT NULL;

-- Indexes to support email search by created_by
CREATE INDEX IF NOT EXISTS idx_notification_created_by ON notification(created_by);