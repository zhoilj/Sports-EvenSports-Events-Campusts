-- Demo data for validating GET /api/checkins/my after deployment.
-- Apply ONLY after 01_schema.sql and migrations V1, V2, V3.
-- Demo login: student_demo / 123456. Change or remove this account before production use.
-- Conditional inserts only; no truncation or deletion.

INSERT INTO sys_user
    (username, password, real_name, role, student_no, department, class_name, gender, status)
SELECT 'student_demo',
       '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH',
       '检录测试学生', 'STUDENT', 'DEMO20261018', '部署验证学院', '部署验证班', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username='student_demo' AND is_deleted=0);

SET @student_id := (SELECT id FROM sys_user WHERE username='student_demo' AND is_deleted=0 ORDER BY id LIMIT 1);
SET @admin_id := (SELECT id FROM sys_user WHERE username='admin' AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO event
    (event_name,event_type,description,start_date,end_date,reg_start_date,reg_end_date,location,organizer,contact_phone,status,create_by)
SELECT '部署验证专用校运会','TRACK_FIELD','仅用于验证学生个人赛程与检录编号查询。',
       '2026-10-19','2026-10-20','2026-10-18','2026-10-19','校田径场','体育部','',2,@admin_id
WHERE NOT EXISTS (SELECT 1 FROM event WHERE event_name='部署验证专用校运会' AND is_deleted=0);
SET @event_id := (SELECT id FROM event WHERE event_name='部署验证专用校运会' AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO sport_item
    (event_id,item_name,item_type,is_relay,relay_member_count,relay_reserve_count,gender_limit,max_participants,current_participants,result_type,result_unit,sort_rule,rules,status)
SELECT @event_id,'男子100米（部署验证）','TRACK',0,4,0,1,20,1,'TIME','秒','ASC','部署验收用项目，不用于正式比赛。',1
WHERE NOT EXISTS (SELECT 1 FROM sport_item WHERE event_id=@event_id AND item_name='男子100米（部署验证）' AND is_deleted=0);
SET @sport_item_id := (SELECT id FROM sport_item WHERE event_id=@event_id AND item_name='男子100米（部署验证）' AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO registration (event_id,sport_item_id,user_id,status,remark)
SELECT @event_id,@sport_item_id,@student_id,2,'部署检录查询验证用报名'
WHERE NOT EXISTS (SELECT 1 FROM registration WHERE event_id=@event_id AND sport_item_id=@sport_item_id AND user_id=@student_id AND is_deleted=0);
SET @registration_id := (SELECT id FROM registration WHERE event_id=@event_id AND sport_item_id=@sport_item_id AND user_id=@student_id AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO schedule
    (event_id,sport_item_id,schedule_name,round_no,round_type,group_no,match_date,start_time,end_time,location,status,remark)
SELECT @event_id,@sport_item_id,'男子100米预赛（部署验证）',1,'PRELIMINARY','A',
       '2026-10-19','09:00:00','10:00:00','田径场1号跑道',1,'部署检录查询验证用赛程'
WHERE NOT EXISTS (SELECT 1 FROM schedule WHERE event_id=@event_id AND schedule_name='男子100米预赛（部署验证）' AND is_deleted=0);
SET @schedule_id := (SELECT id FROM schedule WHERE event_id=@event_id AND schedule_name='男子100米预赛（部署验证）' AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO schedule_entry (schedule_id,registration_id,relay_team_id,group_no,lane_no,start_order,qualification_status)
SELECT @schedule_id,@registration_id,NULL,'A',1,1,'ENTERED'
WHERE NOT EXISTS (SELECT 1 FROM schedule_entry WHERE schedule_id=@schedule_id AND registration_id=@registration_id AND is_deleted=0);
SET @schedule_entry_id := (SELECT id FROM schedule_entry WHERE schedule_id=@schedule_id AND registration_id=@registration_id AND is_deleted=0 ORDER BY id LIMIT 1);

INSERT INTO checkin_record (schedule_entry_id,status)
SELECT @schedule_entry_id,'PENDING'
WHERE NOT EXISTS (SELECT 1 FROM checkin_record WHERE schedule_entry_id=@schedule_entry_id AND is_deleted=0);

-- This query should return exactly one row after the seed script succeeds.
SELECT u.username,e.event_name,si.item_name,s.schedule_name,s.match_date,s.start_time,s.location,
       se.group_no,se.lane_no,CONCAT('CK',LPAD(se.id,8,'0')) AS checkin_code,cr.status AS checkin_status
FROM schedule_entry se
JOIN registration r ON r.id=se.registration_id
JOIN sys_user u ON u.id=r.user_id
JOIN schedule s ON s.id=se.schedule_id
JOIN event e ON e.id=s.event_id
JOIN sport_item si ON si.id=s.sport_item_id
LEFT JOIN checkin_record cr ON cr.schedule_entry_id=se.id AND cr.is_deleted=0
WHERE u.username='student_demo' AND e.event_name='部署验证专用校运会'
  AND s.schedule_name='男子100米预赛（部署验证）' AND se.is_deleted=0;
