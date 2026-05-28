ALTER TABLE notification
  ADD COLUMN status varchar(40),
  ADD COLUMN status_updated_time timestamp;

