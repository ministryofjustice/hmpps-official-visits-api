
insert into official_visit (official_visit_id, prison_visit_slot_id, visit_date, start_time, end_time, dps_location_id, visit_status_code, visit_type_code, prison_code, prisoner_number, current_term, search_type_code, created_by, created_time)
values (1, 1, '2026-01-01', '09:00', '10:00','9485cf4a-750b-4d74-b594-59bacbcda247','SCHEDULED', 'VIDEO', 'MDI', 'A4567AZ', true,'RUB_A', 'TIM', current_timestamp);

insert into prisoner_visited (prisoner_visited_id, official_visit_id, prisoner_number, created_by, created_time)
values (1, 1, 'A4567AZ', 'TIM', current_timestamp);

insert into official_visitor (official_visitor_id, official_visit_id, visitor_type_code, relationship_type_code, first_name, last_name, contact_id, prisoner_contact_id, relationship_code, lead_visitor, created_time, created_by)
values (1, 1, 'CONTACT', 'OFFICIAL', 'Adam', 'Adams', 20085662, 7331628,'CUSPO', true, current_timestamp, 'TIM');

