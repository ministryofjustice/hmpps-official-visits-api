--
-- View to provide the booked official visits for a prison
--
DROP VIEW IF EXISTS v_official_visits_booked;

CREATE VIEW v_official_visits_booked AS
  select ov.prison_code,
         pts.day_code,
         rd.description as day_description,
         ov.official_visit_id,
         pvs.prison_visit_slot_id,
         pvs.prison_time_slot_id,
         ov.visit_date,
         pts.start_time,
         pts.end_time,
         ov.visit_status_code,
         ov.visit_type_code,
         pv.prisoner_number,
         visitor.contact_id,
         visitor.visitor_type_code,
         visitor.relationship_type_code,
         visitor.first_name,
         visitor.last_name,
         visitor.relationship_code,
         pvs.dps_location_id
from official_visit ov
join prisoner_visited pv on pv.official_visit_id = ov.official_visit_id
join official_visitor visitor on visitor.official_visit_id = ov.official_visit_id
join prison_visit_slot pvs on pvs.prison_visit_slot_id = ov.prison_visit_slot_id
join prison_time_slot pts on pts.prison_time_slot_id = pvs.prison_time_slot_id
join reference_data rd on rd.group_code = 'DAY' and rd.code = pts.day_code
order by ov.visit_date, pts.start_time;

