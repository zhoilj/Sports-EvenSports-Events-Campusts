
# 架构设计文档 (Architecture)

> 校园体育赛事管理系统 - 系统架构与技术设计

**文档版本**：v1.0.0  
**创建日期**：2024-01  
**文档状态**：✅ 已评审  
**负责人**：架构师 Winston

---

## 1. 系统架构概述

### 1.1 架构目标

| 目标 | 说明 |
|------|------|
| 可维护性 | 清晰的分层架构，模块职责单一 |
| 可扩展性 | 支持功能模块水平扩展 |
| 可测试性 | 各层可独立测试 |
| 安全性 | JWT认证 + RBAC权限控制 |
| 性能 | 支持100+并发用户 |

### 1.2 整体架构图

```
+-------------------------------------------------------------------------+
|                              客户端层                                    |
|  +-----------------+  +-----------------+  +-----------------+          |
|  |   PC浏览器       |  |   平板浏览器     |  |   手机浏览器     |          |
|  |  (1920px+)      |  |   (768px+)      |  |   (375px+)      |          |
|  +--------+--------+  +--------+--------+  +--------+--------+          |
+-----------|--------------------|--------------------|--------------------|
            |                    |                    |
            +--------------------+--------------------+
                                 | HTTP/HTTPS
                                 v
+-------------------------------------------------------------------------+
|                              前端层 (Vue3)                               |
|  +-------------------------------------------------------------------+  |
|  |                     Vue3 + Element Plus                           |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |Vue Router| |  Axios   | |  Pinia   | | ECharts  |              |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
                                 | RESTful API
                                 v
+-------------------------------------------------------------------------+
|                              后端层 (SpringBoot)                         |
|  +-------------------------------------------------------------------+  |
|  |                        Controller 层                              |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |AuthCtrl  | |EventCtrl | |RegCtrl   | |ResultCtrl|  ...         |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
|  +-------------------------------------------------------------------+  |
|  |                        Service 层                                 |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |AuthSvc   | |EventSvc  | |RegSvc    | |ResultSvc |  ...         |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
|  +-------------------------------------------------------------------+  |
|  |                        Mapper 层 (MyBatis-Plus)                   |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |UserMapper| |EventMapper| |RegMapper | |ResultMapper| ...       |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
|  +-------------------------------------------------------------------+  |
|  |                        横切关注点                                  |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |Security  | |Exception | |Validation| |  Log     |              |  |
|  |  |Filter    | |Handler   | |  AOP     | |  AOP     |              |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
                                 | JDBC
                                 v
+-------------------------------------------------------------------------+
|                              数据层                                      |
|  +-------------------------------------------------------------------+  |
|  |                        MySQL 8.0                                  |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  |  |sys_user  | |event     | |registration| |result   |  ...        |  |
|  |  +----------+ +----------+ +----------+ +----------+              |  |
|  +-------------------------------------------------------------------+  |
+-------------------------------------------------------------------------+
```

---

## 2. 分层架构设计

### 2.1 后端分层架构

