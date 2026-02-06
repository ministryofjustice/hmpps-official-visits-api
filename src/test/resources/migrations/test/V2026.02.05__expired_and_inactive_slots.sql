-- Moorland time slots for visits - expired and future-dated effective
insert into prison_time_slot (prison_time_slot_id, prison_code, day_code, start_time, end_time, effective_date, expiry_date, created_by, created_time)
values (10, 'MDI', 'FRI', '09:10', '10:10', '2023-01-01', '2023-02-01', 'TIM', current_timestamp),
       (11, 'MDI', 'FRI', '09:15', '10:15', '2040-01-01', null, 'TIM', current_timestamp);

alter sequence if exists prison_time_slot_prison_time_slot_id_seq restart with 12;

-- Moorland visit slots - relating to expired and future-dated time slots
insert into prison_visit_slot (prison_visit_slot_id, prison_time_slot_id, dps_location_id, max_adults, max_groups, max_video_sessions, created_by, created_time)
values (10, 10, '9485cf4a-750b-4d74-b594-59bacbcda247', 1, 1, 1, 'TIM', current_timestamp),
       (11, 11, '50b61cbe-e42b-4a77-a00e-709b0421b8ed', 1, 1, 1, 'TIM', current_timestamp);

alter sequence if exists prison_visit_slot_prison_visit_slot_id_seq restart with 12;

-- End