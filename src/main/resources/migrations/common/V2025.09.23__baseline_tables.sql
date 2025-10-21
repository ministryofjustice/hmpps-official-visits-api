--
-- Table prison_time_slot
-- The time slots on each day when visits are supported in this prison.
-- NOTE: The rows have an effective_date (date from which the row takes effect) and an expiry
-- date (date from which the row should be ignored). This allows each prison to configure new
-- time slots effective from a date in the future, and when that date arrives the new slots
-- will automatically come into effect.
--
create table prison_time_slot (
    prison_time_slot_id bigserial NOT NULL CONSTRAINT prison_time_slot_pk PRIMARY KEY,
    prison_code varchar(5) NOT NULL,
    day_code varchar(3) NOT NULL, -- ref: DAY_CODE
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    effective_date date NOT NULL,
    expiry_date date
);

CREATE INDEX idx_prison_time_slot_1 ON prison_time_slot(prison_code);
CREATE INDEX idx_prison_time_slot_2 ON prison_time_slot(effective_date);
CREATE INDEX idx_prison_time_slot_3 ON prison_time_slot(expiry_date);

--
-- Table prison_visit_slot
-- The locations and time slots where visits can take place in each prison.
-- The max_adults, max_groups, and max_video_sessions are all limits which describe
-- when each location and time slot is full, and no more bookings should be taken
-- for it. If max_video_sessions is reached for video type visits, then other in
-- person visits are still possible upto the other limits.
--
create table prison_visit_slot (
    prison_visit_slot_id bigserial NOT NULL CONSTRAINT prison_visit_slot_pk PRIMARY KEY,
    prison_time_slot_id bigint NOT NULL references prison_time_slot(prison_time_slot_id),
    dps_location_id UUID not null,
    max_adults int NOT NULL,
    max_groups int NOT NULL,
    max_video_sessions int NOT NULL
);

CREATE INDEX idx_prison_visit_slot_1 ON prison_visit_slot(prison_time_slot_id);
CREATE INDEX idx_prison_visit_slot_2 ON prison_visit_slot(dps_location_id);

--
-- Table video_location
-- Contains decorating data for video enabled rooms.
-- Ideally, this would be held in the internal-prison-locations-api so it can be shared across services.
--
create table video_location (
   video_location_id bigserial NOT NULL CONSTRAINT video_location_pk PRIMARY KEY,
   dps_location_id UUID NOT NULL,
   video_url varchar(160)
);

CREATE INDEX idx_video_location_1 ON prison_visit_slot(dps_location_id);

--
-- Table refence_data
-- Contains the domain, code, description and display sequence for reference data
-- used within the service. Due to the lack of a shared reference data service,
-- the values will be copies from equivalent reference data in NOMIS initially.
--
create table reference_data (
   reference_data_id bigserial NOT NULL CONSTRAINT reference_data_pk PRIMARY KEY,
   group_code varchar(20) NOT NULL,
   code varchar(20) NOT NULL,
   description varchar(100),
   display_sequence int,
   enabled boolean NOT NULL
);

CREATE INDEX idx_reference_data_1 ON reference_data(group_code, code);

--
-- Table official_visit
-- Contains the booked instances of the official visits.
-- Official visits are booked into a visit slot (for the start/time times and location).
--
create table official_visit (
   official_visit_id bigserial NOT NULL CONSTRAINT official_visit_pk PRIMARY KEY,
   prison_visit_slot_id bigint NOT NULL references prison_visit_slot(prison_visit_slot_id),
   visit_date date NOT NULL,
   visit_status_code varchar(20) NOT NULL, -- ACTIVE, CANCELLED, COMPLETED, AWAITING_OUTCOME
   visit_type_code varchar(20) NOT NULL,  -- IN_PERSON, VIDEO, TELEPHONE,OTHER
   prison_code varchar(10) NOT NULL, -- intentional duplication for support
   prisoner_number varchar(7) NOT NULL, -- intentional duplication for support
   private_notes varchar(240), -- not shared on movement slips or schedules
   public_notes varchar(240), -- can be shared with the prisoner on movement slips
   search_type_code varchar(20), -- ref code SEARCH_TYPE
   completion_code varchar(20), -- SCH by default, staff must enter how it turned out
   override_ban_by varchar(100),
   override_ban_time timestamp,
   created_by varchar(100) NOT NULL,
   created_time timestamp NOT NULL,
   amended_by varchar(100),
   amended_time timestamp
);