```
backend/
├── src/main/java/com/sports/
│   ├── SportEventsApplication.java      # 启动类
│   ├── common/                          # 公共模块
│   │   ├── config/                      # 配置类
│   │   │   ├── SecurityConfig.java      # Spring Security配置
│   │   │   ├── CorsConfig.java          # 跨域配置
│   │   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus配置
│   │   │   └── SwaggerConfig.java       # Swagger配置
│   │   ├── exception/                   # 异常处理
│   │   │   ├── BusinessException.java   # 业务异常
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── result/                      # 统一响应
│   │   │   ├── Result.java              # 响应封装
│   │   │   └── ResultCode.java          # 响应码枚举
│   │   ├── utils/                       # 工具类
│   │   │   ├── JwtUtils.java            # JWT工具
│   │   │   ├── ExcelUtils.java          # Excel工具
│   │   │   └── PageUtils.java           # 分页工具
│   │   └── constant/                    # 常量定义
│   │       └── Constants.java
│   ├── security/                        # 安全模块
│   │   ├── JwtAuthenticationFilter.java # JWT过滤器
│   │   ├── JwtTokenProvider.java        # Token提供者
│   │   ├── UserDetailsServiceImpl.java  # 用户详情服务
│   │   └── SecurityUser.java            # 安全用户
│   ├── modules/                         # 业务模块
│   │   ├── auth/                        # 认证模块
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   └── dto/
│   │   ├── system/                      # 系统管理模块
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── event/                       # 赛事模块
│   │   ├── sportitem/                   # 项目模块
│   │   ├── registration/                # 报名模块
│   │   ├── schedule/                    # 赛程模块
│   │   ├── result/                      # 成绩模块
│   │   ├── news/                        # 公告模块
│   │   └── statistics/                  # 统计模块
│   └── resources/
│       ├── application.yml              # 主配置
│       ├── application-dev.yml          # 开发环境配置
│       ├── application-prod.yml         # 生产环境配置
│       └── mapper/                      # MyBatis XML
└── pom.xml
```

### 2.2 各层职责

| 层级 | 职责 | 规范 |
|------|------|------|
| **Controller** | 接收请求、参数校验、调用Service、返回响应 | 不包含业务逻辑，仅做请求转发 |
| **Service** | 业务逻辑处理、事务管理、调用Mapper | 核心业务逻辑所在，可调用多个Mapper |
| **Mapper** | 数据库访问、SQL执行 | 仅做数据访问，不包含业务逻辑 |
| **Entity** | 数据库表映射实体 | 与数据库表一一对应 |
| **DTO** | 数据传输对象 | 用于Controller与Service之间的数据传输 |
| **VO** | 视图对象 | 用于返回给前端的数据封装 |

### 2.3 前端分层架构

```
frontend/
├── public/
│   └── index.html
├── src/
│   ├── main.js                          # 入口文件
│   ├── App.vue                          # 根组件
│   ├── api/                             # API接口
│   │   ├── auth.js                      # 认证接口
│   │   ├── event.js                     # 赛事接口
│   │   ├── registration.js              # 报名接口
│   │   ├── result.js                    # 成绩接口
│   │   └── index.js                     # 接口汇总
│   ├── assets/                          # 静态资源
│   │   ├── images/
│   │   └── styles/
│   ├── components/                      # 公共组件
│   │   ├── common/                      # 通用组件
│   │   │   ├── Pagination.vue
│   │   │   ├── SearchForm.vue
│   │   │   └── UploadFile.vue
│   │   └── layout/                      # 布局组件
│   │       ├── Header.vue
│   │       ├── Sidebar.vue
│   │       └── Footer.vue
│   ├── router/                          # 路由配置
│   │   └── index.js
│   ├── store/                           # 状态管理 (Pinia)
│   │   ├── modules/
│   │   │   ├── user.js
│   │   │   └── app.js
│   │   └── index.js
│   ├── utils/                           # 工具函数
│   │   ├── request.js                   # Axios封装
│   │   ├── auth.js                      # Token管理
│   │   ├── validate.js                  # 表单校验
│   │   └── index.js
│   ├── views/                           # 页面视图
│   │   ├── login/                       # 登录页
│   │   ├── dashboard/                   # 首页/统计
│   │   ├── event/                       # 赛事管理
│   │   │   ├── list.vue
│   │   │   └── form.vue
│   │   ├── sportitem/                   # 项目管理
│   │   ├── registration/                # 报名管理
│   │   ├── schedule/                    # 赛程管理
│   │   ├── result/                      # 成绩管理
│   │   ├── news/                        # 公告管理
│   │   └── system/                      # 系统管理
│   │       ├── user/
│   │       └── dict/
│   └── permission.js                    # 权限控制
├── package.json
└── vite.config.js
```

---

## 3. 模块边界定义

### 3.1 模块依赖关系

