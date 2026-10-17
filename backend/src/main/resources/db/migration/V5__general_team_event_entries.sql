-- General team-entry support for collective, folk-sport and fun events.
-- Relay items remain a specialized four-runner/team workflow; all team events share
-- the existing team/member persistence while using their own configured roster size.
SET @sports_db = DATABASE();

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='is_team_event'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN is_team_event TINYINT NOT NULL DEFAULT 0 COMMENT ''1 for relay or college-team events'' AFTER is_relay');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='team_member_count'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN team_member_count INT NOT NULL DEFAULT 0 COMMENT ''Required members for non-relay team events'' AFTER relay_member_count');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='team_reserve_count'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN team_reserve_count INT NOT NULL DEFAULT 0 COMMENT ''Reserve members for non-relay team events'' AFTER team_member_count');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='max_teams_per_college'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN max_teams_per_college INT NOT NULL DEFAULT 1 COMMENT ''Maximum active teams per college for this item'' AFTER team_reserve_count');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='team_roster_rule'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN team_roster_rule VARCHAR(30) NOT NULL DEFAULT ''ANY'' COMMENT ''Team roster gender composition rule'' AFTER max_teams_per_college');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

UPDATE sport_item
SET is_team_event = 1
WHERE is_relay = 1
   OR item_name LIKE '%集体%' OR item_name LIKE '%民体%' OR item_name LIKE '%趣味%'
   OR item_name LIKE '%拔河%' OR item_name LIKE '%跳长绳%' OR item_name LIKE '%两人三足%'
   OR item_name LIKE '%三人板鞋%' OR item_name LIKE '%抛绣球%' OR item_name LIKE '%高杆绣球%'
   OR item_name LIKE '%背篓接绣球%' OR item_name LIKE '%背篓投绣球%'
   OR item_name LIKE '%校园越野跑%' OR item_name LIKE '%车轮滚滚%' OR item_name LIKE '%旱地龙舟%'
   OR item_name LIKE '%袋鼠跳%' OR item_name LIKE '%滚铁环%' OR item_name LIKE '%企鹅漫步%'
   OR item_name LIKE '%协同大转盘%';

UPDATE sport_item
SET scoring_multiplier = 2
WHERE is_team_event = 1 AND scoring_multiplier = 1;

UPDATE sport_item
SET team_member_count = CASE
  WHEN item_name LIKE '%校园越野跑%' THEN 10
  WHEN item_name LIKE '%和谐共进%' THEN 10
  WHEN item_name LIKE '%两人三足%' THEN 20
  WHEN item_name LIKE '%拔河%' THEN 20
  WHEN item_name LIKE '%集体跳长绳%' THEN 20
  WHEN item_name LIKE '%车轮滚滚%' THEN 10
  WHEN item_name LIKE '%旱地龙舟%' THEN 10
  WHEN item_name LIKE '%袋鼠跳%' THEN 20
  WHEN item_name LIKE '%滚铁环%' THEN 10
  WHEN item_name LIKE '%企鹅漫步%' THEN 10
  WHEN item_name LIKE '%协同大转盘%' THEN 30
  WHEN item_name LIKE '%三人板鞋%' THEN 18
  WHEN item_name LIKE '%背篓%' THEN 5
  WHEN item_name LIKE '%高杆绣球%' THEN 6
  WHEN item_name LIKE '%抛绣球%' THEN 0
  ELSE team_member_count
END
WHERE is_team_event = 1 AND team_member_count = 0 AND is_relay = 0;

UPDATE sport_item
SET max_teams_per_college = 2
WHERE item_name LIKE '%三人板鞋%' OR item_name LIKE '%高杆绣球%'
   OR item_name LIKE '%背篓%' OR item_name LIKE '%抛绣球%';

UPDATE sport_item
SET team_roster_rule = CASE
  WHEN item_name LIKE '%混合接力%' THEN 'MIXED_RELAY_LEGS'
  WHEN item_name LIKE '%校园越野跑%' THEN 'CUSTOM'
  WHEN item_name LIKE '%和谐共进%' THEN 'MIXED_5M5F'
  WHEN item_name LIKE '%两人三足%' THEN 'PAIRS_1M1F'
  WHEN item_name LIKE '%拔河%' THEN 'MIXED_10M10F'
  WHEN item_name LIKE '%集体跳长绳%' THEN 'MIXED_10M10F'
  WHEN item_name LIKE '%车轮滚滚%' OR item_name LIKE '%旱地龙舟%'
    OR item_name LIKE '%滚铁环%' OR item_name LIKE '%企鹅漫步%' THEN 'MIXED_5M5F'
  WHEN item_name LIKE '%袋鼠跳%' THEN 'MIXED_10M10F'
  WHEN item_name LIKE '%协同大转盘%' THEN 'MIXED_15M15F'
  WHEN item_name LIKE '%三人板鞋%' THEN 'MIXED_MIN_6F'
  WHEN item_name LIKE '%背篓%' THEN 'MIXED_2M2F_MIN'
  WHEN item_name LIKE '%高杆绣球%' THEN 'MIXED_3M3F'
  WHEN item_name LIKE '%抛绣球%' THEN 'CUSTOM'
  ELSE 'ANY'
END
WHERE team_roster_rule = 'ANY'
  AND (is_team_event = 1 OR is_relay = 1);
