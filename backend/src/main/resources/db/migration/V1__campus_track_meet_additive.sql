-- Campus track-and-field meet migration V1
-- Safe additive migration: does not drop or truncate existing tables.
-- Apply only after backing up the target database and verifying the schema version.
-- Existing core tables expected: event, sport_item, registration, schedule, sys_user.

CREATE TABLE IF NOT EXISTS relay_team (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    sport_item_id BIGINT NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    college VARCHAR(100) DEFAULT NULL,
    class_name VARCHAR(100) DEFAULT NULL,
    creator_user_id BIGINT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-pending 2-approved 3-rejected 4-cancelled',
    review_by BIGINT DEFAULT NULL,
    review_time DATETIME DEFAULT NULL,
    review_remark VARCHAR(255) DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_relay_event_item (event_id, sport_item_id),
    KEY idx_relay_college (college),
    CONSTRAINT fk_relay_team_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_relay_team_item FOREIGN KEY (sport_item_id) REFERENCES sport_item(id),
    CONSTRAINT fk_relay_team_creator FOREIGN KEY (creator_user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接力队伍';

CREATE TABLE IF NOT EXISTS relay_team_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    leg_order TINYINT NOT NULL,
    is_reserve TINYINT NOT NULL DEFAULT 0,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_relay_leg (team_id, leg_order, is_deleted),
    UNIQUE KEY uk_relay_member (team_id, user_id, is_deleted),
    KEY idx_relay_member_user (user_id),
    CONSTRAINT fk_relay_member_team FOREIGN KEY (team_id) REFERENCES relay_team(id),
    CONSTRAINT fk_relay_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接力队员及棒次';

CREATE TABLE IF NOT EXISTS schedule_entry (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id BIGINT NOT NULL,
    registration_id BIGINT DEFAULT NULL,
    relay_team_id BIGINT DEFAULT NULL,
    group_no VARCHAR(20) DEFAULT NULL,
    lane_no TINYINT DEFAULT NULL,
    start_order INT DEFAULT NULL,
    seed_score DECIMAL(10,3) DEFAULT NULL,
    qualification_status VARCHAR(20) NOT NULL DEFAULT 'ENTERED',
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_registration (schedule_id, registration_id, is_deleted),
    UNIQUE KEY uk_schedule_team (schedule_id, relay_team_id, is_deleted),
    KEY idx_schedule_group_lane (schedule_id, group_no, lane_no),
    CONSTRAINT fk_schedule_entry_schedule FOREIGN KEY (schedule_id) REFERENCES schedule(id),
    CONSTRAINT fk_schedule_entry_registration FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT fk_schedule_entry_relay_team FOREIGN KEY (relay_team_id) REFERENCES relay_team(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次参赛名单、分组和分道';

CREATE TABLE IF NOT EXISTS checkin_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_entry_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING-CHECKED-IN-ABSENT-WITHDRAWN-DQ',
    checked_by BIGINT DEFAULT NULL,
    checked_at DATETIME DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_checkin_entry (schedule_entry_id, is_deleted),
    CONSTRAINT fk_checkin_entry FOREIGN KEY (schedule_entry_id) REFERENCES schedule_entry(id),
    CONSTRAINT fk_checkin_user FOREIGN KEY (checked_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='比赛检录记录';

CREATE TABLE IF NOT EXISTS field_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schedule_entry_id BIGINT NOT NULL,
    attempt_no TINYINT NOT NULL,
    score DECIMAL(10,3) DEFAULT NULL,
    is_foul TINYINT NOT NULL DEFAULT 0,
    is_valid TINYINT NOT NULL DEFAULT 1,
    recorder_id BIGINT DEFAULT NULL,
    recorded_at DATETIME DEFAULT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_field_attempt (schedule_entry_id, attempt_no, is_deleted),
    CONSTRAINT fk_field_attempt_entry FOREIGN KEY (schedule_entry_id) REFERENCES schedule_entry(id),
    CONSTRAINT fk_field_attempt_recorder FOREIGN KEY (recorder_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='田赛试跳试投成绩';

CREATE TABLE IF NOT EXISTS scoring_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    place_no INT NOT NULL,
    points INT NOT NULL DEFAULT 0,
    rule_name VARCHAR(100) DEFAULT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_event_place (event_id, place_no, is_deleted),
    CONSTRAINT fk_scoring_rule_event FOREIGN KEY (event_id) REFERENCES event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校运会名次积分规则';

CREATE TABLE IF NOT EXISTS result_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    result_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL COMMENT 'CREATED-UPDATED-REVIEWED-PUBLISHED',
    before_value JSON DEFAULT NULL,
    after_value JSON DEFAULT NULL,
    operator_id BIGINT NOT NULL,
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remark VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_result_audit_result (result_id, operated_at),
    CONSTRAINT fk_result_audit_result FOREIGN KEY (result_id) REFERENCES result(id),
    CONSTRAINT fk_result_audit_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成绩变更审计日志';
