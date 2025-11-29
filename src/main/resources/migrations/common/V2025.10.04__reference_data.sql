
-- Visit status codes
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_STATUS', 'SCHEDULED' , 'Scheduled', 1, true),
       ('VIS_STATUS', 'CANCELLED' , 'Cancelled', 2, true),
       ('VIS_STATUS', 'COMPLETED_NORMAL' , 'Completed normally', 3, true),
       ('VIS_STATUS', 'COMPLETED_ABNORMAL' , 'Completed early', 4, true),
       ('VIS_STATUS', 'EXPIRED' , 'Expired', 5, true);

-- Visit completion reason codes
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_COMPLETION', 'STAFF_CANCELLED' , 'Cancelled for operational reasons', 2, true),
       ('VIS_COMPLETION', 'VISITOR_CANCELLED' , 'Visitor cancelled', 3, true),
       ('VIS_COMPLETION', 'VISITOR_DENIED' , 'Visitor denied entry', 6, true),
       ('VIS_COMPLETION', 'PRISONER_EARLY' , 'Prisoner completed early', 4, true),
       ('VIS_COMPLETION', 'VISITOR_EARLY' , 'Visitor completed early', 5, true),
       ('VIS_COMPLETION', 'PRISONER_REFUSED' , 'Prisoner refused', 7, true),
       ('VIS_COMPLETION', 'NORMAL' , 'Normal completion', 1, true);

-- Visitor and prisoner attendance codes
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('ATTENDANCE', 'ATTENDED' , 'Attended', 1, true),
       ('ATTENDANCE', 'ABSENT' , 'Did not attend', 2, true);

-- Visit type codes
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_TYPE', 'IN_PERSON' , 'Attend in person', 1, true),
       ('VIS_TYPE', 'VIDEO' , 'Video', 2, true),
       ('VIS_TYPE', 'TELEPHONE' , 'Telephone', 3, true);

-- Visit search level - search applies to the prisoner
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('SEARCH_LEVEL', 'FULL' , 'Full search', 4, true),
       ('SEARCH_LEVEL', 'PAT' , 'Pat down search', 99, false),
       ('SEARCH_LEVEL', 'RUB' , 'Rub down search', 99, false),
       ('SEARCH_LEVEL', 'RUB_A' , 'Rubdown level A', 2, true),
       ('SEARCH_LEVEL', 'RUB_B' , 'Rubdown level B', 3, true),
       ('SEARCH_LEVEL', 'STR' , 'Strip search', 99, false);

-- Contact types
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('CONTACT_TYPE', 'OFFICIAL' , 'Official', 1, true),
       ('CONTACT_TYPE', 'SOCIAL' , 'Social / Family', 2, true);


-- Visitor type - always 'CONTACT' for now (for use if OPV - official prisoner visitors - come into scope)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VISITOR_TYPE', 'CONTACT' , 'Contact', 1, true),
       ('VISITOR_TYPE', 'OPV' , 'Official prison visitor', 2, true);

-- Days of the week
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('DAY', 'MON' , 'Monday', 1, true),
       ('DAY', 'TUE' , 'Tuesday', 2, true),
       ('DAY', 'WED' , 'Wednesday', 3, true),
       ('DAY', 'THU' , 'Thursday', 4, true),
       ('DAY', 'FRI' , 'Friday', 5, true),
       ('DAY', 'SAT' , 'Saturday', 6, true),
       ('DAY', 'SUN' , 'Sunday', 7, true);
