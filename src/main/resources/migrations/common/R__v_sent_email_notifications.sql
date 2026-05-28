--
-- View to provide sent email notification data for reporting and search
--
DROP VIEW IF EXISTS v_sent_email_notifications;

CREATE VIEW v_sent_email_notifications AS
SELECT n.notification_id,
       n.official_visit_id,
       ov.prison_code,
       n.created_time AS sent_date_time,
       ov.visit_date,
       ov.start_time AS visit_start_time,
       ov.end_time AS visit_end_time,
       n.email_address,
       n.email_status,
       n.reason AS notification_type,
       ov.prisoner_number
  FROM notification n
  JOIN official_visit ov ON ov.official_visit_id = n.official_visit_id