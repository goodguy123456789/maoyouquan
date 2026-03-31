# 猫友圈 — 系统设计规格文档

**日期：** 2026-03-30
**项目：** 猫友圈（Cat Community Platform）
**架构：** 前后端分离，前端 HTML/CSS/Nginx，后端 Spring Boot 4 + MyBatis Plus + MySQL

---

## 1. 项目概述

猫友圈是一个猫协会社区平台，支持猫咪信息展示、猫咪新闻浏览、用户互动（点赞、评论）。前端为静态多页面，后端提供 REST API，两者通过 JWT 鉴权通信。

---

## 2. 用户角色与权限

| 角色 | 权限 |
|------|------|
| 游客 | 浏览猫咪列表/详情、浏览新闻 |
| 登录用户 | 游客权限 + 提交猫咪信息、点赞/取消点赞、发表评论 |
| 管理员 | 登录用户权限 + 审核猫咪、管理新闻、屏蔽评论 |

- 评论提交后立即显示，管理员可屏蔽（`is_blocked=true`）
- 猫咪信息提交后状态为 PENDING，管理员审核通过后前端可见

---

## 3. 技术栈

| 层次 | 技术 |
|------|------|
| 前端服务 | Nginx（静态文件 + 反向代理 /api/* 到 8080） |
| 前端语言 | HTML5 + CSS3 + Vanilla JavaScript |
| 后端框架 | Spring Boot 4.0.5 |
| ORM | MyBatis Plus |
| 数据库 | MySQL 8（端口 3306，密码 1234） |
| 鉴权 | JWT（Spring Security） |
| 图片存储 | 后端本地 /uploads/ 目录，Nginx 同时 serve |

---

## 4. 数据库设计

### 4.1 users（用户表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| username | VARCHAR(50) UNIQUE NOT NULL | 登录名 |
| password_hash | VARCHAR(255) NOT NULL | BCrypt 加密 |
| email | VARCHAR(100) UNIQUE | |
| nickname | VARCHAR(50) | 显示名 |
| avatar_url | VARCHAR(255) | 头像访问路径 |
| role | ENUM('USER','ADMIN') DEFAULT 'USER' | |
| is_active | TINYINT(1) DEFAULT 1 | |
| created_at | DATETIME DEFAULT NOW() | |

### 4.2 cats（猫咪信息表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| name | VARCHAR(50) NOT NULL | 姓名 |
| gender | ENUM('MALE','FEMALE') | 性别 |
| age | INT | 年龄（岁） |
| breed | VARCHAR(50) | 品种 |
| avatar_url | VARCHAR(255) | 头像访问路径 |
| avatar_path | VARCHAR(255) | 磁盘存储路径（上传文件时用） |
| personality | VARCHAR(255) | 性格描述 |
| like_count | INT DEFAULT 0 | 点赞数（冗余字段，便于排序） |
| status | ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING' | 审核状态 |
| submitter_id | BIGINT FK -> users.id | 提交人 |
| created_at | DATETIME DEFAULT NOW() | 添加时间 |
| updated_at | DATETIME DEFAULT NOW() ON UPDATE NOW() | |

### 4.3 cat_likes（点赞记录表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| cat_id | BIGINT FK -> cats.id | |
| user_id | BIGINT FK -> users.id | |
| created_at | DATETIME DEFAULT NOW() | |

**联合唯一索引：** UNIQUE(cat_id, user_id)

### 4.4 comments（评论表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| cat_id | BIGINT FK -> cats.id | |
| user_id | BIGINT FK -> users.id | |
| content | TEXT NOT NULL | |
| is_blocked | TINYINT(1) DEFAULT 0 | 管理员屏蔽 |
| created_at | DATETIME DEFAULT NOW() | |

### 4.5 news（新闻表）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO_INCREMENT | |
| title | VARCHAR(200) NOT NULL | |
| content | TEXT NOT NULL | |
| category | ENUM('KNOWLEDGE','ANNOUNCEMENT','MIGRATION') | 科普/公告/迁入迁出 |
| cover_url | VARCHAR(255) | 封面图 |
| author_id | BIGINT FK -> users.id | 发布管理员 |
| created_at | DATETIME DEFAULT NOW() | |
| updated_at | DATETIME DEFAULT NOW() ON UPDATE NOW() | |

---

## 5. 前端页面结构（方案 B：轻量模块化多页面）

```
frontend/
├── index.html              首页 — 猫咪列表（卡片网格，姓名+头像）
├── cat-detail.html         猫咪详情（姓名/性别/年龄/品种/性格/点赞/评论区）
├── news.html               新闻列表（分类标签页 + 列表）
├── news-detail.html        新闻详情
├── login.html              登录页
├── register.html           注册页
├── admin/
│   ├── index.html          管理后台首页（数据统计）
│   ├── cats.html           猫咪审核管理
│   ├── news.html           新闻管理（增删改）
│   └── comments.html       评论屏蔽管理
├── js/
│   ├── api.js              统一封装所有 fetch 请求
│   ├── common.js           公共导航栏渲染、工具函数
│   └── auth.js             JWT 存储、登录状态检查
└── css/
    ├── style.css           全局样式（用户端橙色暖调）
    └── admin.css           管理后台样式（深色专业风）
```

**视觉风格：**
- 用户端：橙色暖调（主色 #f97316），圆角卡片，猫咪表情符号点缀
- 管理端：深色侧边栏（#0f172a），蓝色高亮（#38bdf8），数据表格

---

## 6. 后端 API 设计

### 认证
```
POST /api/auth/register      注册
POST /api/auth/login         登录，返回 JWT
GET  /api/auth/me            获取当前用户信息（需JWT）
```

### 猫咪
```
GET  /api/cats               分页列表（仅返回 status=APPROVED 的猫咪）
                             ?page=1&size=12
                             &name=小花          姓名模糊搜索
                             &breed=波斯猫        品种筛选（模糊匹配）
                             &sort=likes|latest  热度排序 or 最新（默认latest）
GET  /api/cats/{id}          猫咪详情
POST /api/cats               提交猫咪信息（需登录，初始PENDING）
POST /api/upload/image       上传图片，返回访问URL
POST /api/cats/{id}/like     点赞/取消点赞（需登录，幂等）
                             响应：{ liked: true|false, likeCount: 128 }
```

### 评论
```
GET  /api/cats/{id}/comments     获取评论列表（过滤 is_blocked）
POST /api/cats/{id}/comments     发表评论（需登录）
```

### 新闻
```
GET  /api/news               分页列表
                             ?page=1&size=10
                             &category=KNOWLEDGE|ANNOUNCEMENT|MIGRATION
GET  /api/news/{id}          新闻详情
```

### 管理员（需 ADMIN 角色 JWT）
```
GET    /api/admin/cats/pending        待审核列表
PUT    /api/admin/cats/{id}/status    审核通过/拒绝 {status: APPROVED|REJECTED}
PUT    /api/admin/comments/{id}/block  屏蔽评论（软屏蔽，is_blocked=true）
POST   /api/admin/news                发布新闻
PUT    /api/admin/news/{id}           编辑新闻
DELETE /api/admin/news/{id}           删除新闻
```

---

## 7. 关键实现说明

### 猫咪分页+搜索+排序
MyBatis Plus `Page` 对象 + `QueryWrapper`：
- `like("name", keyword)` 实现姓名模糊搜索
- `like("breed", breed)` 实现品种模糊搜索（可与姓名同时使用）
- `orderByDesc("like_count")` 热度排序
- `orderByDesc("created_at")` 最新排序

### 点赞幂等处理
- 点赞：INSERT INTO cat_likes，成功则 cats.like_count +1
- 取消：DELETE FROM cat_likes WHERE cat_id=? AND user_id=?，成功则 like_count -1
- 前端请求同一接口，后端判断记录是否存在决定操作方向

### 图片上传
- 支持文件上传（MultipartFile → /uploads/{uuid}.jpg）
- 支持 URL 直接填写
- 返回统一的访问 URL（Nginx serve /uploads/ 目录）

### Nginx 配置要点
```nginx
location /api/ {
    proxy_pass http://localhost:8080;
}
location /uploads/ {
    alias /path/to/uploads/;
}
location / {
    root /path/to/frontend;
    try_files $uri $uri/ /index.html;
}
```

---

## 8. 验证方案

1. **本地启动**：MySQL 起 → Spring Boot 启动（自动建表） → Nginx serve 前端
2. **注册/登录**：注册账号，登录拿到 JWT
3. **提交猫咪**：用登录 JWT 提交一只猫，状态应为 PENDING
4. **管理员审核**：用 ADMIN 账号审核，状态改为 APPROVED，前端首页可见
5. **点赞测试**：点赞 → like_count+1，再点 → like_count-1
6. **评论屏蔽**：发表评论 → 普通用户可见 → 管理员屏蔽 → 不再显示
7. **搜索排序**：按名称模糊搜索，切换热度/最新排序验证结果
8. **新闻分类**：管理员发布三类新闻，前端分类筛选验证
