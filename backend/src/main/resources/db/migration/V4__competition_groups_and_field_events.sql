-- V4: explicit competition-group metadata for student eligibility and event separation.
-- A = ordinary undergraduate group (甲组); B = sports-major group (乙组).
-- Existing rows remain NULL until an administrator verifies the student's category.
-- This migration is additive and safe to rerun; it does not delete or rewrite registrations/results.

SET @sports_db = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sys_user' AND COLUMN_NAME='competition_group'),
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN competition_group VARCHAR(10) NULL DEFAULT NULL COMMENT ''A-普通本科组 B-体育专业组'' AFTER class_name'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='sport_item' AND COLUMN_NAME='competition_group'),
  'SELECT 1',
  'ALTER TABLE sport_item ADD COLUMN competition_group VARCHAR(10) NULL DEFAULT NULL COMMENT ''A-普通本科组 B-体育专业组'' AFTER item_name'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='registration' AND COLUMN_NAME='competition_group'),
  'SELECT 1',
  'ALTER TABLE registration ADD COLUMN competition_group VARCHAR(10) NULL DEFAULT NULL COMMENT ''Group snapshot at registration'' AFTER user_id'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='relay_team' AND COLUMN_NAME='competition_group'),
  'SELECT 1',
  'ALTER TABLE relay_team ADD COLUMN competition_group VARCHAR(10) NULL DEFAULT NULL COMMENT ''All relay members must belong to this group'' AFTER sport_item_id'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;
