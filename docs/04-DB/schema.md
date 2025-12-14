# 数据库设计文档 (Schema)

> 校园体育赛事管理系统 - 数据库设计说明

**文档版本**：v1.0.0  
**创建日期**：2024-01  
**文档状态**：✅ 已评审  
**负责人**：架构师 Winston

---

## 1. 数据库概述

### 1.1 基本信息

| 项目 | 说明 |
|------|------|
| 数据库类型 | MySQL 8.0 |
| 字符集 | utf8mb4 |
| 排序规则 | utf8mb4_unicode_ci |
| 存储引擎 | InnoDB |
| 数据库名 | sports_events |

### 1.2 设计原则

1. **命名规范**
   - 表名：小写字母 + 下划线，如 `sys_user`、`sport_item`
   - 字段名：小写字母 + 下划线，如 `create_time`、`event_id`
   - 索引名：`idx_` + 字段名，如 `idx_status`
   - 唯一索引：`uk_` + 字段名，如 `uk_username`

2. **通用字段**
   - `id`：主键，BIGINT 自增
   - `is_deleted`：逻辑删除标记，0-否 1-是
   - `create_time`：创建时间
   - `update_time`：更新时间

3. **外键策略**
   - 不使用物理外键，通过应用层保证数据一致性
   - 关联字段命名：`{关联表}_id`

---

## 2. ER 图

```
+------------------+       +------------------+       +------------------+
|    sys_user      |       |      event       |       |   sport_item     |
+------------------+       +------------------+       +------------------+
| PK id            |       | PK id            |       | PK id            |
|    username      |       |    event_name    |       | FK event_id      |
|    password      |       |    event_type    |       |    item_name     |
|    real_name     |       |    start_date    |       |    item_type     |
|    role          |       |    end_date      |       |    gender_limit  |
|    student_no    |       |    status        |       |    result_type   |
|    department    |       |    ...           |       |    ...           |
+--------+---------+       +--------+---------+       +--------+---------+
         |                          |                          |
         |                          |                          |
         |    +--------------------+|+-------------------------+
         |    |                     |
         v    v                     v
+------------------+       +------------------+
|  registration    |       |    schedule      |
+------------------+       +------------------+
| PK id            |       | PK id            |
| FK event_id      |       | FK event_id      |
| FK sport_item_id |       | FK sport_item_id |
| FK user_id       |       |    match_date    |
|    status        |       |    start_time    |
|    ...           |       |    location      |
+--------+---------+       +--------+---------+
         |                          |
         |                          |
         +------------+-------------+
                      |
                      v
              +------------------+
              |     result       |
              +------------------+
              | PK id            |
              | FK event_id      |
              | FK sport_item_id |
              | FK schedule_id   |
              | FK registration_id|
              | FK user_id       |
              |    score         |
              |    ranking       |
              +------------------+

+------------------+       +------------------+
|  sys_dict_type   |       |  sys_dict_data   |
+------------------+       +------------------+
| PK id            |       | PK id            |
|    dict_code     |<----->| FK dict_code     |
|    dict_name     |       |    item_value    |
+------------------+       |    item_label    |
                           +------------------+

+------------------+
|      news        |
+------------------+
| PK id            |
|    title         |
|    news_type     |
|    content       |
| FK event_id      |
|    is_top        |
+------------------+
```

---

## 3. 表结构详细说明

### 3.1 系统管理模块

