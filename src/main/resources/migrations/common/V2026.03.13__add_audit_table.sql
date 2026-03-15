create table audited_event (
  audited_event_id bigserial NOT NULL CONSTRAINT audited_event_pk PRIMARY KEY,
  official_visit_id bigserial NOT NULL,
  prison_code varchar(10) NOT NULL,
  prisoner_number varchar(7) NOT NULL,
  event_source varchar(5) NOT NULL,
  user_name varchar(100) NOT NULL,
  user_full_name varchar(100) NOT NULL,
  summary_text varchar(255) NOT NULL,
  detail_text varchar(400) NOT NULL,
  event_date_time timestamp NOT NULL
);

CREATE INDEX idx_audited_event_1 ON audited_event(official_visit_id);
CREATE INDEX idx_audited_event_2 ON audited_event(prison_code);
CREATE INDEX idx_audited_event_3 ON audited_event(prisoner_number);
CREATE INDEX idx_audited_event_4 ON audited_event(event_date_time);
CREATE INDEX idx_audited_event_5 ON audited_event(user_name);
