
# API 接口文档

> 校园体育赛事管理系统 - RESTful API 设计

**文档版本**：v1.0.0  
**创建日期**：2024-01  
**文档状态**：✅ 已评审  
**负责人**：架构师 Winston  
**Base URL**：`http://localhost:8080/api`

---

## 1. 接口规范

### 1.1 请求规范

| 项目 | 说明 |
|------|------|
| 协议 | HTTP/HTTPS |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | JWT Bearer Token |

### 1.2 请求头

```http
Content-Type: application/json
Authorization: Bearer {token}
```

### 1.3 响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { },
  "timestamp": 1704067200000
}
```

### 1.4 分页响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10,
    "pages": 10
  },
  "timestamp": 1704067200000
}
```

### 1.5 错误码定义

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证/Token无效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 系统错误 |
| 1001 | 用户名或密码错误 |
| 1002 | Token已过期 |
| 1003 | 账号已被禁用 |
| 2001 | 赛事不存在 |
| 2002 | 赛事状态不允许此操作 |
| 3001 | 重复报名 |
| 3002 | 报名已截止 |
| 3003 | 报名人数已满 |
| 4001 | 成绩已存在 |
| 4002 | Excel格式错误 |

---

## 2. 认证模块 (Auth)

### 2.1 用户登录

**POST** `/auth/login`

**权限**：公开

**请求参数**：
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "user": {
      "id": 1,
      "username": "admin",
      "realName": "系统管理员",
      "role": "ADMIN",
      "avatar": null
    }
  }
}
```

**错误响应**：
```json
{
  "code": 1001,
  "message": "用户名或密码错误"
}
```

---

### 2.2 用户登出

**POST** `/auth/logout`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "message": "登出成功"
}
```

---

### 2.3 获取当前用户信息

**GET** `/auth/info`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "realName": "系统管理员",
    "gender": 1,
    "phone": "13800138000",
    "email": "admin@example.com",
    "avatar": null,
    "role": "ADMIN",
    "studentNo": null,
    "department": "体育部",
    "className": null
  }
}
```

---

### 2.4 修改密码

**PUT** `/auth/password`

**权限**：需认证

**请求参数**：
```json
{
  "oldPassword": "admin123",
  "newPassword": "newpass123"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "密码修改成功"
}
```

---

## 3. 用户管理模块 (User)

### 3.1 用户列表（分页）

**GET** `/system/user/page`

**权限**：ADMIN

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10 |
| username | string | 否 | 用户名（模糊查询） |
| realName | string | 否 | 真实姓名（模糊查询） |
| role | string | 否 | 角色筛选 |
| department | string | 否 | 院系筛选 |
| status | int | 否 | 状态筛选 |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "username": "admin",
        "realName": "系统管理员",
        "gender": 1,
        "phone": "13800138000",
        "role": "ADMIN",
        "department": "体育部",
        "status": 1,
        "createTime": "2024-01-01 00:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10,
    "pages": 1
  }
}
```

---

### 3.2 用户详情

**GET** `/system/user/{id}`

**权限**：ADMIN

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 2,
    "username": "student01",
    "realName": "李明",
    "gender": 1,
    "phone": "13800138001",
    "email": "liming@example.com",
    "role": "STUDENT",
    "studentNo": "2021001001",
    "department": "计算机学院",
    "className": "计算机2101班",
    "status": 1,
    "createTime": "2024-01-01 00:00:00"
  }
}
```

---

### 3.3 新增用户

**POST** `/system/user`

**权限**：ADMIN

**请求参数**：
```json
{
  "username": "student04",
  "password": "123456",
  "realName": "赵六",
  "gender": 1,
  "phone": "13800138004",
  "email": "zhaoliu@example.com",
  "role": "STUDENT",
  "studentNo": "2021003001",
  "department": "机械学院",
  "className": "机械2101班"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "新增成功",
  "data": {
    "id": 5
  }
}
```

---

### 3.4 修改用户

**PUT** `/system/user/{id}`

**权限**：ADMIN

**请求参数**：
```json
{
  "realName": "赵六（修改）",
  "phone": "13800138005",
  "department": "电子学院"
}
```

---

### 3.5 删除用户

**DELETE** `/system/user/{id}`

**权限**：ADMIN

**响应示例**：
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 3.6 重置密码

**PUT** `/system/user/{id}/reset-password`

**权限**：ADMIN

**响应示例**：
```json
{
  "code": 200,
  "message": "密码已重置为: 123456"
}
```

---

## 4. 字典管理模块 (Dict)

### 4.1 字典类型列表

**GET** `/system/dict/type/list`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "dictCode": "event_type",
      "dictName": "赛事类型"
    },
    {
      "id": 2,
      "dictCode": "event_status",
      "dictName": "赛事状态"
    }
  ]
}
```

