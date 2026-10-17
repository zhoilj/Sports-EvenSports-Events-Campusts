-- Phase 1 additive migration for the runtime Java entity schema.
-- BACK UP FIRST. Verify SHOW CREATE TABLE for sys_user/event/sport_item/registration.
-- Never run schema.sql or backend/src/main/resources/db/data.sql against an existing database:
-- the former contains DROP TABLE and the latter contains TRUNCATE TABLE.
-- This script does not drop tables or truncate business data.

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='event' AND column_name='max_projects_per_athlete')=0,
'ALTER TABLE event ADD COLUMN max_projects_per_athlete INT NOT NULL DEFAULT 3 COMMENT ''Per-athlete event entry limit; 0 means unlimited''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sport_item' AND column_name='participation_mode')=0,
'ALTER TABLE sport_item ADD COLUMN participation_mode VARCHAR(20) NOT NULL DEFAULT ''INDIVIDUAL'' COMMENT ''INDIVIDUAL or RELAY''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sport_item' AND column_name='team_member_count')=0,
'ALTER TABLE sport_item ADD COLUMN team_member_count INT NULL COMMENT ''Required relay roster size''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sport_item' AND column_name='current_participants')=0,
'ALTER TABLE sport_item ADD COLUMN current_participants INT NOT NULL DEFAULT 0 COMMENT ''Active entry count; relay counted by team''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name='college_id')=0,
'ALTER TABLE sys_user ADD COLUMN college_id BIGINT NULL COMMENT ''Normalized college reference''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_user' AND column_name='class_id')=0,
'ALTER TABLE sys_user ADD COLUMN class_id BIGINT NULL COMMENT ''Normalized class reference''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='registration' AND column_name='registration_type')=0,
'ALTER TABLE registration ADD COLUMN registration_type VARCHAR(20) NOT NULL DEFAULT ''INDIVIDUAL'' COMMENT ''INDIVIDUAL or RELAY''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='registration' AND column_name='team_name')=0,
'ALTER TABLE registration ADD COLUMN team_name VARCHAR(100) NULL COMMENT ''Relay team name''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='registration' AND column_name='college_snapshot')=0,
'ALTER TABLE registration ADD COLUMN college_snapshot VARCHAR(100) NULL COMMENT ''College at registration time''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='registration' AND column_name='class_snapshot')=0,
'ALTER TABLE registration ADD COLUMN class_snapshot VARCHAR(50) NULL COMMENT ''Class at registration time''','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS org_college (
 id BIGINT NOT NULL AUTO_INCREMENT, college_name VARCHAR(100) NOT NULL, status TINYINT NOT NULL DEFAULT 1,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_org_college_name(college_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS org_class (
 id BIGINT NOT NULL AUTO_INCREMENT, college_id BIGINT NOT NULL, class_name VARCHAR(50) NOT NULL, status TINYINT NOT NULL DEFAULT 1,
 create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_org_class_name(college_id,class_name), KEY idx_org_class_college(college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO org_college(college_name)
SELECT DISTINCT TRIM(department) FROM sys_user WHERE department IS NOT NULL AND TRIM(department)<>'';
INSERT IGNORE INTO org_class(college_id,class_name)
SELECT DISTINCT c.id,TRIM(u.class_name) FROM sys_user u JOIN org_college c ON c.college_name=TRIM(u.department)
WHERE u.department IS NOT NULL AND TRIM(u.department)<>'' AND u.class_name IS NOT NULL AND TRIM(u.class_name)<>'';
UPDATE sys_user u LEFT JOIN org_college c ON c.college_name=TRIM(u.department)
LEFT JOIN org_class cl ON cl.college_id=c.id AND cl.class_name=TRIM(u.class_name)
SET u.college_id=c.id,u.class_id=cl.id WHERE u.department IS NOT NULL OR u.class_name IS NOT NULL;

CREATE TABLE IF NOT EXISTS registration_member (
 id BIGINT NOT NULL AUTO_INCREMENT, registration_id BIGINT NOT NULL, user_id BIGINT NOT NULL, member_order INT NOT NULL,
 college_id BIGINT NULL, class_id BIGINT NULL, college_snapshot VARCHAR(100) NULL, class_snapshot VARCHAR(50) NULL,
 is_deleted TINYINT NOT NULL DEFAULT 0, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_registration_member_user(registration_id,user_id,is_deleted),
 UNIQUE KEY uk_registration_member_order(registration_id,member_order,is_deleted),
 KEY idx_registration_member_user(user_id), KEY idx_registration_member_registration(registration_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

UPDATE registration r JOIN sys_user u ON u.id=r.user_id
SET r.registration_type='INDIVIDUAL', r.college_snapshot=COALESCE(r.college_snapshot,u.department),
r.class_snapshot=COALESCE(r.class_snapshot,u.class_name)
WHERE r.registration_type IS NULL OR r.registration_type='INDIVIDUAL';

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='registration' AND index_name='uk_user_item')>0,
'ALTER TABLE registration DROP INDEX uk_user_item','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='registration' AND index_name='idx_registration_event_item_user_status')=0,
'ALTER TABLE registration ADD INDEX idx_registration_event_item_user_status(event_id,sport_item_id,user_id,status,is_deleted)','SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

UPDATE sport_item si SET si.current_participants=(SELECT COUNT(*) FROM registration r
WHERE r.sport_item_id=si.id AND r.status IN (1,2) AND r.is_deleted=0);