#### 3.1.1 用户表 (sys_user)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 用户ID，主键 |
| username | VARCHAR(50) | NO | - | 用户名，唯一 |
| password | VARCHAR(100) | NO | - | 密码(BCrypt加密) |
| real_name | VARCHAR(50) | YES | NULL | 真实姓名 |
| gender | TINYINT | YES | 0 | 性别: 0-未知 1-男 2-女 |
| phone | VARCHAR(20) | YES | NULL | 手机号 |
| email | VARCHAR(100) | YES | NULL | 邮箱 |
| avatar | VARCHAR(255) | YES | NULL | 头像URL |
| role | VARCHAR(20) | NO | 'STUDENT' | 角色: ADMIN/TEACHER/STUDENT |
| student_no | VARCHAR(50) | YES | NULL | 学号(学生角色) |
| department | VARCHAR(100) | YES | NULL | 院系 |
| class_name | VARCHAR(50) | YES | NULL | 班级 |
| status | TINYINT | NO | 1 | 状态: 0-禁用 1-正常 |
| is_deleted | TINYINT | NO | 0 | 是否删除: 0-否 1-是 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| PRIMARY | 主键 | id | 主键索引 |
| uk_username | 唯一 | username | 用户名唯一 |
| uk_student_no | 唯一 | student_no | 学号唯一 |
| idx_role | 普通 | role | 按角色查询 |
| idx_department | 普通 | department | 按院系查询 |
| idx_status | 普通 | status | 按状态查询 |

#### 3.1.2 字典类型表 (sys_dict_type)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 字典类型ID |
| dict_code | VARCHAR(50) | NO | - | 字典编码，唯一 |
| dict_name | VARCHAR(100) | NO | - | 字典名称 |
| remark | VARCHAR(255) | YES | NULL | 备注 |
| status | TINYINT | NO | 1 | 状态 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

#### 3.1.3 字典数据表 (sys_dict_data)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 字典数据ID |
| dict_code | VARCHAR(50) | NO | - | 字典编码 |
| item_value | VARCHAR(50) | NO | - | 字典项值 |
| item_label | VARCHAR(100) | NO | - | 字典项标签 |
| sort_order | INT | NO | 0 | 排序 |
| remark | VARCHAR(255) | YES | NULL | 备注 |
| status | TINYINT | NO | 1 | 状态 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

---

### 3.2 赛事管理模块

#### 3.2.1 赛事表 (event)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 赛事ID |
| event_name | VARCHAR(100) | NO | - | 赛事名称 |
| event_type | VARCHAR(50) | NO | - | 赛事类型(字典) |
| description | TEXT | YES | NULL | 赛事描述 |
| cover_image | VARCHAR(255) | YES | NULL | 封面图片 |
| start_date | DATE | NO | - | 开始日期 |
| end_date | DATE | NO | - | 结束日期 |
| reg_start_date | DATE | YES | NULL | 报名开始日期 |
| reg_end_date | DATE | YES | NULL | 报名截止日期 |
| location | VARCHAR(200) | YES | NULL | 比赛地点 |
| organizer | VARCHAR(100) | YES | NULL | 主办方 |
| contact_person | VARCHAR(50) | YES | NULL | 联系人 |
| contact_phone | VARCHAR(20) | YES | NULL | 联系电话 |
| status | TINYINT | NO | 1 | 状态: 1-草稿 2-报名中 3-进行中 4-已结束 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_by | BIGINT | YES | NULL | 创建人ID |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| idx_event_type | 普通 | event_type | 按类型查询 |
| idx_status | 普通 | status | 按状态查询 |
| idx_start_date | 普通 | start_date | 按日期查询 |
| idx_create_time | 普通 | create_time | 按创建时间排序 |

#### 3.2.2 比赛项目表 (sport_item)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 项目ID |
| event_id | BIGINT | NO | - | 赛事ID |
| item_name | VARCHAR(100) | NO | - | 项目名称 |
| item_type | VARCHAR(50) | YES | NULL | 项目类型(字典) |
| gender_limit | TINYINT | YES | 0 | 性别限制: 0-不限 1-仅男 2-仅女 |
| max_participants | INT | YES | 0 | 最大参赛人数 |
| result_type | VARCHAR(20) | YES | 'SCORE' | 成绩类型: TIME/DISTANCE/SCORE |
| result_unit | VARCHAR(20) | YES | NULL | 成绩单位 |
| sort_rule | VARCHAR(10) | YES | 'DESC' | 排序规则: ASC/DESC |
| description | TEXT | YES | NULL | 项目描述 |
| rules | TEXT | YES | NULL | 比赛规则 |
| sort_order | INT | NO | 0 | 排序 |
| status | TINYINT | NO | 1 | 状态 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