---

### 4.2 根据字典编码获取字典数据

**GET** `/system/dict/data/{dictCode}`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "itemValue": "TRACK_FIELD",
      "itemLabel": "田径运动会"
    },
    {
      "itemValue": "BASKETBALL",
      "itemLabel": "篮球联赛"
    }
  ]
}
```

---

## 5. 赛事管理模块 (Event)

### 5.1 赛事列表（分页）

**GET** `/event/page`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| eventName | string | 否 | 赛事名称（模糊） |
| eventType | string | 否 | 赛事类型 |
| status | int | 否 | 状态 |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "eventName": "2024年春季运动会",
        "eventType": "TRACK_FIELD",
        "eventTypeName": "田径运动会",
        "startDate": "2024-04-15",
        "endDate": "2024-04-17",
        "regEndDate": "2024-04-10",
        "location": "校田径场",
        "status": 2,
        "statusName": "报名中",
        "itemCount": 5,
        "regCount": 120
      }
    ],
    "total": 1,
    "page": 1,
    "size": 10
  }
}
```

---

### 5.2 赛事详情

**GET** `/event/{id}`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "eventName": "2024年春季运动会",
    "eventType": "TRACK_FIELD",
    "eventTypeName": "田径运动会",
    "description": "一年一度的春季田径运动会",
    "coverImage": "/upload/event/cover1.jpg",
    "startDate": "2024-04-15",
    "endDate": "2024-04-17",
    "regStartDate": "2024-03-01",
    "regEndDate": "2024-04-10",
    "location": "校田径场",
    "organizer": "体育部",
    "contactPerson": "张老师",
    "contactPhone": "13800138000",
    "status": 2,
    "statusName": "报名中",
    "createTime": "2024-01-15 10:00:00"
  }
}
```

---

### 5.3 新增赛事

**POST** `/event`

**权限**：ADMIN

**请求参数**：
```json
{
  "eventName": "2024年篮球联赛",
  "eventType": "BASKETBALL",
  "description": "校园篮球联赛",
  "startDate": "2024-05-01",
  "endDate": "2024-05-15",
  "regStartDate": "2024-04-01",
  "regEndDate": "2024-04-25",
  "location": "校篮球馆",
  "organizer": "体育部",
  "contactPerson": "李老师",
  "contactPhone": "13800138001"
}
```

---

### 5.4 修改赛事

**PUT** `/event/{id}`

**权限**：ADMIN

---

### 5.5 删除赛事

**DELETE** `/event/{id}`

**权限**：ADMIN

---

### 5.6 变更赛事状态

**PUT** `/event/{id}/status`

**权限**：ADMIN

**请求参数**：
```json
{
  "status": 3
}
```

---

## 6. 比赛项目模块 (SportItem)

### 6.1 项目列表

**GET** `/sport-item/list`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 是 | 赛事ID |

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "eventId": 1,
      "itemName": "男子100米",
      "itemType": "TRACK",
      "itemTypeName": "径赛",
      "genderLimit": 1,
      "genderLimitName": "仅男",
      "maxParticipants": 50,
      "currentParticipants": 30,
      "resultType": "TIME",
      "resultUnit": "秒"
    }
  ]
}
```

---

### 6.2 项目详情