```
+---------------------------------------------------------------+
|                         业务模块依赖图                         |
+---------------------------------------------------------------+
|                                                               |
|   +----------+                                                |
|   |  auth    | <-----------------------------------------+    |
|   +----------+                                           |    |
|        |                                                 |    |
|        v                                                 |    |
|   +----------+      +----------+      +----------+       |    |
|   |  system  | <--- |  event   | <--- |sportitem |       |    |
|   +----------+      +----------+      +----------+       |    |
|        |                 |                 |             |    |
|        |                 v                 v             |    |
|        |           +----------+      +----------+        |    |
|        |           | schedule |      |registration| -----+    |
|        |           +----------+      +----------+             |
|        |                 |                 |                  |
|        |                 +--------+--------+                  |
|        |                          v                           |
|        |                    +----------+                      |
|        |                    |  result  |                      |
|        |                    +----------+                      |
|        |                          |                           |
|        |                          v                           |
|        |                    +----------+                      |
|        +----------------->  |statistics|                      |
|                             +----------+                      |
|                                                               |
|   +----------+                                                |
|   |   news   | (独立模块，无依赖)                              |
|   +----------+                                                |
|                                                               |
+---------------------------------------------------------------+
```

### 3.2 模块接口边界

| 模块 | 对外接口 | 依赖模块 |
|------|----------|----------|
| **auth** | 登录、登出、获取当前用户、刷新Token | system |
| **system** | 用户CRUD、字典CRUD、配置管理 | - |
| **event** | 赛事CRUD、状态变更 | system(字典) |
| **sportitem** | 项目CRUD | event |
| **registration** | 报名CRUD、审核、我的报名 | event, sportitem, auth |
| **schedule** | 赛程CRUD | event, sportitem |
| **result** | 成绩CRUD、排名、Excel导入导出 | registration, schedule |
| **news** | 公告CRUD | system(字典) |
| **statistics** | 报名统计、成绩统计 | event, registration, result |

### 3.3 模块通信规则

```
规则1：模块间通过Service层调用，禁止Controller直接调用其他模块Mapper
规则2：跨模块查询使用DTO传输，禁止直接暴露Entity
规则3：模块内部可直接使用Entity
规则4：公共模块(common)可被所有业务模块依赖
规则5：业务模块间禁止循环依赖
```

---

## 4. 技术选型说明

### 4.1 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **JDK** | 17 | LTS版本，SpringBoot 3.x要求 |
| **SpringBoot** | 3.2.x | 主框架 |
| **Spring Security** | 6.x | 安全框架 |
| **MyBatis-Plus** | 3.5.x | ORM框架，简化CRUD |
| **MySQL** | 8.0 | 关系型数据库 |
| **Druid** | 1.2.x | 数据库连接池 |
| **JWT** | 0.12.x (jjwt) | Token认证 |
| **Validation** | 3.x | 参数校验 |
| **Lombok** | 1.18.x | 简化代码 |
| **Hutool** | 5.8.x | 工具类库 |
| **EasyExcel** | 3.3.x | Excel处理 |
| **Knife4j** | 4.x | API文档 |

### 4.2 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| **Vue** | 3.4.x | 前端框架 |
| **Vite** | 5.x | 构建工具 |
| **Element Plus** | 2.5.x | UI组件库 |
| **Vue Router** | 4.x | 路由管理 |
| **Pinia** | 2.x | 状态管理 |
| **Axios** | 1.6.x | HTTP客户端 |
| **ECharts** | 5.x | 图表库 |
| **SCSS** | - | CSS预处理器 |

### 4.3 技术选型理由

| 选型 | 理由 |
|------|------|
| SpringBoot 3.x | 最新LTS，性能优化，原生支持GraalVM |
| MyBatis-Plus | 比JPA更灵活，CRUD代码生成，分页插件 |
| JWT | 无状态认证，适合前后端分离 |
| Vue3 + Vite | 更好的性能，Composition API更灵活 |
| Element Plus | 成熟稳定，组件丰富，文档完善 |
| EasyExcel | 阿里出品，大数据量Excel处理性能好 |

