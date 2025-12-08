
-- Additional visit completion reason codes
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_COMPLETION', 'STAFF_EARLY' , 'Staff completed early', 8, true),
       ('VIS_COMPLETION', 'PRISONER_CANCELLED' , 'Prisoner cancelled', 9, true),
       ('VIS_COMPLETION', 'VISITOR_NO_SHOW' , 'Visitor did not show', 10, true);
