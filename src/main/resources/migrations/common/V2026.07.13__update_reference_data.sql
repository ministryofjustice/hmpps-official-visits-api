update reference_data set description = 'In person' where group_code = 'VIS_TYPE' and code = 'IN_PERSON';
update reference_data set description = 'Phone' where group_code = 'VIS_TYPE' and code = 'TELEPHONE';
update reference_data set description = 'Operational reasons' where group_code = 'VIS_COMPLETION' and code = 'STAFF_CANCELLED';
update reference_data set description = 'Visitor cancellation' where group_code = 'VIS_COMPLETION' and code = 'VISITOR_CANCELLED';
update reference_data set description = 'Prisoner cancellation' where group_code = 'VIS_COMPLETION' and code = 'PRISONER_CANCELLED';