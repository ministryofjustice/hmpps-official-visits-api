-- Adds the completion_notes column to the official_visit table.  This column can be populated on completion or cancellation of a visit.
ALTER TABLE official_visit ADD COLUMN completion_notes VARCHAR(240);
