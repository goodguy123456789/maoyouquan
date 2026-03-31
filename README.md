# 猫友圈 (MaoYouQuan)

猫协会社区平台，前后端分离架构。

## 技术栈

- **后端**：Spring Boot 3.3.4 + MyBatis Plus + MySQL 8 + JWT
- **前端**：原生 HTML / CSS / JavaScript（静态多页面）
- **服务器**：Nginx（静态文件托管 + API 反向代理）

## 账号说明

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 普通用户 | testuser | pass123 |

## 本地启动步骤

### 前置要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+
- Nginx（Windows 版）

### 1. 克隆项目

```bash
git clone <仓库地址>
cd demo
```

### 2. 创建数据库并建表

```bash
mysql -u root -p<你的密码> -e "CREATE DATABASE IF NOT EXISTS maoyouquan CHARACTER SET utf8mb4;"
mysql -u root -p<你的密码> --default-character-set=utf8mb4 maoyouquan < backend/src/main/resources/sql/init.sql
```

### 3. 修改数据库配置

编辑 `backend/src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/maoyouquan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: "你的MySQL密码"   # ← 改这里
```

端口默认为 **8088**，如需修改：

```yaml
server:
  port: 8088   # ← 改这里
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

看到 `Started MaoyouquanApplication` 表示启动成功。

### 5. 配置并启动 Nginx

编辑 Nginx 的 `conf/nginx.conf`，将以下两处**绝对路径**改为你本机的实际路径：

```nginx
# 改为你的 frontend 目录路径
root   C:/你的实际路径/demo/frontend;

# 改为你的 uploads 目录路径
alias  C:/你的实际路径/demo/backend/uploads/;
```

同时确认 `proxy_pass` 端口与后端一致（默认 8088）：

```nginx
proxy_pass http://127.0.0.1:8088;
```

然后启动 Nginx：

```bash
# 进入 nginx 目录
cd D:/nginx-x.x.x/
./nginx.exe
```

### 6. 访问

| 地址 | 说明 |
|------|------|
| http://localhost/ | 前端首页 |
| http://localhost/admin/ | 管理后台 |
| http://localhost:8088/api/ | 后端 API |

## 项目结构

```
demo/
├── backend/                # Spring Boot 后端
│   ├── src/main/java/      # Java 源码
│   ├── src/main/resources/
│   │   ├── application.yml # 配置文件（需修改DB密码）
│   │   └── sql/init.sql    # 建表脚本
│   └── pom.xml
├── frontend/               # 静态前端
│   ├── index.html          # 首页（猫咪列表）
│   ├── cat-detail.html     # 猫咪详情
│   ├── news.html           # 新闻列表
│   ├── login.html          # 登录
│   ├── register.html       # 注册
│   ├── submit-cat.html     # 提交猫咪
│   ├── admin/              # 管理后台页面
│   ├── css/                # 样式文件
│   └── js/                 # JS 文件
├── nginx.conf              # Nginx 配置模板（需修改路径）
└── README.md
```

## 注意事项

- `nginx.conf` 中的路径是模板示例，克隆后**必须修改**为本机实际路径
- 密码加密方式为 **MD5**（demo 项目）
- 图片来自 [cataas.com](https://cataas.com)，需要联网才能显示
