-- Phase 1 registration compatibility and relay configuration.
-- Safe additive migration for existing databases; back up before applying.

SET @sports_db = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='current_participants'),
  'SELECT 1',
  'ALTER TABLE sport_item ADD COLUMN current_participants INT NOT NULL DEFAULT 0 COMMENT ''Current active registration count'' AFTER max_participants'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='event' AND COLUMN_NAME='max_items_per_user'),
  'SELECT 1',
  'ALTER TABLE event ADD COLUMN max_items_per_user INT NOT NULL DEFAULT 0 COMMENT ''Maximum individual projects per student; 0 means unlimited'' AFTER reg_end_date'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='is_relay'),
  'SELECT 1',
  'ALTER TABLE sport_item ADD COLUMN is_relay TINYINT NOT NULL DEFAULT 0 COMMENT ''Whether this is a relay team event'' AFTER item_type'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='relay_member_count'),
  'SELECT 1',
  'ALTER TABLE sport_item ADD COLUMN relay_member_count TINYINT NOT NULL DEFAULT 4 COMMENT ''Required relay members'' AFTER is_relay'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='relay_reserve_count'),
  'SELECT 1',
  'ALTER TABLE sport_item ADD COLUMN relay_reserve_count TINYINT NOT NULL DEFAULT 0 COMMENT ''Allowed relay reserves'' AFTER relay_member_count'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

UPDATE sport_item si
SET si.current_participants = (
  SELECT COUNT(*)
  FROM registration r
  WHERE r.sport_item_id = si.id
    AND r.status IN (1, 2)
    AND r.is_deleted = 0
);