---

### 3.3 报名管理模块

#### 3.3.1 报名表 (registration)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 报名ID |
| event_id | BIGINT | NO | - | 赛事ID |
| sport_item_id | BIGINT | NO | - | 项目ID |
| user_id | BIGINT | NO | - | 用户ID |
| status | TINYINT | NO | 1 | 状态: 1-待审核 2-已通过 3-已拒绝 4-已取消 |
| reject_reason | VARCHAR(255) | YES | NULL | 拒绝原因 |
| audit_by | BIGINT | YES | NULL | 审核人ID |
| audit_time | DATETIME | YES | NULL | 审核时间 |
| remark | VARCHAR(255) | YES | NULL | 备注 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| uk_user_item | 唯一 | user_id, sport_item_id, is_deleted | 防止重复报名 |
| idx_event_id | 普通 | event_id | 按赛事查询 |
| idx_sport_item_id | 普通 | sport_item_id | 按项目查询 |
| idx_user_id | 普通 | user_id | 按用户查询 |
| idx_status | 普通 | status | 按状态查询 |

---

### 3.4 赛程管理模块

#### 3.4.1 赛程表 (schedule)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 赛程ID |
| event_id | BIGINT | NO | - | 赛事ID |
| sport_item_id | BIGINT | NO | - | 项目ID |
| schedule_name | VARCHAR(100) | YES | NULL | 赛程名称 |
| round_no | INT | YES | 1 | 轮次 |
| group_no | VARCHAR(20) | YES | NULL | 组别 |
| match_date | DATE | NO | - | 比赛日期 |
| start_time | TIME | NO | - | 开始时间 |
| end_time | TIME | YES | NULL | 结束时间 |
| location | VARCHAR(200) | YES | NULL | 比赛场地 |
| referee | VARCHAR(100) | YES | NULL | 裁判 |
| status | TINYINT | NO | 1 | 状态: 1-未开始 2-进行中 3-已结束 |
| remark | VARCHAR(255) | YES | NULL | 备注 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

---

### 3.5 成绩管理模块

#### 3.5.1 成绩表 (result)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 成绩ID |
| event_id | BIGINT | NO | - | 赛事ID |
| sport_item_id | BIGINT | NO | - | 项目ID |
| schedule_id | BIGINT | YES | NULL | 赛程ID |
| registration_id | BIGINT | NO | - | 报名ID |
| user_id | BIGINT | NO | - | 用户ID |
| score | DECIMAL(10,2) | YES | NULL | 成绩值 |
| score_text | VARCHAR(50) | YES | NULL | 成绩文本 |
| ranking | INT | YES | NULL | 名次 |
| points | INT | YES | 0 | 积分 |
| record_by | BIGINT | YES | NULL | 录入人ID |
| record_time | DATETIME | YES | NULL | 录入时间 |
| remark | VARCHAR(255) | YES | NULL | 备注 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

**索引设计：**
| 索引名 | 类型 | 字段 | 说明 |
|--------|------|------|------|
| uk_registration | 唯一 | registration_id, is_deleted | 一个报名一条成绩 |
| idx_event_id | 普通 | event_id | 按赛事查询 |
| idx_sport_item_id | 普通 | sport_item_id | 按项目查询 |
| idx_ranking | 普通 | ranking | 按名次排序 |

---

### 3.6 公告管理模块