---

## 5. 权限设计方案

### 5.1 认证流程

```
Client              Controller           Service             Database
  |                     |                    |                    |
  | 1. POST /auth/login |                    |                    |
  | {username,password} |                    |                    |
  |-------------------->|                    |                    |
  |                     | 2. login()         |                    |
  |                     |------------------->|                    |
  |                     |                    | 3. 查询用户        |
  |                     |                    |------------------->|
  |                     |                    |<-------------------|
  |                     |                    | 4. 验证密码(BCrypt)|
  |                     | 5. 生成JWT         |                    |
  |                     |<-------------------|                    |
  | 6. 返回Token        |                    |                    |
  |<--------------------|                    |                    |
  |                     |                    |                    |
  | 7. 请求业务接口      |                    |                    |
  | Authorization:Bearer|                    |                    |
  |-------------------->|                    |                    |
  |                     | 8. JwtFilter验证   |                    |
  |                     | 解析用户信息        |                    |
  |                     | 设置SecurityContext|                    |
  | 9. 返回数据         |                    |                    |
  |<--------------------|                    |                    |
```

### 5.2 JWT Token 结构

```json
{
  "header": {
    "alg": "HS512",
    "typ": "JWT"
  },
  "payload": {
    "sub": "1",
    "username": "admin",
    "role": "ADMIN",
    "iat": 1704067200,
    "exp": 1704153600
  },
  "signature": "..."
}
```

### 5.3 权限控制实现

#### 5.3.1 后端权限注解

```java
// 角色控制
@PreAuthorize("hasRole('ADMIN')")
public Result<?> createEvent(...) { }

// 多角色控制
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public Result<?> inputResult(...) { }

// 自定义权限表达式
@PreAuthorize("@permissionService.hasPermission('event:create')")
public Result<?> createEvent(...) { }
```

#### 5.3.2 前端权限控制

```javascript
// 路由权限
{
  path: '/event',
  meta: { roles: ['ADMIN'] }
}

// 按钮权限
<el-button v-if="hasRole('ADMIN')">新增</el-button>

// 权限指令
<el-button v-permission="['ADMIN', 'TEACHER']">录入成绩</el-button>
```

### 5.4 数据权限设计

| 角色 | 数据范围 | 实现方式 |
|------|----------|----------|
| ADMIN | 全部数据 | 无过滤 |
| TEACHER | 全部数据（只读为主） | 无过滤 |
| STUDENT | 仅自己的数据 | SQL自动拼接 `user_id = #{currentUserId}` |

---

## 6. 关键流程时序图

### 6.1 学生报名流程

```
Student         Frontend        Controller       Service          Database
  |                |                |                |                |
  | 1.点击报名      |                |                |                |
  |--------------->|                |                |                |
  |                | 2.GET /event/{id}               |                |
  |                |--------------->|                |                |
  |                |                | 3.getById()    |                |
  |                |                |--------------->|                |
  |                |                |                | 4.SELECT       |
  |                |                |                |--------------->|
  |                |                |                |<---------------|
  |                |<---------------|<---------------|                |
  | 5.显示赛事详情  |                |                |                |
  |<---------------|                |                |                |
  |                |                |                |                |
  | 6.选择项目确认  |                |                |                |
  |--------------->|                |                |                |
  |                | 7.POST /registration            |                |
  |                | {eventId,sportItemId}           |                |
  |                |--------------->|                |                |
  |                |                | 8.create()     |                |
  |                |                |--------------->|                |
  |                |                |                | 9.校验:        |
  |                |                |                | -赛事状态      |
  |                |                |                | -是否重复      |
  |                |                |                | -人数限制      |
  |                |                |                | 10.INSERT      |
  |                |                |                |--------------->|
  |                |                |                |<---------------|
  |                |<---------------|<---------------|                |
  | 11.报名成功     |                |                |                |
  |<---------------|                |                |                |
```

