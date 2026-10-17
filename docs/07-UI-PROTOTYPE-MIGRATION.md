# 校运会网页版系统迁移说明

## 目标分支
`feat/migrate-campus-meet-web-prototype`，基于现有开发分支 `codex/campus-meet-checkin-nav-validated` 创建，不直接修改 `main`。

## 本次迁移
- 将独立 HTML 原型的深绿色导航、赛事上下文、模块分组和快捷入口迁移到 Vue 3 + Element Plus 现有布局。
- 在赛事总览中增加赛前工作流程入口，同时保留原有后端统计和 ECharts 数据加载。
- 新增“秩序册生成”向导，读取既有赛事、项目、赛程接口，按赛程排序预览，并调用 `GET /api/reports/order-book.csv?eventId=...` 下载 CSV。
- 沿用现有 JWT 请求拦截器、Pinia 用户状态和路由角色权限，不引入第二套本地演示数据。

## 功能边界
- 项目、报名、赛程、竞赛执行、成绩复核与团体积分仍由原有业务页面和后端接口负责。
- 秩序册向导是编排流程入口和导出页面，不代替现有赛程、名单生成或冲突检查服务。
- 本次不新增数据库表，不执行数据库迁移，不覆盖既有报名或成绩数据。
- 正式印发前仍需用学校当届秩序册核对组别、赛次、晋级规则、场地和接力名单。

## 本地验证
在仓库根目录分别运行：
- `cd frontend && npm ci && npm run build:check`
- `cd backend && mvn -B clean verify`

该分支需要通过 GitHub Actions 构建和人工业务验收后再考虑合并。构建状态以本分支最新提交对应的工作流结果为准。
