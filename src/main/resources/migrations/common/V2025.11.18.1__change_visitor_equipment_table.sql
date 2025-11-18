
drop table visitor_equipment RESTRICT;

--
-- Recreate table and indexes in simpler form
--
create table visitor_equipment(
   visitor_equipment_id bigserial NOT NULL CONSTRAINT visitor_equipment_pk PRIMARY KEY,
   official_visitor_id bigint NOT NULL references official_visitor(official_visitor_id),
   description varchar(240),
   approved boolean NOT NULL DEFAULT false,
   approved_time timestamp,
   approved_by varchar(100),
   created_time timestamp NOT NULL,
   created_by varchar(100) NOT NULL,
   updated_time timestamp,
   updated_by varchar(100)
);

CREATE INDEX idx_visitor_equipment_1 ON visitor_equipment(official_visitor_id);
CREATE INDEX idx_visitor_equipment_2 ON visitor_equipment(approved_by);
CREATE INDEX idx_visitor_equipment_3 ON visitor_equipment(created_time);
