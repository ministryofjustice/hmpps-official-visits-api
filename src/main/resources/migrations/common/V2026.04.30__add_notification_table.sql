CREATE TABLE notification
(
  notification_id            bigserial  NOT NULL CONSTRAINT notification_id_pk PRIMARY KEY,
  official_visit_id          bigserial NOT NULL,
  template_id                varchar(100) NOT NULL ,
  email_address              varchar(100) NOT NULL ,
  reason                     varchar(40) NOT NULL,
  gov_notify_notification_id uuid NOT NULL,
  created_time               timestamp NOT NULL
);

CREATE INDEX idx_notification_visit_id ON notification(official_visit_id);
CREATE INDEX idx_notification_email ON notification(email_address);
CREATE UNIQUE INDEX idx_gov_notify_notification_id ON notification(gov_notify_notification_id);
