CREATE TABLE official_visit_client_email
(
  official_visit_client_email_id bigserial    NOT NULL CONSTRAINT official_visit_client_email_pk PRIMARY KEY,
  official_visit_id              bigint       NOT NULL REFERENCES official_visit (official_visit_id),
  email_address                  varchar(100) NOT NULL,
  created_by                     varchar(100) NOT NULL,
  created_time                   timestamp    NOT NULL
);

CREATE INDEX idx_official_visit_client_email_1 ON official_visit_client_email(official_visit_id);
CREATE INDEX idx_official_visit_client_email_2 ON official_visit_client_email(email_address);
-- Enforce case-insensitive uniqueness per visit
CREATE UNIQUE INDEX idx_official_visit_client_email_3
    ON official_visit_client_email(
                                   official_visit_id,
                                   LOWER(TRIM(email_address))
        );