### 6.2 成绩录入流程

```
Teacher         Frontend        Controller       Service          Database
  |                |                |                |                |
  | 1.选择赛程      |                |                |                |
  |--------------->|                |                |                |
  |                | 2.GET /schedule/{id}/participants               |
  |                |--------------->|                |                |
  |                |                |--------------->|                |
  |                |                |                |--------------->|
  |                |<---------------|<---------------|<---------------|
  | 3.显示参赛名单  |                |                |                |
  |<---------------|                |                |                |
  |                |                |                |                |
  | 4.录入成绩      |                |                |                |
  |--------------->|                |                |                |
  |                | 5.POST /result |                |                |
  |                | {registrationId,score}          |                |
  |                |--------------->|                |                |
  |                |                | 6.create()     |                |
  |                |                |--------------->|                |
  |                |                |                | 7.校验:        |
  |                |                |                | -报名状态      |
  |                |                |                | -是否已录入    |
  |                |                |                | 8.INSERT       |
  |                |                |                |--------------->|
  |                |                |                |<---------------|
  |                |                |                | 9.计算排名     |
  |                |                |                |--------------->|
  |                |<---------------|<---------------|<---------------|
  | 10.录入成功     |                |                |                |
  |<---------------|                |                |                |
```

### 6.3 Excel导入成绩流程

```
Teacher         Frontend        Controller       Service          Database
  |                |                |                |                |
  | 1.上传Excel    |                |                |                |
  |--------------->|                |                |                |
  |                | 2.POST /result/import           |                |
  |                | (multipart/form-data)           |                |
  |                |--------------->|                |                |
  |                |                | 3.import()     |                |
  |                |                |--------------->|                |
  |                |                |                | 4.解析Excel    |
  |                |                |                | (EasyExcel)    |
  |                |                |                |                |
  |                |                |                | 5.逐行校验     |
  |                |                |                | -学号存在      |
  |                |                |                | -姓名匹配      |
  |                |                |                | -成绩有效      |
  |                |                |                |                |
  |                |                |                | 6.批量INSERT   |
  |                |                |                |--------------->|
  |                |                |                |<---------------|
  |                |<---------------|<---------------|                |
  | 7.返回导入结果  |                |                |                |
  | (成功/失败数)   |                |                |                |
  |<---------------|                |                |                |
```

---

## 7. 部署架构

### 7.1 开发环境

```
+-----------------------------------------------------------+
|                      开发者本机                            |
|  +-------------+  +-------------+  +-------------+        |
|  |   VS Code   |  |   IDEA      |  |   MySQL     |        |
|  |  (Frontend) |  |  (Backend)  |  |   8.0       |        |
|  |  localhost  |  |  localhost  |  |  localhost  |        |
|  |  :5173      |  |  :8080      |  |  :3306      |        |
|  +-------------+  +-------------+  +-------------+        |
+-----------------------------------------------------------+
```

### 7.2 生产环境（单机部署）

```
+-----------------------------------------------------------+
|                      Linux Server                          |
|  +-----------------------------------------------------+  |
|  |                    Nginx (80/443)                   |  |
|  |  +-------------------+  +-------------------+       |  |
|  |  |  静态资源 /dist/* |  | 反向代理 /api/*   |       |  |
|  |  +-------------------+  +-------------------+       |  |
|  +-----------------------------------------------------+  |
|                           |                               |
|                           v                               |
|  +-----------------------------------------------------+  |
|  |              SpringBoot Application (8080)          |  |
|  |              java -jar app.jar                      |  |
|  +-----------------------------------------------------+  |
|                           |                               |
|                           v                               |
|  +-----------------------------------------------------+  |
|  |                    MySQL 8.0 (3306)                 |  |
|  +-----------------------------------------------------+  |
+-----------------------------------------------------------+
```

