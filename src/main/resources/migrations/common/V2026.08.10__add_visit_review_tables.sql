CREATE TABLE visit_review_queue
(
  visit_review_queue_id BIGSERIAL PRIMARY KEY,
  official_visit_id     BIGINT NOT NULL,
  created_time          TIMESTAMP NOT NULL,
  triggering_event      VARCHAR(40) NOT NULL
);

CREATE INDEX idx_visit_review_queue_official_visit_id ON visit_review_queue (official_visit_id);
CREATE INDEX idx_visit_review_queue_triggering_event ON visit_review_queue (triggering_event);

CREATE TABLE visit_review
(
  visit_review_id   BIGSERIAL PRIMARY KEY,
  official_visit_id BIGINT NOT NULL,
  raised_time       TIMESTAMP NOT NULL,
  expired_time      TIMESTAMP
);

CREATE INDEX idx_visit_review_official_visit_id ON visit_review (official_visit_id);
CREATE INDEX idx_visit_review_expired_time ON visit_review (expired_time);

CREATE TABLE visit_review_detail
(
  visit_review_detail_id   BIGSERIAL PRIMARY KEY,
  visit_review_id          BIGINT NOT NULL references visit_review(visit_review_id),
  raised_time              TIMESTAMP NOT NULL,
  issue_type               VARCHAR(40) NOT NULL,
  issue_detail             VARCHAR(100),
  acknowledgement_time     TIMESTAMP,
  acknowledged_by          VARCHAR(60) NOT NULL
);

CREATE INDEX idx_visit_review_detail_issue_type ON visit_review_detail (issue_type);
CREATE INDEX idx_visit_review_acknowledgement_time ON visit_review_detail (acknowledgement_time);
