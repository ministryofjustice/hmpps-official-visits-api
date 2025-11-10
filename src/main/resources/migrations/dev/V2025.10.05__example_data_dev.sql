
-- Moorland time slots for visits - 3 each on MON, WEDS and FRI
insert into prison_time_slot (prison_time_slot_id, prison_code, day_code, start_time, end_time, effective_date, expiry_date, created_by, created_time)
values (1, 'MDI', 'MON', '09:00', '10:00', '2025-10-01', null, 'TIM', current_timestamp),
       (2, 'MDI', 'MON', '10:00', '11:00', '2025-10-01', null, 'TIM', current_timestamp),
       (3, 'MDI', 'MON', '11:00', '12:00', '2025-10-01', null, 'TIM', current_timestamp),
       (4, 'MDI', 'WED', '09:00', '10:00', '2025-10-01', null, 'TIM', current_timestamp),
       (5, 'MDI', 'WED', '10:00', '11:00', '2025-10-01', null, 'TIM', current_timestamp),
       (6, 'MDI', 'WED', '11:00', '12:00', '2025-10-01', null, 'TIM', current_timestamp),
       (7, 'MDI', 'FRI', '09:00', '10:00', '2025-10-01', null, 'TIM', current_timestamp),
       (8, 'MDI', 'FRI', '10:00', '11:00', '2025-10-01', null, 'TIM', current_timestamp),
       (9, 'MDI', 'FRI', '11:00', '12:00', '2025-10-01', null, 'TIM', current_timestamp);

alter sequence if exists prison_time_slot_prison_time_slot_id_seq restart with 10;

-- Moorland visit slots - timeslot + location + limits
insert into prison_visit_slot (prison_visit_slot_id, prison_time_slot_id, dps_location_id, max_adults, max_groups, max_video_sessions, created_by, created_time)
values (1, 1, '9485cf4a-750b-4d74-b594-59bacbcda247', 10, 5, 4, 'TIM', current_timestamp),
       (2, 2, '50b61cbe-e42b-4a77-a00e-709b0421b8ed', 10, 5, 4, 'TIM', current_timestamp),
       (3, 3, '9485cf4a-750b-4d74-b594-59bacbcda247', 4, 2, 1, 'TIM', current_timestamp),
       (4, 4, '9485cf4a-750b-4d74-b594-59bacbcda247', 10, 5, 4, 'TIM', current_timestamp),
       (5, 5, '50b61cbe-e42b-4a77-a00e-709b0421b8ed', 10, 5, 4, 'TIM', current_timestamp),
       (6, 6, '9485cf4a-750b-4d74-b594-59bacbcda247', 4, 2, 1, 'TIM', current_timestamp),
       (7, 7, '9485cf4a-750b-4d74-b594-59bacbcda247', 10, 5, 4, 'TIM', current_timestamp),
       (8, 8, '50b61cbe-e42b-4a77-a00e-709b0421b8ed', 10, 5, 4, 'TIM', current_timestamp),
       (9, 9, '9485cf4a-750b-4d74-b594-59bacbcda247', 1, 1, 1, 'TIM', current_timestamp);

alter sequence if exists prison_visit_slot_prison_visit_slot_id_seq restart with 10;

-- Existing visit
insert into official_visit (official_visit_id, prison_visit_slot_id, visit_date, start_time, end_time, dps_location_id, visit_status_code, visit_type_code, prison_code, prisoner_number, current_term, search_type_code, created_by, created_time)
values (1, 1, current_date, '09:00', '10:00','9485cf4a-750b-4d74-b594-59bacbcda247','SCH', 'VIDEO', 'MDI', 'G4793VF', true,'RUB_A', 'TIM', current_timestamp);

alter sequence if exists official_visit_official_visit_id_seq restart with 2;

-- Prisoner visited
insert into prisoner_visited (prisoner_visited_id, official_visit_id, prisoner_number, created_by, created_time)
values (1, 1, 'G4793VF', 'TIM', current_timestamp);

alter sequence if exists prisoner_visited_prisoner_visited_id_seq restart with 2;

-- Existing visitors
insert into official_visitor (official_visitor_id, official_visit_id, visitor_type_code, contact_type_code, first_name, last_name, contact_id, prisoner_contact_id, relationship_code, lead_visitor, created_time, created_by)
values (1, 1, 'O', 'O', 'Adam', 'Adams', 20085662, 7331628,'CUSP', true, current_timestamp, 'TIM');

alter sequence if exists official_visitor_official_visitor_id_seq restart with 2;

-- End