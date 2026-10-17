# Railway 部署与学生个人检录验收

本方案将 Vue 前端、Spring Boot 后端和 MySQL 部署为三个 Railway 服务。前端由 Nginx 提供静态页面并将 `/api/*` 同源转发到后端私有网络，浏览器只需要访问前端公网域名。

## 前置条件

- GitHub 仓库：`zhoilj/Sports-EvenSports-Events-Campusts`
- 部署分支：`codex/campus-meet-railway-deploy`
- Railway 账号；费用/额度以 Railway 控制台当前显示为准。
- 必须使用新建的空数据库，不要把演示流程接入学校现有业务数据库。

## 1. 创建数据库

在 Railway 新建 Project，添加 MySQL 服务，服务名称建议设为 `MySQL`。从 Variables / Connect 页面取得数据库主机、端口、用户名、密码和数据库名。不要把真实连接信息写进 GitHub。

## 2. 部署后端

添加 GitHub 服务，选择此仓库和分支 `codex/campus-meet-railway-deploy`，服务名设为 `backend`，Root Directory 为 `/backend`，使用该目录的 Dockerfile。

在 backend Variables 中设置（变量引用里的 `MySQL` 必须与数据库服务名称完全一致）：

```text
PORT=8080
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
JWT_SECRET=<在 Railway 控制台单独设置的随机强密钥，至少 32 字节>
```

不要将真实密码或 JWT_SECRET 提交到仓库。后端容器监听 8080，前端通过 Railway 私有域名访问它。通常不需要为 backend 单独生成公网域名。

## 3. 初始化空数据库

原始 `docs/04-DB/schema.sql` 含 `DROP TABLE`，`backend/src/main/resources/db/data.sql` 含 `TRUNCATE TABLE`。不要直接在已有数据库运行它们。本目录的 `01_schema.sql` 移除了 DROP、CREATE DATABASE 和 USE 指令，但仍只应在一个新建的空数据库上执行一次。

使用 Navicat / MySQL 客户端连接 Railway MySQL，选中该服务分配的数据库，按顺序运行：

1. `docs/deployment/01_schema.sql`
2. `backend/src/main/resources/db/migration/V1__campus_track_meet_additive.sql`
3. `backend/src/main/resources/db/migration/V2__registration_compatibility_and_relay_config.sql`
4. `backend/src/main/resources/db/migration/V3__competition_operations.sql`
5. `docs/deployment/03_demo_seed.sql`

数据库连接信息从 Railway Connect 页面获取。若 Navicat 需要临时开启公网连接，完成导入后关闭公网访问。确认当前选择的是刚建的演示数据库。不要把默认管理员账号直接用于生产环境。

## 4. 部署前端

再添加一个 GitHub 服务，选择相同仓库和分支，服务名设为 `frontend`，Root Directory 为 `/frontend`，使用该目录的 Dockerfile。

在 frontend Variables 中设置：

```text
BACKEND_HOST=${{backend.RAILWAY_PRIVATE_DOMAIN}}
BACKEND_PORT=8080
```

确保 `backend` 与后端服务名称一致。等待构建成功后，只给 frontend 服务生成公网域名。该域名就是师生访问系统的入口。

## 5. 学生个人检录验收

演示 SQL 会创建测试账号和一条已审核、已编排且有检录记录的个人项目：

- 用户名：`student_demo`
- 密码：`123456`
- 赛事：部署验证专用校运会
- 项目：男子100米（部署验证）
- 场次：男子100米预赛（部署验证）
- 比赛时间：2026-10-19 09:00
- 场地：田径场1号跑道
- 检录状态：`PENDING`
- 检录编号：由 `schedule_entry.id` 动态生成，格式为 `CK` 加八位数字

验收步骤：

1. 打开 frontend 公网域名并进入 `/login`。
2. 使用 `student_demo / 123456` 登录。
3. 进入左侧“我的参赛” → “我的赛程与检录号”。
4. 页面应显示上面的测试赛程、检录编号、道次和待检录状态。
5. 浏览器开发者工具 Network 中，`GET /api/checkins/my` 应返回 HTTP 200，且包含该学生的测试赛事。
6. 未登录访问接口应被拒绝；使用非学生角色调用该学生接口应被权限规则拒绝。
7. 使用另一个学生账号登录时，不应看到 `student_demo` 的个人报名记录。

演示账号只用于验收。正式投入使用前应删除演示数据或更改密码，并配置学校实际用户体系。

## 常见问题

- **502 / Bad Gateway**：检查 frontend 的 `BACKEND_HOST` 是否引用了正确的 backend 私有域名、后端是否启动，以及两边端口是否都是 8080。
- **数据库连接失败**：核对 DB_URL、DB_USERNAME、DB_PASSWORD 的 Railway 变量引用；确认已执行 schema 和 V1–V3。
- **登录失败**：确认 demo seed 已成功执行、用户状态为 1，且 JWT_SECRET 已设置。
- **页面刷新 404**：确认使用本目录的 frontend Dockerfile 和 Nginx 模板。
- **检录页为空**：先确认演示 SQL 最后的 SELECT 返回一行，再检查 `schedule_entry` 和 `checkin_record` 表。
