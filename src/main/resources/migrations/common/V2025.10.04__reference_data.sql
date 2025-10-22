
-- Visit status (VIS_STS domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_STS', 'CANC' , 'Cancelled', 1, true),
       ('VIS_STS', 'HMPOP' , 'Terminated by staff', 1, true),
       ('VIS_STS', 'NORM' , 'Normal completion', 1, true),
       ('VIS_STS', 'OFFEND' , 'Offender completed early', 1, true),
       ('VIS_STS', 'SCH' , 'Scheduled', 2, true),
       ('VIS_STS', 'COMP' , 'Completed', 3, true),
       ('VIS_STS', 'EXP' , 'Expired', 4, true),
       ('VIS_STS', 'VDE' , 'Visitor declined entry', 5, true),
       ('VIS_STS', 'VISITOR' , 'Visitor completed early', 1, true),
       ('VIS_STS', 'A' , 'Active', 5, true);

-- Visit completion codes (VIS_COMPLETE domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_COMPLETE', 'CANC' , 'Cancelled', 99, true),
       ('VIS_COMPLETE', 'HMPOP' , 'Terminated by staff', 99, true),
       ('VIS_COMPLETE', 'NORM' , 'Normal completion', 2, true),
       ('VIS_COMPLETE', 'OFFEND' , 'Offender completed early', 99, true),
       ('VIS_COMPLETE', 'VISITOR' , 'Visitor completed early', 99, true),
       ('VIS_COMPLETE', 'SCH' , 'Scheduled', 99, true),
       ('VIS_COMPLETE', 'VDE' , 'Visitor declined entry', 99, true);

-- Visit type code(use type OTHER if the type of visit is not known from NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_TYPE_CODE', 'IN_PERSON' , 'Official visit', 1, true),
       ('VIS_TYPE_CODE', 'VIDEO' , 'Social contact', 2, true),
       ('VIS_TYPE_CODE', 'TELEPHONE' , 'Social contact', 2, true),
       ('VIS_TYPE_CODE', 'OTHER' , 'Social contact', 2, true);

-- Visit location type (VIS_LOC_TYPE domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_LOC_TYPE', 'BOOTH' , 'Booth', 1, true),
       ('VIS_LOC_TYPE', 'BOOTHS' , 'Booths', 99, true),
       ('VIS_LOC_TYPE', 'CHAPL' , 'Chaplaincy', 1, true),
       ('VIS_LOC_TYPE', 'CLSD' , 'Closed visit', 1, true),
       ('VIS_LOC_TYPE', 'MEDI' , 'Medical', 1, true),
       ('VIS_LOC_TYPE', 'ROOM' , 'Room', 99, true),
       ('VIS_LOC_TYPE', 'TOWN' , 'Town visits', 99, false),
       ('VIS_LOC_TYPE', 'TABLE' , 'Table', 99, true);

-- Visit search level (SEARCH_LEVEL domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('SEARCH_LEVEL', 'FULL' , 'Full search', 4, true),
       ('SEARCH_LEVEL', 'PAT' , 'Pat down search', 99, false),
       ('SEARCH_LEVEL', 'RUB' , 'Rub down search', 99, false),
       ('SEARCH_LEVEL', 'RUB_A' , 'Rubdown level A', 2, true),
       ('SEARCH_LEVEL', 'RUB_B' , 'Rubdown level B', 3, true),
       ('SEARCH_LEVEL', 'STR' , 'Strip search', 99, false);

-- Contact types (CONTACTS domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('CONTACTS', 'O' , 'Official', 2, true),
       ('CONTACTS', 'S' , 'Social / Family', 1, true);

-- Equipment category (not in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('EQUIP_CATEGORY', 'PHONE' , 'Mobile phone', 1, true),
       ('EQUIP_CATEGORY', 'LAPTOP' , 'Laptop computer', 2, true),
       ('EQUIP_CATEGORY', 'WATCH' , 'Smart watch', 3, true),
       ('EQUIP_CATEGORY', 'TABLET' , 'Tablet computer', 4, true),
       ('EQUIP_CATEGORY', 'RECORDING' , 'Recording device', 5, true),
       ('EQUIP_CATEGORY', 'CAMERA' , 'Camera', 6, true),
       ('EQUIP_CATEGORY', 'OTHER' , 'Other electronic device', 7, true);

-- Days of the week (DAY domain in NOMIS)
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('DAY', 'MON' , 'Monday', 1, true),
       ('DAY', 'TUE' , 'Tuesday', 2, true),
       ('DAY', 'WED' , 'Wednesday', 3, true),
       ('DAY', 'THU' , 'Thursday', 4, true),
       ('DAY', 'FRI' , 'Friday', 5, true),
       ('DAY', 'SAT' , 'Saturday', 6, true),
       ('DAY', 'SUN' , 'Sunday', 7, true);