**GET** `/sport-item/{id}`

**权限**：需认证

---

### 6.3 新增项目

**POST** `/sport-item`

**权限**：ADMIN

**请求参数**：
```json
{
  "eventId": 1,
  "itemName": "女子800米",
  "itemType": "TRACK",
  "genderLimit": 2,
  "maxParticipants": 40,
  "resultType": "TIME",
  "resultUnit": "秒",
  "sortRule": "ASC",
  "description": "女子800米跑"
}
```

---

### 6.4 修改项目

**PUT** `/sport-item/{id}`

**权限**：ADMIN

---

### 6.5 删除项目

**DELETE** `/sport-item/{id}`

**权限**：ADMIN

---

## 7. 报名管理模块 (Registration)

### 7.1 报名列表（分页）

**GET** `/registration/page`

**权限**：ADMIN, TEACHER

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| eventId | long | 否 | 赛事ID |
| sportItemId | long | 否 | 项目ID |
| status | int | 否 | 状态 |
| realName | string | 否 | 报名人姓名 |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "eventId": 1,
        "eventName": "2024年春季运动会",
        "sportItemId": 1,
        "sportItemName": "男子100米",
        "userId": 2,
        "realName": "李明",
        "studentNo": "2021001001",
        "department": "计算机学院",
        "status": 1,
        "statusName": "待审核",
        "createTime": "2024-03-15 10:00:00"
      }
    ],
    "total": 1
  }
}
```

---

### 7.2 我的报名列表

**GET** `/registration/my`

**权限**：STUDENT

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 否 | 赛事ID |
| status | int | 否 | 状态 |

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "eventId": 1,
      "eventName": "2024年春季运动会",
      "sportItemId": 1,
      "sportItemName": "男子100米",
      "status": 2,
      "statusName": "已通过",
      "createTime": "2024-03-15 10:00:00",
      "auditTime": "2024-03-16 09:00:00"
    }
  ]
}
```

---

### 7.3 提交报名

**POST** `/registration`

**权限**：STUDENT

**请求参数**：
```json
{
  "eventId": 1,
  "sportItemId": 1,
  "remark": "备注信息"
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "报名成功，请等待审核",
  "data": {
    "id": 10
  }
}
```

**错误响应**：
```json
{
  "code": 3001,
  "message": "您已报名该项目，请勿重复报名"
}
```

---

### 7.4 取消报名

**PUT** `/registration/{id}/cancel`

**权限**：STUDENT（仅限待审核状态）

---

### 7.5 审核报名

**PUT** `/registration/{id}/audit`

**权限**：ADMIN, TEACHER

**请求参数**：
```json
{
  "status": 2,
  "rejectReason": ""
}
```

| status | 说明 |
|--------|------|
| 2 | 通过 |
| 3 | 拒绝（需填写rejectReason） |

---

### 7.6 批量审核

**PUT** `/registration/batch-audit`

**权限**：ADMIN, TEACHER

**请求参数**：
```json
{
  "ids": [1, 2, 3],
  "status": 2
}
```

---

## 8. 赛程管理模块 (Schedule)

### 8.1 赛程列表

**GET** `/schedule/list`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 是 | 赛事ID |
| sportItemId | long | 否 | 项目ID |
| matchDate | date | 否 | 比赛日期 |

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "eventId": 1,
      "sportItemId": 1,
      "sportItemName": "男子100米",
      "scheduleName": "预赛",
      "roundNo": 1,
      "groupNo": "A组",
      "matchDate": "2024-04-15",
      "startTime": "09:00:00",
      "endTime": "10:00:00",
      "location": "田径场1号跑道",
      "referee": "王裁判",
      "status": 1,
      "statusName": "未开始"
    }
  ]
}
```

---

### 8.2 赛程详情

**GET** `/schedule/{id}`

**权限**：需认证

---

### 8.3 新增赛程

**POST** `/schedule`

**权限**：ADMIN

**请求参数**：
```json
{
  "eventId": 1,
  "sportItemId": 1,
  "scheduleName": "决赛",
  "roundNo": 2,
  "groupNo": null,
  "matchDate": "2024-04-16",
  "startTime": "14:00:00",
  "endTime": "15:00:00",
  "location": "田径场1号跑道",
  "referee": "王裁判"
}
```

---

### 8.4 修改赛程

**PUT** `/schedule/{id}`

**权限**：ADMIN

---

### 8.5 删除赛程

**DELETE** `/schedule/{id}`

**权限**：ADMIN

---

### 8.6 获取赛程参赛人员

**GET** `/schedule/{id}/participants`

**权限**：ADMIN, TEACHER

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "registrationId": 1,
      "userId": 2,
      "realName": "李明",
      "studentNo": "2021001001",
      "department": "计算机学院",
      "hasResult": false
    }
  ]
}
```

