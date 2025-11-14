insert into official_visit (official_visit_id, prison_visit_slot_id, visit_date, start_time, end_time, dps_location_id, visit_status_code, visit_type_code, prison_code, prisoner_number, current_term, search_type_code, created_by, created_time)
values (1, 7, current_date, '09:00', '10:00','9485cf4a-750b-4d74-b594-59bacbcda247','SCH', 'VIDEO', 'MDI', 'G4793VF', true,'RUB_A', 'TIM', current_timestamp);

insert into official_visit (official_visit_id, prison_visit_slot_id, visit_date, start_time, end_time, dps_location_id, visit_status_code, visit_type_code, prison_code, prisoner_number, current_term, search_type_code, created_by, created_time)
values (2, 9, current_date, '11:00', '12:00','9485cf4a-750b-4d74-b594-59bacbcda247','SCH', 'VIDEO', 'MDI', 'G4793VF', true,'RUB_A', 'TIM', current_timestamp);

insert into official_visit (official_visit_id, prison_visit_slot_id, visit_date, start_time, end_time, dps_location_id, visit_status_code, visit_type_code, prison_code, prisoner_number, current_term, search_type_code, created_by, created_time)
values (3, 7, current_date, '09:00', '10:00','9485cf4a-750b-4d74-b594-59bacbcda247','CANCELLED', 'VIDEO', 'MDI', 'G4793VF', true,'RUB_A', 'TIM', current_timestamp);

alter sequence if exists official_visit_official_visit_id_seq restart with 4;

-- Prisoner visited
insert into prisoner_visited (prisoner_visited_id, official_visit_id, prisoner_number, created_by, created_time)
values (1, 1, 'G4793VF', 'TIM', current_timestamp);

insert into prisoner_visited (prisoner_visited_id, official_visit_id, prisoner_number, created_by, created_time)
values (2, 2, 'G4793VF', 'TIM', current_timestamp);

insert into prisoner_visited (prisoner_visited_id, official_visit_id, prisoner_number, created_by, created_time)
values (3, 3, 'G4793VF', 'TIM', current_timestamp);

alter sequence if exists prisoner_visited_prisoner_visited_id_seq restart with 4;

-- Existing visitors
insert into official_visitor (official_visitor_id, official_visit_id, visitor_type_code, contact_type_code, first_name, last_name, contact_id, prisoner_contact_id, relationship_code, lead_visitor, created_time, created_by)
values (1, 1, 'O', 'O', 'Adam', 'Adams', 20085662, 7331628,'CUSP', true, current_timestamp, 'TIM');

insert into official_visitor (official_visitor_id, official_visit_id, visitor_type_code, contact_type_code, first_name, last_name, contact_id, prisoner_contact_id, relationship_code, lead_visitor, created_time, created_by)
values (2, 2, 'O', 'O', 'Adam', 'Adams', 20085662, 7331628,'CUSP', true, current_timestamp, 'TIM');

insert into official_visitor (official_visitor_id, official_visit_id, visitor_type_code, contact_type_code, first_name, last_name, contact_id, prisoner_contact_id, relationship_code, lead_visitor, created_time, created_by)
values (3, 3, 'O', 'O', 'Adam', 'Adams', 20085662, 7331628,'CUSP', true, current_timestamp, 'TIM');

alter sequence if exists official_visitor_official_visitor_id_seq restart with 4;