#### 3.6.1 公告表 (news)

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|--------|------|--------|--------|------|
| id | BIGINT | NO | AUTO_INCREMENT | 公告ID |
| title | VARCHAR(200) | NO | - | 标题 |
| news_type | VARCHAR(50) | YES | NULL | 公告类型(字典) |
| content | TEXT | YES | NULL | 内容 |
| cover_image | VARCHAR(255) | YES | NULL | 封面图片 |
| event_id | BIGINT | YES | NULL | 关联赛事ID |
| is_top | TINYINT | NO | 0 | 是否置顶 |
| view_count | INT | NO | 0 | 浏览次数 |
| publish_time | DATETIME | YES | NULL | 发布时间 |
| status | TINYINT | NO | 1 | 状态: 0-草稿 1-已发布 |
| is_deleted | TINYINT | NO | 0 | 是否删除 |
| create_by | BIGINT | YES | NULL | 创建人ID |
| create_time | DATETIME | NO | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | NO | CURRENT_TIMESTAMP | 更新时间 |

---

## 4. 索引策略

### 4.1 索引设计原则

1. **主键索引**：所有表使用 BIGINT 自增主键
2. **唯一索引**：业务唯一字段（用户名、学号、报名记录）
3. **普通索引**：高频查询字段（状态、类型、外键）
4. **复合索引**：多条件查询场景

### 4.2 索引汇总

| 表名 | 索引名 | 类型 | 字段 | 用途 |
|------|--------|------|------|------|
| sys_user | uk_username | 唯一 | username | 登录查询 |
| sys_user | uk_student_no | 唯一 | student_no | 学号查询 |
| sys_user | idx_role | 普通 | role | 按角色筛选 |
| event | idx_status | 普通 | status | 按状态筛选 |
| event | idx_start_date | 普通 | start_date | 按日期排序 |
| sport_item | idx_event_id | 普通 | event_id | 按赛事查询 |
| registration | uk_user_item | 唯一 | user_id, sport_item_id | 防重复报名 |
| registration | idx_status | 普通 | status | 按状态筛选 |
| result | uk_registration | 唯一 | registration_id | 一报名一成绩 |
| result | idx_ranking | 普通 | ranking | 排名查询 |
| news | idx_is_top | 普通 | is_top | 置顶排序 |

---

## 5. 字典数据

### 5.1 字典类型

| 字典编码 | 字典名称 |
|----------|----------|
| event_type | 赛事类型 |
| event_status | 赛事状态 |
| item_type | 项目类型 |
| registration_status | 报名状态 |
| news_type | 公告类型 |
| gender | 性别 |
| result_type | 成绩类型 |

### 5.2 字典数据明细

#### 赛事类型 (event_type)
| 值 | 标签 |
|----|------|
| TRACK_FIELD | 田径运动会 |
| BASKETBALL | 篮球联赛 |
| FOOTBALL | 足球联赛 |
| BADMINTON | 羽毛球比赛 |
| TABLE_TENNIS | 乒乓球比赛 |
| SWIMMING | 游泳比赛 |
| OTHER | 其他 |

#### 赛事状态 (event_status)
| 值 | 标签 |
|----|------|
| 1 | 草稿 |
| 2 | 报名中 |
| 3 | 进行中 |
| 4 | 已结束 |

#### 报名状态 (registration_status)
| 值 | 标签 |
|----|------|
| 1 | 待审核 |
| 2 | 已通过 |
| 3 | 已拒绝 |
| 4 | 已取消 |

#### 公告类型 (news_type)
| 值 | 标签 |
|----|------|
| NOTICE | 赛事通知 |
| RESULT | 成绩公告 |
| RULE | 规则说明 |
| OTHER | 其他 |

---

## 6. 初始化数据

### 6.1 管理员账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ADMIN |

### 6.2 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| teacher01 | admin123 | TEACHER | 测试教师 |
| student01 | admin123 | STUDENT | 测试学生1 |
| student02 | admin123 | STUDENT | 测试学生2 |
| student03 | admin123 | STUDENT | 测试学生3 |

---

## 7. 附录

### 7.1 修订历史

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0.0 | 2024-01 | Winston (Architect) | 初始版本 |

---

*文档结束*