CREATE INDEX idx_official_visit_1 ON official_visit(prison_visit_slot_id);
CREATE INDEX idx_official_visit_2 ON official_visit(visit_date);
CREATE INDEX idx_official_visit_3 ON official_visit(prison_code);
CREATE INDEX idx_official_visit_4 ON official_visit(prisoner_number);
CREATE INDEX idx_official_visit_5 ON official_visit(visit_type_code);
CREATE INDEX idx_official_visit_6 ON official_visit(visit_status_code);
CREATE INDEX idx_official_visit_7 ON official_visit(created_time);

--
-- Table prisoner_visited
-- Contains the details of the prisoner being visited and their attendance at it.
--
create table prisoner_visited (
   prisoner_visited_id bigserial NOT NULL CONSTRAINT prisoner_visited_pk PRIMARY KEY,
   official_visit_id bigint NOT NULL references official_visit(official_visit_id),
   prisoner_number varchar(7) NOT NULL, -- denormalized intentionally ease of checking
   attendance_code varchar(20),
   attendance_notes varchar(240),
   attendance_by varchar(100),
   attendance_time timestamp
);

CREATE INDEX idx_prisoner_visited_1 ON prisoner_visited(official_visit_id);
CREATE INDEX idx_prisoner_visited_2 ON prisoner_visited(prisoner_number);
CREATE INDEX idx_prisoner_visited_3 ON prisoner_visited(attendance_code);

--
-- Table official_visitor
-- Contains the details of each person coming to the visit, and their attendance at it.
--
create table official_visitor (
   official_visitor_id bigserial NOT NULL CONSTRAINT official_visitor_pk PRIMARY KEY,
   official_visit_id bigint NOT NULL references official_visit(official_visit_id),
   visitor_type_code varchar(20) NOT NULL, -- CONTACT, OPV, PRISONER
   contact_type_code varchar(20) NOT NULL, -- SOCIAL, OFFICIAL, NOT_A_CONTACT
   first_name varchar(60), -- denormalized for ease of checking - otherwise just IDs
   last_name varchar(60), -- denormalized for ease of checking - otherwise just IDs
   contact_id bigint, -- if in contacts or null
   prisoner_contact_id bigint, --if defined in social or official relationship or null
   relationship_code varchar(20), -- denormalized for ease of checking
   opv_organisation varchar(100), -- if an OPV visitor
   lead_visitor boolean NOT NULL DEFAULT false,
   assisted_visit boolean NOT NULL DEFAULT false,
   email_address varchar(160), -- potential to notify lead visitor about this visit
   phone_number varchar(30), -- potential to notify lead visitor about this visit
   visitor_notes varchar(240),
   attendance_code varchar(20),
   attendance_notes varchar(240),
   attendance_by varchar(100),
   attendance_time timestamp,
   created_by varchar(100) NOT NULL,
   created_time timestamp NOT NULL,
   amended_by varchar(100),
   amended_time timestamp
);

CREATE INDEX idx_official_visitor_1 ON official_visitor(official_visit_id);
CREATE INDEX idx_official_visitor_2 ON official_visitor(visitor_type_code);
CREATE INDEX idx_official_visitor_3 ON official_visitor(contact_type_code);
CREATE INDEX idx_official_visitor_4 ON official_visitor(last_name, first_name);
CREATE INDEX idx_official_visitor_5 ON official_visitor(contact_id, prisoner_contact_id);
CREATE INDEX idx_official_visitor_6 ON official_visitor(relationship_code);
CREATE INDEX idx_official_visitor_7 ON official_visitor(created_time);
CREATE INDEX idx_official_visitor_8 ON official_visitor(created_by);

--
-- Table visitor_equipment
-- Contains the details of equipment brought into the prison by a visitor.
--
create table visitor_equipment(
   visitor_equipment_id bigserial NOT NULL CONSTRAINT visitor_equipment_pk PRIMARY KEY,
   official_visitor_id bigint NOT NULL references official_visitor(official_visitor_id),
   category_code varchar(20) NOT NULL, -- ref: EQUIP_CATEGORY, MOBILE, LAPTOP, TABLET
   model varchar(60),
   reason varchar(100),
   approved boolean NOT NULL DEFAULT false,
   approval_notes varchar(240),
   approved_by varchar(100),
   approved_time timestamp,
   created_by varchar(100) NOT NULL,
   created_time timestamp NOT NULL,
   updated_by varchar(100),
   updated_time timestamp
);

CREATE INDEX idx_visitor_equipment_1 ON visitor_equipment(official_visitor_id);
CREATE INDEX idx_visitor_equipment_2 ON visitor_equipment(category_code);
CREATE INDEX idx_visitor_equipment_3 ON visitor_equipment(approved_by);
CREATE INDEX idx_visitor_equipment_4 ON visitor_equipment(created_time);

-- END --
