
-- Replaced by correct values
delete from reference_data where group_code = 'VIS_TYPE_CODE';

-- Visit type codes updated with accurate descriptions
insert into reference_data (group_code, code, description, display_sequence, enabled)
values ('VIS_TYPE_CODE', 'IN_PERSON' , 'Attend in person', 1, true),
       ('VIS_TYPE_CODE', 'VIDEO' , 'Video', 2, true),
       ('VIS_TYPE_CODE', 'TELEPHONE' , 'Telephone', 3, true);