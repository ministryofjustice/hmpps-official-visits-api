-- Alter Table : visit_review_detail to match the design docs
ALTER TABLE visit_review_detail RENAME COLUMN acknowledgement_time TO acknowledged_time;
ALTER INDEX idx_visit_review_detail_acknowledgement_time RENAME TO idx_visit_review_detail_acknowledged_time;
ALTER TABLE visit_review_detail ALTER COLUMN acknowledged_by DROP NOT NULL;