### 7.3 Nginx 配置示例

```nginx
server {
    listen 80;
    server_name sports.example.com;
    
    # 前端静态资源
    location / {
        root /var/www/sports/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # 后端API代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
    
    # 文件上传
    location /upload/ {
        alias /var/www/sports/upload/;
    }
}
```

---

## 8. 安全设计

### 8.1 安全措施清单

| 安全威胁 | 防护措施 |
|----------|----------|
| **身份伪造** | JWT Token认证，Token签名验证 |
| **密码泄露** | BCrypt加密存储，传输使用HTTPS |
| **SQL注入** | MyBatis-Plus参数化查询，禁止拼接SQL |
| **XSS攻击** | 前端输入过滤，后端输出转义 |
| **CSRF攻击** | JWT无状态认证，无需CSRF Token |
| **越权访问** | RBAC权限控制，数据权限过滤 |
| **暴力破解** | 登录失败次数限制，账号锁定 |
| **敏感数据** | 日志脱敏，接口返回脱敏 |

### 8.2 密码安全

```java
// 密码加密
String encodedPassword = new BCryptPasswordEncoder().encode(rawPassword);

// 密码验证
boolean matches = new BCryptPasswordEncoder().matches(rawPassword, encodedPassword);
```

### 8.3 接口安全

```java
// 统一响应，不暴露内部错误
@ExceptionHandler(Exception.class)
public Result<?> handleException(Exception e) {
    log.error("系统异常", e);
    return Result.error("系统繁忙，请稍后重试");
}

// 参数校验
@PostMapping("/event")
public Result<?> create(@Valid @RequestBody EventDTO dto) { }
```

---

## 9. 性能设计

### 9.1 数据库优化

| 优化项 | 措施 |
|--------|------|
| 索引设计 | 查询字段建立索引，避免全表扫描 |
| 分页查询 | 使用MyBatis-Plus分页插件，限制每页数量 |
| 连接池 | Druid连接池，配置合理的连接数 |
| 慢SQL监控 | Druid监控，记录慢SQL |

### 9.2 接口优化

| 优化项 | 措施 |
|--------|------|
| 响应压缩 | Gzip压缩响应数据 |
| 字  
�βü� | ���践���ֶΣ�����SELECT * |  
| �������� | ��������/���£��������ݿ⽻�� | 
### 9.3 前端优化

| 优化项 | 措施 |
|--------|------|
| 代码分割 | 路由懒加载，按需加载组件 |
| 资源压缩 | Vite构建压缩JS/CSS |
| 缓存策略 | 静态资源设置缓存头 |
| 图片优化 | 图片压缩，使用WebP格式 |

---

## 10. 错误处理设计

### 10.1 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": 1704067200000
}
```

### 10.2 错误码定义

| 错误码 | 说明 | HTTP状态码 |
|--------|------|------------|
| 200 | 成功 | 200 |
| 400 | 参数错误 | 400 |
| 401 | 未认证 | 401 |
| 403 | 无权限 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 系统错误 | 500 |
| 1001 | 用户名或密码错误 | 200 |
| 1002 | Token已过期 | 200 |
| 2001 | 赛事不存在 | 200 |
| 2002 | 赛事状态不允许此操作 | 200 |
| 3001 | 重复报名 | 200 |
| 3002 | 报名已截止 | 200 |

### 10.3 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
```

---

## 11. 附录

### 11.1 技术栈版本汇总

| 类别 | 技术 | 版本 |
|------|------|------|
| 后端 | JDK | 17 |
| | SpringBoot | 3.2.x |
| | MyBatis-Plus | 3.5.x |
| | Spring Security | 6.x |
| | MySQL | 8.0 |
| 前端 | Vue | 3.4.x |
| | Vite | 5.x |
| | Element Plus | 2.5.x |
| | ECharts | 5.x |

### 11.2 修订历史

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0.0 | 2024-01 | Winston (Architect) | 初始版本 |

---

*文档结束*