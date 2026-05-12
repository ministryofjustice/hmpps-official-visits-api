
-- Swaleside (SWI) time slot: every Wednesday 09:00 to 09:45
-- This mirrors the reported production scenario where bookings against a slot with
-- max_groups=2 but multiple visitors per visit are incorrectly exhausting capacity.
insert into prison_time_slot (prison_time_slot_id, prison_code, day_code, start_time, end_time, effective_date, expiry_date, created_by, created_time)
values (20, 'SWI', 'WED', '09:00', '09:45', '2025-10-01', null, 'SWALESIDE_ADMIN', current_timestamp);

alter sequence if exists prison_time_slot_prison_time_slot_id_seq restart with 21;

-- Swaleside visit slot for Legal Vidlink – Groups 2, Adults 10, Video sessions 2
insert into prison_visit_slot (prison_visit_slot_id, prison_time_slot_id, dps_location_id, max_adults, max_groups, max_video_sessions, created_by, created_time)
values (20, 20, 'f47ac10b-58cc-4372-a567-0e02b2c3d479', 10, 2, 2, 'SWALESIDE_ADMIN', current_timestamp);

alter sequence if exists prison_visit_slot_prison_visit_slot_id_seq restart with 21;

-- End