---

## 9. 成绩管理模块 (Result)

### 9.1 成绩列表（分页）

**GET** `/result/page`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 否 | 赛事ID |
| sportItemId | long | 否 | 项目ID |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "eventId": 1,
        "eventName": "2024年春季运动会",
        "sportItemId": 1,
        "sportItemName": "男子100米",
        "userId": 2,
        "realName": "李明",
        "studentNo": "2021001001",
        "department": "计算机学院",
        "score": 11.25,
        "scoreText": "11.25秒",
        "ranking": 1,
        "points": 9,
        "recordTime": "2024-04-15 10:30:00"
      }
    ],
    "total": 1
  }
}
```

---

### 9.2 项目成绩排名

**GET** `/result/ranking`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sportItemId | long | 是 | 项目ID |

**响应示例**：
```json
{
  "code": 200,
  "data": [
    {
      "ranking": 1,
      "realName": "李明",
      "studentNo": "2021001001",
      "department": "计算机学院",
      "score": 11.25,
      "scoreText": "11.25秒",
      "points": 9
    },
    {
      "ranking": 2,
      "realName": "张伟",
      "studentNo": "2021002001",
      "department": "电子学院",
      "score": 11.58,
      "scoreText": "11.58秒",
      "points": 7
    }
  ]
}
```

---

### 9.3 录入成绩

**POST** `/result`

**权限**：ADMIN, TEACHER

**请求参数**：
```json
{
  "registrationId": 1,
  "scheduleId": 1,
  "score": 11.25,
  "remark": ""
}
```

---

### 9.4 修改成绩

**PUT** `/result/{id}`

**权限**：ADMIN, TEACHER

---

### 9.5 删除成绩

**DELETE** `/result/{id}`

**权限**：ADMIN

---

### 9.6 Excel导入成绩

**POST** `/result/import`

**权限**：ADMIN, TEACHER

**请求参数**：
- Content-Type: multipart/form-data
- file: Excel文件
- sportItemId: 项目ID

**Excel模板字段**：
| 列 | 字段 | 必填 | 说明 |
|----|------|------|------|
| A | 学号 | 是 | 用于匹配报名记录 |
| B | 姓名 | 是 | 用于校验 |
| C | 成绩 | 是 | 数值 |
| D | 备注 | 否 | - |

**响应示例**：
```json
{
  "code": 200,
  "message": "导入完成",
  "data": {
    "total": 50,
    "success": 48,
    "fail": 2,
    "errors": [
      {
        "row": 10,
        "studentNo": "2021001010",
        "reason": "未找到报名记录"
      },
      {
        "row": 25,
        "studentNo": "2021002005",
        "reason": "成绩格式错误"
      }
    ]
  }
}
```

---

### 9.7 Excel导出成绩

**GET** `/result/export`

**权限**：ADMIN, TEACHER

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sportItemId | long | 是 | 项目ID |

**响应**：Excel文件下载

---

## 10. 公告管理模块 (News)

### 10.1 公告列表（分页）

**GET** `/news/page`

**权限**：需认证

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码 |
| size | int | 否 | 每页数量 |
| newsType | string | 否 | 公告类型 |
| title | string | 否 | 标题（模糊） |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1,
        "title": "2024年春季运动会报名通知",
        "newsType": "NOTICE",
        "newsTypeName": "赛事通知",
        "coverImage": "/upload/news/cover1.jpg",
        "eventId": 1,
        "eventName": "2024年春季运动会",
        "isTop": 1,
        "viewCount": 1520,
        "publishTime": "2024-03-01 09:00:00"
      }
    ],
    "total": 1
  }
}
```

