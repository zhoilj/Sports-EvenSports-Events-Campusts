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
