DROP VIEW IF EXISTS v_visits_for_review;

CREATE VIEW v_visits_for_review AS
SELECT ov.prison_code,
       ov.official_visit_id,
       ov.visit_date,
       ov.start_time,
       ov.end_time,
       ov.visit_status_code,
       ov.visit_type_code,
       ov.dps_location_id,
       pv.prisoner_number,
       vr.visit_review_id,
       vr.raised_time,
       vr.expired_time,
       vrd.visit_review_detail_id,
       vrd.issue_type,
       vrd.issue_detail,
       vrd.acknowledged_by,
       vrd.acknowledgement_time,
       vrd.raised_time as detail_raised_time
FROM official_visit ov
JOIN prisoner_visited pv on pv.official_visit_id = ov.official_visit_id
JOIN visit_review vr on vr.official_visit_id = ov.official_visit_id
JOIN visit_review_detail vrd on vrd.visit_review_id = vr.visit_review_id;