---

### 10.2 公告详情

**GET** `/news/{id}`

**权限**：需认证

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "title": "2024年春季运动会报名通知",
    "newsType": "NOTICE",
    "newsTypeName": "赛事通知",
    "content": "<p>各位同学：</p><p>2024年春季运动会报名开始...</p>",
    "coverImage": "/upload/news/cover1.jpg",
    "eventId": 1,
    "eventName": "2024年春季运动会",
    "isTop": 1,
    "viewCount": 1521,
    "publishTime": "2024-03-01 09:00:00",
    "createTime": "2024-02-28 15:00:00"
  }
}
```

---

### 10.3 新增公告

**POST** `/news`

**权限**：ADMIN

**请求参数**：
```json
{
  "title": "成绩公告",
  "newsType": "RESULT",
  "content": "<p>成绩已公布...</p>",
  "eventId": 1,
  "isTop": 0
}
```

---

### 10.4 修改公告

**PUT** `/news/{id}`

**权限**：ADMIN

---

### 10.5 删除公告

**DELETE** `/news/{id}`

**权限**：ADMIN

---

## 11. 统计模块 (Statistics)

### 11.1 报名统计

**GET** `/statistics/registration`

**权限**：ADMIN, TEACHER

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 否 | 赛事ID |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "totalCount": 500,
    "pendingCount": 50,
    "approvedCount": 420,
    "rejectedCount": 30,
    "byEvent": [
      {
        "eventId": 1,
        "eventName": "2024年春季运动会",
        "count": 300
      }
    ],
    "byDepartment": [
      {
        "department": "计算机学院",
        "count": 80
      },
      {
        "department": "电子学院",
        "count": 65
      }
    ]
  }
}
```

---

### 11.2 成绩统计

**GET** `/statistics/result`

**权限**：ADMIN, TEACHER

**请求参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| eventId | long | 否 | 赛事ID |
| sportItemId | long | 否 | 项目ID |

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "totalCount": 300,
    "avgScore": 12.5,
    "maxScore": 10.2,
    "minScore": 15.8,
    "distribution": [
      {
        "range": "10-11秒",
        "count": 15
      },
      {
        "range": "11-12秒",
        "count": 45
      },
      {
        "range": "12-13秒",
        "count": 80
      },
      {
        "range": "13-14秒",
        "count": 100
      },
      {
        "range": "14秒以上",
        "count": 60
      }
    ],
    "topDepartments": [
      {
        "department": "体育学院",
        "totalPoints": 150,
        "goldCount": 5,
        "silverCount": 3,
        "bronzeCount": 4
      },
      {
        "department": "计算机学院",
        "totalPoints": 120,
        "goldCount": 3,
        "silverCount": 4,
        "bronzeCount": 5
      }
    ]
  }
}
```

---

### 11.3 首页统计概览

**GET** `/statistics/overview`

**权限**：ADMIN, TEACHER

**响应示例**：
```json
{
  "code": 200,
  "data": {
    "eventCount": 5,
    "activeEventCount": 2,
    "totalRegistration": 1500,
    "totalResult": 800,
    "recentEvents": [
      {
        "id": 1,
        "eventName": "2024年春季运动会",
        "status": 2,
        "regCount": 300
      }
    ],
    "registrationTrend": [
      {
        "date": "2024-03-01",
        "count": 50
      },
      {
        "date": "2024-03-02",
        "count": 80
      }
    ]
  }
}
```

---

## 12. 文件上传模块 (File)

### 12.1 上传文件

**POST** `/file/upload`

**权限**：需认证

**请求参数**：
- Content-Type: multipart/form-data
- file: 文件

**响应示例**：
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "url": "/upload/2024/01/abc123.jpg",
    "name": "abc123.jpg",
    "size": 102400
  }
}
```

