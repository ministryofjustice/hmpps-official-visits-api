--
-- View to provide the available official visit slots for a prison
--
DROP VIEW IF EXISTS v_official_visit_summary;

CREATE VIEW v_official_visit_summary AS
select ov.official_visit_id,
  ov.prison_code,
  ov.prisoner_number,
  ov.visit_status_code,
  ov.visit_type_code,
  ov.visit_date,
  ov.start_time,
  ov.end_time,
  ov.dps_location_id,
  ov.prison_visit_slot_id,
  ov.staff_notes,
  ov.prisoner_notes,
  ov.visitor_concern_notes,
  ov.completion_code,
  ov.created_by,
  ov.created_time,
  ov.updated_by,
  ov.updated_time,
  ov.offender_book_id,
  (select count(1) from official_visitor ov2 where ov2.official_visit_id = ov.official_visit_id) as number_of_visitors,
  ov.completion_notes
  from official_visit ov
