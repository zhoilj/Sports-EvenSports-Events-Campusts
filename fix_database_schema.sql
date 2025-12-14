-- ============================================================
-- 校园体育赛事管理系统 - 数据库字段修复脚本
-- 用于修复 Entity 与数据库表结构不一致的问题
-- ============================================================

USE `sports_events`;

-- 1. 为 sport_item 表添加 current_participants 字段
-- 该字段用于记录当前报名人数
ALTER TABLE `sport_item`
ADD COLUMN `current_participants` INT NOT NULL DEFAULT 0 COMMENT '当前报名人数'
AFTER `max_participants`;

-- 2. 更新 current_participants 字段的值（基于实际报名数据）
UPDATE `sport_item` si
SET `current_participants` = (
    SELECT COUNT(*)
    FROM `registration` r
    WHERE r.sport_item_id = si.id
    AND r.status IN (1, 2)  -- 待审核或已通过的报名
    AND r.is_deleted = 0
);

-- 3. 为 sys_user 表添加 college 字段（如果不存在）
-- 注意：schema 中使用的是 department，但 Entity 中可能使用 college
-- 这里添加一个别名或新字段以保持兼容性
-- ALTER TABLE `sys_user` ADD COLUMN `college` VARCHAR(100) DEFAULT NULL COMMENT '学院' AFTER `department`;
-- 如果要使用同一个字段，可以直接在查询时使用 department 作为 college

-- ============================================================
-- 执行完成后请检查:
-- 1. sport_item 表是否有 current_participants 字段
-- 2. current_participants 的值是否正确更新
-- ============================================================