---

## 13. 权限矩阵

### 13.1 接口权限汇总

| 模块 | 接口 | ADMIN | TEACHER | STUDENT | 公开 |
|------|------|-------|---------|---------|------|
| **认证** | POST /auth/login | - | - | - | ✅ |
| | POST /auth/logout | ✅ | ✅ | ✅ | - |
| | GET /auth/info | ✅ | ✅ | ✅ | - |
| | PUT /auth/password | ✅ | ✅ | ✅ | - |
| **用户** | GET /system/user/page | ✅ | - | - | - |
| | POST /system/user | ✅ | - | - | - |
| | PUT /system/user/{id} | ✅ | - | - | - |
| | DELETE /system/user/{id} | ✅ | - | - | - |
| **字典** | GET /system/dict/* | ✅ | ✅ | ✅ | - |
| **赛事** | GET /event/page | ✅ | ✅ | ✅ | - |
| | GET /event/{id} | ✅ | ✅ | ✅ | - |
| | POST /event | ✅ | - | - | - |
| | PUT /event/{id} | ✅ | - | - | - |
| | DELETE /event/{id} | ✅ | - | - | - |
| **项目** | GET /sport-item/* | ✅ | ✅ | ✅ | - |
| | POST /sport-item | ✅ | - | - | - |
| | PUT /sport-item/{id} | ✅ | - | - | - |
| | DELETE /sport-item/{id} | ✅ | - | - | - |
| **报名** | GET /registration/page | ✅ | ✅ | - | - |
| | GET /registration/my | ✅ | ✅ | ✅ | - |
| | POST /registration | - | - | ✅ | - |
| | PUT /registration/{id}/cancel | - | - | ✅ | - |
| | PUT /registration/{id}/audit | ✅ | ✅ | - | - |
| **赛程** | GET /schedule/* | ✅ | ✅ | ✅ | - |
| | POST /schedule | ✅ | - | - | - |
| | PUT /schedule/{id} | ✅ | - | - | - |
| | DELETE /schedule/{id} | ✅ | - | - | - |
| **成绩** | GET /result/page | ✅ | ✅ | ✅ | - |
| | GET /result/ranking | ✅ | ✅ | ✅ | - |
| | POST /result | ✅ | ✅ | - | - |
| | PUT /result/{id} | ✅ | ✅ | - | - |
| | DELETE /result/{id} | ✅ | - | - | - |
| | POST /result/import | ✅ | ✅ | - | - |
| | GET /result/export | ✅ | ✅ | - | - |
| **公告** | GET /news/page | ✅ | ✅ | ✅ | - |
| | GET /news/{id} | ✅ | ✅ | ✅ | - |
| | POST /news | ✅ | - | - | - |
| | PUT /news/{id} | ✅ | - | - | - |
| | DELETE /news/{id} | ✅ | - | - | - |
| **统计** | GET /statistics/* | ✅ | ✅ | - | - |
| **文件** | POST /file/upload | ✅ | ✅ | ✅ | - |

---

## 14. 附录

### 14.1 通用请求参数

| 参数 | 类型 | 说明 |
|------|------|------|
| page | int | 页码，从1开始 |
| size | int | 每页数量，默认10，最大100 |
| sort | string | 排序字段 |
| order | string | 排序方向：asc/desc |

### 14.2 日期格式

| 类型 | 格式 | 示例 |
|------|------|------|
| 日期 | yyyy-MM-dd | 2024-01-15 |
| 时间 | HH:mm:ss | 09:30:00 |
| 日期时间 | yyyy-MM-dd HH:mm:ss | 2024-01-15 09:30:00 |

### 14.3 修订历史

| 版本 | 日期 | 修订人 | 修订内容 |
|------|------|--------|----------|
| v1.0.0 | 2024-01 | Winston (Architect) | 初始版本 |

---

*文档结束*