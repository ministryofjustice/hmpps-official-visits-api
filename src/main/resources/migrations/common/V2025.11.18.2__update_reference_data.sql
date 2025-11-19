
-- Replaced by correct values
delete from reference_data where group_code = 'VIS_TYPE_CODE';

-- No longer needed for MVP
delete from reference_data where group_code = 'EQUIP_CATEGORY';

-- Visit type codes updated with accurate descriptions
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_TYPE_CODE', 'IN_PERSON' , 'In person visit', 1, true),
       ('VIS_TYPE_CODE', 'VIDEO' , 'Video visit', 2, true),
       ('VIS_TYPE_CODE', 'TELEPHONE' , 'Telephone call', 3, true);