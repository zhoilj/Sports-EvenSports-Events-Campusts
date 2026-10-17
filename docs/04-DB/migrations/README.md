# 第一阶段数据库迁移

## 安全边界

- 先备份数据库，并在备份副本上执行迁移。
- `V001__phase1_registration_model.sql` 是增量迁移，不执行 `DROP TABLE` 或 `TRUNCATE TABLE`。
- 迁移会删除旧的唯一索引 `uk_user_item`（仅删除索引，不删除报名数据），新增按赛事/项目/运动员/状态查询的索引，并依据待审核和已通过的报名记录重算 `sport_item.current_participants`。
- 迁移会创建学院、班级主数据表，并根据现有用户的学院/班级文本回填标准化 ID。执行后请抽查回填结果。
- `docs/04-DB/schema.sql` 是全新空库建表脚本，包含 `DROP TABLE`，严禁用于已有业务数据库。
- 历史 `backend/src/main/resources/db/data.sql` 已停用，不再包含可执行 SQL；旧版本会清空业务表且字段映射不一致。
- 应用配置已将 `spring.sql.init.mode` 设为 `never`，初始化 SQL 不会随应用启动自动执行。
- 不要在生产环境启用 `local-password-reset` profile。JWT 密钥和数据库凭据必须通过环境变量注入。

## 执行前检查

1. 核对实际数据库的 `SHOW CREATE TABLE`，确认实体映射与运行数据库一致。
2. 备份数据库并在副本运行迁移。
3. 检查学院/班级映射、有效报名数量及报名记录主键是否保持不变。
4. 在测试环境完成个人报名、接力报名、审核、取消、限报和并发名额测试后再部署。
