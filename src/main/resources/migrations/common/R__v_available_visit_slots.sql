--
-- View to provide the available official visit slots for a prison
--
DROP VIEW IF EXISTS v_available_visit_slots;

CREATE VIEW v_available_visit_slots AS
   select
       pts.prison_code,
       pvs.prison_visit_slot_id,
       pvs.prison_time_slot_id,
       rd.display_sequence,
       pts.day_code,
       rd.description as day_description,
       pts.start_time,
       pts.end_time,
       pts.effective_date,
       pts.expiry_date,
       pvs.dps_location_id,
       pvs.max_adults,
       pvs.max_groups,
       pvs.max_video_sessions
   FROM prison_time_slot pts
   JOIN prison_visit_slot pvs on pvs.prison_time_slot_id = pts.prison_time_slot_id
   JOIN reference_data rd on rd.group_code = 'DAY' and rd.code = pts.day_code
   order by rd.display_sequence, rd.description, pts.start_time;
