-- Competition operations V3: rounds, result workflow, and relay result support.
-- Additive and safe to rerun against the current schema. Back up the database first.

SET @sports_db = DATABASE();
SET @had_result_workflow = EXISTS(
  SELECT 1 FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='is_published'
);

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='schedule' AND COLUMN_NAME='round_type'),
  'SELECT 1',
  'ALTER TABLE schedule ADD COLUMN round_type VARCHAR(20) NOT NULL DEFAULT ''FINAL'' COMMENT ''PRELIMINARY/SEMIFINAL/FINAL/FIELD_FINAL'' AFTER round_no'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

UPDATE schedule
SET round_type = CASE
  WHEN schedule_name LIKE '%预赛%' THEN 'PRELIMINARY'
  WHEN schedule_name LIKE '%半决赛%' THEN 'SEMIFINAL'
  WHEN schedule_name LIKE '%决赛%' THEN 'FINAL'
  ELSE round_type
END
WHERE schedule_name IS NOT NULL
  AND (schedule_name LIKE '%预赛%' OR schedule_name LIKE '%半决赛%' OR schedule_name LIKE '%决赛%');

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='relay_team_id'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN relay_team_id BIGINT NULL AFTER registration_id'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='result_status'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN result_status VARCHAR(20) NOT NULL DEFAULT ''VALID'' COMMENT ''VALID/DNS/DNF/DQ/WD'' AFTER score_text'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='review_status'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN review_status TINYINT NOT NULL DEFAULT 1 COMMENT ''1-pending 2-reviewed 3-returned'' AFTER points'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='review_by'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN review_by BIGINT NULL AFTER review_status'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='review_time'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN review_time DATETIME NULL AFTER review_by'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='review_remark'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN review_remark VARCHAR(255) NULL AFTER review_time'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='is_published'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN is_published TINYINT NOT NULL DEFAULT 0 COMMENT ''0-draft 1-published'' AFTER review_remark'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='published_by'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN published_by BIGINT NULL AFTER is_published'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND COLUMN_NAME='published_at'),
  'SELECT 1',
  'ALTER TABLE result ADD COLUMN published_at DATETIME NULL AFTER published_by'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

-- A result may belong to an individual registration or a relay team. Nullable registration/user
-- columns are required for team results. Existing rows remain unchanged.
ALTER TABLE result MODIFY COLUMN registration_id BIGINT NULL;
ALTER TABLE result MODIFY COLUMN user_id BIGINT NULL;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND INDEX_NAME='uk_registration'),
  'ALTER TABLE result DROP INDEX uk_registration',
  'SELECT 1'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND INDEX_NAME='uk_result_schedule_registration'),
  'SELECT 1',
  'ALTER TABLE result ADD UNIQUE KEY uk_result_schedule_registration (schedule_id, registration_id, is_deleted)'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND INDEX_NAME='uk_result_schedule_team'),
  'SELECT 1',
  'ALTER TABLE result ADD UNIQUE KEY uk_result_schedule_team (schedule_id, relay_team_id, is_deleted)'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=@sports_db AND TABLE_NAME='result' AND CONSTRAINT_NAME='fk_result_relay_team'),
  'SELECT 1',
  'ALTER TABLE result ADD CONSTRAINT fk_result_relay_team FOREIGN KEY (relay_team_id) REFERENCES relay_team(id)'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;

UPDATE result SET result_status='VALID' WHERE result_status IS NULL OR result_status='';
-- On the first migration only, retain legacy results as already reviewed and published.
-- The guard prevents subsequent reruns from publishing newly entered draft results.
SET @sql = IF(
  @had_result_workflow = 0,
  'UPDATE result SET review_status=2, is_published=1, published_at=COALESCE(record_time, create_time) WHERE is_published=0 AND review_status=1 AND relay_team_id IS NULL',
  'SELECT 1'
);
PREPARE sports_stmt FROM @sql; EXECUTE sports_stmt; DEALLOCATE PREPARE sports_stmt;
