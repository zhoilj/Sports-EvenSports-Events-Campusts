-- Official order-book configuration fields.
-- Additive migration only; back up the target database before applying.
SET @sports_db = DATABASE();

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='division'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN division VARCHAR(20) NOT NULL DEFAULT ''OPEN'' COMMENT ''A/B/STAFF/OPEN'' AFTER gender_limit');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='gender_category'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN gender_category VARCHAR(20) NOT NULL DEFAULT ''OPEN'' COMMENT ''MALE/FEMALE/MIXED/OPEN'' AFTER division');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='round_policy'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN round_policy VARCHAR(30) NOT NULL DEFAULT ''FLEXIBLE'' COMMENT ''FINAL_ONLY/PRELIMINARY_FINAL/FLEXIBLE'' AFTER gender_category');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='scoring_multiplier'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN scoring_multiplier INT NOT NULL DEFAULT 1 COMMENT ''Points multiplier for the official rule'' AFTER sort_rule');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='participation_points'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN participation_points INT NOT NULL DEFAULT 0 COMMENT ''Participation points for checked-in participants'' AFTER scoring_multiplier');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='record_points'),
  'SELECT 1', 'ALTER TABLE sport_item ADD COLUMN record_points INT NOT NULL DEFAULT 9 COMMENT ''Points for a record-breaking result before multiplier'' AFTER participation_points');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='schedule' AND COLUMN_NAME='qualification_count'),
  'SELECT 1', 'ALTER TABLE schedule ADD COLUMN qualification_count INT NULL COMMENT ''Total qualifiers from this round'' AFTER round_type');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='schedule' AND COLUMN_NAME='award_count'),
  'SELECT 1', 'ALTER TABLE schedule ADD COLUMN award_count INT NULL COMMENT ''Places eligible for placement points in this round'' AFTER qualification_count');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='is_record'),
  'SELECT 1', 'ALTER TABLE result ADD COLUMN is_record TINYINT NOT NULL DEFAULT 0 COMMENT ''1 if the result breaks a recognized meet record'' AFTER result_status');
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

-- Best-effort migration of existing item definitions. Administrators should verify
-- every item against the official order book before opening the competition.
UPDATE sport_item
SET division = CASE
  WHEN item_name LIKE '%甲组%' THEN 'A'
  WHEN item_name LIKE '%乙组%' THEN 'B'
  WHEN item_name LIKE '%教工组%' THEN 'STAFF'
  ELSE division
END
WHERE division = 'OPEN';

UPDATE sport_item
SET gender_category = CASE
  WHEN item_name LIKE '%混合接力%' OR item_name LIKE '%混合%' THEN 'MIXED'
  WHEN item_name LIKE '%男子%' THEN 'MALE'
  WHEN item_name LIKE '%女子%' THEN 'FEMALE'
  WHEN gender_limit = 1 THEN 'MALE'
  WHEN gender_limit = 2 THEN 'FEMALE'
  ELSE 'OPEN'
END
WHERE gender_category = 'OPEN';

UPDATE sport_item
SET round_policy = CASE
  WHEN item_name LIKE '%混合接力%' OR item_type = 'FIELD' THEN 'FINAL_ONLY'
  WHEN item_name LIKE '%100米%' OR item_name LIKE '%200米%'
    OR item_name LIKE '%4×100%' OR item_name LIKE '%4x100%' OR item_name LIKE '%4X100%' THEN 'PRELIMINARY_FINAL'
  ELSE 'FINAL_ONLY'
END
WHERE round_policy = 'FLEXIBLE';

UPDATE sport_item
SET scoring_multiplier = 2
WHERE scoring_multiplier = 1
  AND (is_relay = 1
   OR item_name LIKE '%集体%' OR item_name LIKE '%民体%' OR item_name LIKE '%趣味%'
   OR item_name LIKE '%拔河%' OR item_name LIKE '%跳长绳%' OR item_name LIKE '%两人三足%'
   OR item_name LIKE '%三人板鞋%' OR item_name LIKE '%抛绣球%' OR item_name LIKE '%背篓接绣球%'
   OR item_name LIKE '%校园越野跑%' OR item_name LIKE '%车轮滚滚%' OR item_name LIKE '%旱地龙舟%'
   OR item_name LIKE '%袋鼠跳%' OR item_name LIKE '%滚铁环%' OR item_name LIKE '%企鹅漫步%'
   OR item_name LIKE '%协同大转盘%');

UPDATE sport_item
SET participation_points = 3
WHERE participation_points = 0
  AND (item_name LIKE '%趣味%' OR item_name LIKE '%车轮滚滚%' OR item_name LIKE '%旱地龙舟%'
   OR item_name LIKE '%袋鼠跳%' OR item_name LIKE '%滚铁环%' OR item_name LIKE '%企鹅漫步%'
   OR item_name LIKE '%协同大转盘%' OR item_name LIKE '%集体跳长绳%');

UPDATE sport_item SET record_points = 9 WHERE record_points IS NULL OR record_points < 0;
