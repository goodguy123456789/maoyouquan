# 猫友圈 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建猫友圈前后端分离社区平台，含用户认证、猫咪展示/点赞/评论、新闻分类展示及管理后台。

**Architecture:** 前端为轻量模块化多页面静态 HTML，由 Nginx 托管；后端为 Spring Boot 4 REST API（端口 8080），JWT 鉴权；MySQL 存储数据，图片文件存于后端 /uploads/ 目录。

**Tech Stack:** Spring Boot 4.0.5 · MyBatis Plus · MySQL 8 · Spring Security + JWT · HTML5/CSS3/Vanilla JS · Nginx

---

## 文件结构总览

```
demo/
├── backend/                              Spring Boot 项目
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/maoyouquan/
│       │   ├── MaoyouquanApplication.java
│       │   ├── config/
│       │   │   ├── SecurityConfig.java   Spring Security + JWT 过滤器注册
│       │   │   └── WebMvcConfig.java     CORS、静态资源映射
│       │   ├── entity/
│       │   │   ├── User.java
│       │   │   ├── Cat.java
│       │   │   ├── CatLike.java
│       │   │   ├── Comment.java
│       │   │   └── News.java
│       │   ├── mapper/                   MyBatis Plus Mapper 接口
│       │   │   ├── UserMapper.java
│       │   │   ├── CatMapper.java
│       │   │   ├── CatLikeMapper.java
│       │   │   ├── CommentMapper.java
│       │   │   └── NewsMapper.java
│       │   ├── dto/                      请求/响应 DTO
│       │   │   ├── R.java                统一响应格式
│       │   │   ├── LoginRequest.java
│       │   │   ├── RegisterRequest.java
│       │   │   ├── CatSubmitRequest.java
│       │   │   ├── CommentRequest.java
│       │   │   └── NewsRequest.java
│       │   ├── security/
│       │   │   ├── JwtTokenProvider.java JWT 生成/解析
│       │   │   ├── JwtAuthFilter.java    每次请求提取 JWT
│       │   │   └── UserDetailsServiceImpl.java
│       │   ├── service/
│       │   │   ├── AuthService.java
│       │   │   ├── CatService.java
│       │   │   ├── CommentService.java
│       │   │   ├── NewsService.java
│       │   │   └── FileService.java
│       │   └── controller/
│       │       ├── AuthController.java
│       │       ├── CatController.java
│       │       ├── CommentController.java
│       │       ├── NewsController.java
│       │       ├── FileController.java
│       │       └── AdminController.java
│       └── resources/
│           ├── application.yml
│           └── sql/init.sql              建表 SQL（启动时手动执行）
└── frontend/
    ├── index.html                        首页（猫咪列表）
    ├── cat-detail.html                   猫咪详情
    ├── news.html                         新闻列表
    ├── news-detail.html                  新闻详情
    ├── login.html
    ├── register.html
    ├── submit-cat.html                   提交猫咪
    ├── admin/
    │   ├── index.html                    管理后台首页
    │   ├── cats.html                     猫咪审核
    │   ├── news.html                     新闻管理
    │   └── comments.html                 评论管理
    ├── js/
    │   ├── api.js                        fetch 封装 + JWT 自动附加
    │   ├── auth.js                       登录状态管理
    │   └── common.js                     导航栏渲染、分页组件、工具函数
    └── css/
        ├── style.css                     用户端橙色暖调
        └── admin.css                     管理端深色专业风
```

---

## Task 1: 数据库建表

**Files:**
- Create: `backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: 编写建表 SQL**

```sql
CREATE DATABASE IF NOT EXISTS maoyouquan DEFAULT CHARACTER SET utf8mb4;
USE maoyouquan;

CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    nickname VARCHAR(50),
    avatar_url VARCHAR(255),
    role ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT NOW()
);

CREATE TABLE cats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    gender ENUM('MALE','FEMALE'),
    age INT,
    breed VARCHAR(50),
    avatar_url VARCHAR(255),
    avatar_path VARCHAR(255),
    personality VARCHAR(255),
    like_count INT NOT NULL DEFAULT 0,
    status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    submitter_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    FOREIGN KEY (submitter_id) REFERENCES users(id)
);

CREATE TABLE cat_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uk_cat_user (cat_id, user_id),
    FOREIGN KEY (cat_id) REFERENCES cats(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_blocked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cat_id) REFERENCES cats(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE news (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    category ENUM('KNOWLEDGE','ANNOUNCEMENT','MIGRATION') NOT NULL,
    cover_url VARCHAR(255),
    author_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- 初始管理员账号（密码 admin123，BCrypt）
INSERT INTO users (username, password_hash, nickname, role)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBpwTTyU9WxQ2W', '管理员', 'ADMIN');
```

- [ ] **Step 2: 执行建表**

```bash
mysql -u root -p1234 < backend/src/main/resources/sql/init.sql
```

预期输出：无报错，Query OK。

- [ ] **Step 3: 验证表结构**

```bash
mysql -u root -p1234 -e "USE maoyouquan; SHOW TABLES;"
```

预期：列出 5 张表 + users 中有 admin 账号。

---

## Task 2: Spring Boot 项目初始化

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/maoyouquan/MaoyouquanApplication.java`

- [ ] **Step 1: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.5</version>
    </parent>
    <groupId>com.maoyouquan</groupId>
    <artifactId>maoyouquan</artifactId>
    <version>1.0.0</version>
    <properties>
        <java.version>21</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>3.5.7</version>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: 创建 application.yml**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/maoyouquan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: "1234"
    driver-class-name: com.mysql.cj.jdbc.Driver
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: maoyouquan-secret-key-must-be-at-least-256-bits-long-for-hs256
  expiration: 86400000  # 24小时(毫秒)

upload:
  dir: ./uploads/

server:
  port: 8080
```

- [ ] **Step 3: 创建启动类**

```java
// backend/src/main/java/com/maoyouquan/MaoyouquanApplication.java
package com.maoyouquan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MaoyouquanApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaoyouquanApplication.class, args);
    }
}
```

- [ ] **Step 4: 验证项目能编译**

```bash
cd backend && mvn compile -q
```

预期：BUILD SUCCESS，无编译报错。

- [ ] **Step 5: 提交**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml \
  backend/src/main/java/com/maoyouquan/MaoyouquanApplication.java \
  backend/src/main/resources/sql/init.sql
git commit -m "chore: 初始化 Spring Boot 项目结构与数据库建表"
```

---

## Task 3: 实体类 + 统一响应格式

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/dto/R.java`
- Create: `backend/src/main/java/com/maoyouquan/entity/User.java`
- Create: `backend/src/main/java/com/maoyouquan/entity/Cat.java`
- Create: `backend/src/main/java/com/maoyouquan/entity/CatLike.java`
- Create: `backend/src/main/java/com/maoyouquan/entity/Comment.java`
- Create: `backend/src/main/java/com/maoyouquan/entity/News.java`

- [ ] **Step 1: 创建统一响应格式 R.java**

```java
package com.maoyouquan.dto;

import lombok.Data;

@Data
public class R<T> {
    private boolean success;
    private T data;
    private String message;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.success = true;
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.success = false;
        r.message = message;
        return r;
    }
}
```

- [ ] **Step 2: 创建 User 实体**

```java
package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String passwordHash;
    private String email;
    private String nickname;
    private String avatarUrl;
    private String role;  // USER | ADMIN
    private Integer isActive;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 Cat 实体**

```java
package com.maoyouquan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cats")
public class Cat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String gender;   // MALE | FEMALE
    private Integer age;
    private String breed;
    private String avatarUrl;
    private String avatarPath;
    private String personality;
    private Integer likeCount;
    private String status;   // PENDING | APPROVED | REJECTED
    private Long submitterId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 CatLike、Comment、News 实体**

```java
// CatLike.java
package com.maoyouquan.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("cat_likes")
public class CatLike {
    @TableId(type = IdType.AUTO) private Long id;
    private Long catId;
    private Long userId;
    private LocalDateTime createdAt;
}
```

```java
// Comment.java
package com.maoyouquan.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("comments")
public class Comment {
    @TableId(type = IdType.AUTO) private Long id;
    private Long catId;
    private Long userId;
    private String content;
    private Integer isBlocked;
    private LocalDateTime createdAt;
}
```

```java
// News.java
package com.maoyouquan.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("news")
public class News {
    @TableId(type = IdType.AUTO) private Long id;
    private String title;
    private String content;
    private String category;  // KNOWLEDGE | ANNOUNCEMENT | MIGRATION
    private String coverUrl;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 5: 编译验证**

```bash
cd backend && mvn compile -q
```

预期：BUILD SUCCESS。

- [ ] **Step 6: 提交**

```bash
git add backend/src/main/java/com/maoyouquan/
git commit -m "feat: 添加实体类和统一响应格式 R"
```

---

## Task 4: Mapper 接口

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/mapper/UserMapper.java`
- Create: `backend/src/main/java/com/maoyouquan/mapper/CatMapper.java`
- Create: `backend/src/main/java/com/maoyouquan/mapper/CatLikeMapper.java`
- Create: `backend/src/main/java/com/maoyouquan/mapper/CommentMapper.java`
- Create: `backend/src/main/java/com/maoyouquan/mapper/NewsMapper.java`

- [ ] **Step 1: 创建所有 Mapper 接口**

```java
// UserMapper.java
package com.maoyouquan.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.maoyouquan.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}
```

对 CatMapper、CatLikeMapper、CommentMapper、NewsMapper 重复相同模式，只改 `User` 为对应实体类名。

- [ ] **Step 2: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/maoyouquan/mapper/
git commit -m "feat: 添加 MyBatis Plus Mapper 接口"
```

---

## Task 5: JWT 安全模块

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/security/JwtTokenProvider.java`
- Create: `backend/src/main/java/com/maoyouquan/security/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/maoyouquan/security/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/maoyouquan/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/maoyouquan/config/WebMvcConfig.java`
- Test: `backend/src/test/java/com/maoyouquan/security/JwtTokenProviderTest.java`

- [ ] **Step 1: 写 JwtTokenProvider 测试（先写测试）**

```java
package com.maoyouquan.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JwtTokenProviderTest {
    private final JwtTokenProvider provider =
        new JwtTokenProvider("maoyouquan-secret-key-must-be-at-least-256-bits-long-for-hs256", 86400000L);

    @Test
    void generateAndValidateToken() {
        String token = provider.generateToken("testuser", "USER");
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("testuser");
    }

    @Test
    void invalidTokenReturnsFalse() {
        assertThat(provider.validateToken("invalid.token.here")).isFalse();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd backend && mvn test -pl . -Dtest=JwtTokenProviderTest -q 2>&1 | tail -5
```

预期：FAIL（类不存在）。

- [ ] **Step 3: 实现 JwtTokenProvider**

```java
package com.maoyouquan.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generateToken(String username, String role) {
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload().getSubject();
    }

    public String getRole(String token) {
        return (String) Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload().get("role");
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd backend && mvn test -Dtest=JwtTokenProviderTest -q 2>&1 | tail -5
```

预期：Tests run: 2, Failures: 0。

- [ ] **Step 5: 实现 UserDetailsServiceImpl**

```java
package com.maoyouquan.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) throw new UsernameNotFoundException("用户不存在: " + username);
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPasswordHash(),
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
}
```

- [ ] **Step 6: 实现 JwtAuthFilter**

```java
package com.maoyouquan.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (tokenProvider.validateToken(token)) {
                String username = tokenProvider.getUsername(token);
                UserDetails ud = userDetailsService.loadUserByUsername(username);
                var auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

- [ ] **Step 7: 实现 SecurityConfig**

```java
package com.maoyouquan.config;

import com.maoyouquan.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cats/**", "/api/news/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/uploads/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 8: 实现 WebMvcConfig（CORS）**

```java
package com.maoyouquan.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:" + uploadDir);
    }
}
```

- [ ] **Step 9: 编译验证**

```bash
cd backend && mvn compile -q
```

- [ ] **Step 10: 提交**

```bash
git add backend/src/main/java/com/maoyouquan/security/ \
  backend/src/main/java/com/maoyouquan/config/ \
  backend/src/test/java/com/maoyouquan/security/
git commit -m "feat: JWT 安全模块 + Spring Security 配置"
```

---

## Task 6: 用户认证 API

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/dto/LoginRequest.java`
- Create: `backend/src/main/java/com/maoyouquan/dto/RegisterRequest.java`
- Create: `backend/src/main/java/com/maoyouquan/service/AuthService.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/AuthController.java`
- Test: `backend/src/test/java/com/maoyouquan/service/AuthServiceTest.java`

- [ ] **Step 1: 创建 DTO**

```java
// LoginRequest.java
package com.maoyouquan.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data public class LoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}
```

```java
// RegisterRequest.java
package com.maoyouquan.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data public class RegisterRequest {
    @NotBlank @Size(min=3, max=50) private String username;
    @NotBlank @Size(min=6) private String password;
    @Email private String email;
    private String nickname;
}
```

- [ ] **Step 2: 编写 AuthService 测试**

```java
package com.maoyouquan.service;

import com.maoyouquan.dto.RegisterRequest;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserMapper userMapper;
    @Mock JwtTokenProvider tokenProvider;
    @InjectMocks AuthService authService;

    @Test
    void register_newUser_savesWithEncodedPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any())).thenReturn(1);

        authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(new BCryptPasswordEncoder().matches("password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_duplicateUsername_throwsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("existing");
        req.setPassword("pass123");
        when(userMapper.selectOne(any())).thenReturn(new User());

        assertThatThrownBy(() -> authService.register(req))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("用户名已存在");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd backend && mvn test -Dtest=AuthServiceTest -q 2>&1 | tail -5
```

- [ ] **Step 4: 实现 AuthService**

```java
package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.dto.*;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public void register(RegisterRequest req) {
        if (userMapper.selectOne(new QueryWrapper<User>().eq("username", req.getUsername())) != null) {
            throw new RuntimeException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setRole("USER");
        user.setIsActive(1);
        userMapper.insert(user);
    }

    public Map<String, String> login(LoginRequest req) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", req.getUsername()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole());
        return Map.of("token", token, "role", user.getRole(), "nickname", user.getNickname());
    }

    public User getCurrentUser(String username) {
        return userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd backend && mvn test -Dtest=AuthServiceTest -q 2>&1 | tail -5
```

预期：Tests run: 2, Failures: 0。

- [ ] **Step 6: 实现 AuthController**

```java
package com.maoyouquan.controller;

import com.maoyouquan.dto.*;
import com.maoyouquan.entity.User;
import com.maoyouquan.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public R<?> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return R.ok("注册成功");
    }

    @PostMapping("/login")
    public R<?> login(@Valid @RequestBody LoginRequest req) {
        return R.ok(authService.login(req));
    }

    @GetMapping("/me")
    public R<User> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = authService.getCurrentUser(userDetails.getUsername());
        user.setPasswordHash(null); // 不返回密码
        return R.ok(user);
    }
}
```

- [ ] **Step 7: 启动项目手动测试注册/登录**

```bash
cd backend && mvn spring-boot:run &
# 等待启动后
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123","nickname":"测试用户"}' | python -m json.tool
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"pass123"}' | python -m json.tool
```

预期：注册返回 `{"success":true}`，登录返回含 `token` 的 JSON。

- [ ] **Step 8: 提交**

```bash
git add backend/src/main/java/com/maoyouquan/ backend/src/test/
git commit -m "feat: 用户注册/登录 API + JWT 鉴权"
```

---

## Task 7: 猫咪 CRUD + 分页搜索

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/dto/CatSubmitRequest.java`
- Create: `backend/src/main/java/com/maoyouquan/service/CatService.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/CatController.java`
- Test: `backend/src/test/java/com/maoyouquan/service/CatServiceTest.java`

- [ ] **Step 1: 创建 CatSubmitRequest DTO**

```java
package com.maoyouquan.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data public class CatSubmitRequest {
    @NotBlank private String name;
    private String gender;
    private Integer age;
    private String breed;
    private String avatarUrl;
    private String personality;
}
```

- [ ] **Step 2: 编写 CatService 测试**

```java
package com.maoyouquan.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.CatSubmitRequest;
import com.maoyouquan.entity.Cat;
import com.maoyouquan.mapper.CatMapper;
import com.maoyouquan.mapper.CatLikeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatServiceTest {
    @Mock CatMapper catMapper;
    @Mock CatLikeMapper catLikeMapper;
    @InjectMocks CatService catService;

    @Test
    void submitCat_setsPendingStatusAndSubmitterId() {
        CatSubmitRequest req = new CatSubmitRequest();
        req.setName("小花");
        req.setBreed("波斯猫");
        when(catMapper.insert(any())).thenReturn(1);

        catService.submitCat(req, 42L);

        ArgumentCaptor<Cat> captor = ArgumentCaptor.forClass(Cat.class);
        verify(catMapper).insert(captor.capture());
        Cat saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("PENDING");
        assertThat(saved.getSubmitterId()).isEqualTo(42L);
        assertThat(saved.getName()).isEqualTo("小花");
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
cd backend && mvn test -Dtest=CatServiceTest -q 2>&1 | tail -5
```

- [ ] **Step 4: 实现 CatService**

```java
package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.CatSubmitRequest;
import com.maoyouquan.entity.*;
import com.maoyouquan.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CatService {
    private final CatMapper catMapper;
    private final CatLikeMapper catLikeMapper;

    public Page<Cat> listApproved(int page, int size, String name, String breed, String sort) {
        QueryWrapper<Cat> qw = new QueryWrapper<Cat>().eq("status", "APPROVED");
        if (StringUtils.hasText(name)) qw.like("name", name);
        if (StringUtils.hasText(breed)) qw.like("breed", breed);
        if ("likes".equals(sort)) qw.orderByDesc("like_count");
        else qw.orderByDesc("created_at");
        return catMapper.selectPage(new Page<>(page, size), qw);
    }

    public Cat getById(Long id) {
        Cat cat = catMapper.selectById(id);
        if (cat == null || !"APPROVED".equals(cat.getStatus())) throw new RuntimeException("猫咪不存在");
        return cat;
    }

    public void submitCat(CatSubmitRequest req, Long submitterId) {
        Cat cat = new Cat();
        cat.setName(req.getName());
        cat.setGender(req.getGender());
        cat.setAge(req.getAge());
        cat.setBreed(req.getBreed());
        cat.setAvatarUrl(req.getAvatarUrl());
        cat.setPersonality(req.getPersonality());
        cat.setLikeCount(0);
        cat.setStatus("PENDING");
        cat.setSubmitterId(submitterId);
        catMapper.insert(cat);
    }

    @Transactional
    public Map<String, Object> toggleLike(Long catId, Long userId) {
        QueryWrapper<CatLike> qw = new QueryWrapper<CatLike>()
            .eq("cat_id", catId).eq("user_id", userId);
        CatLike existing = catLikeMapper.selectOne(qw);
        boolean liked;
        if (existing == null) {
            CatLike like = new CatLike();
            like.setCatId(catId); like.setUserId(userId);
            catLikeMapper.insert(like);
            catMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Cat>()
                .eq("id", catId).setSql("like_count = like_count + 1"));
            liked = true;
        } else {
            catLikeMapper.delete(qw);
            catMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Cat>()
                .eq("id", catId).setSql("like_count = like_count - 1"));
            liked = false;
        }
        Cat cat = catMapper.selectById(catId);
        return Map.of("liked", liked, "likeCount", cat.getLikeCount());
    }

    public boolean hasLiked(Long catId, Long userId) {
        return catLikeMapper.selectOne(new QueryWrapper<CatLike>()
            .eq("cat_id", catId).eq("user_id", userId)) != null;
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd backend && mvn test -Dtest=CatServiceTest -q 2>&1 | tail -5
```

- [ ] **Step 6: 实现 CatController**

```java
package com.maoyouquan.controller;

import com.maoyouquan.dto.*;
import com.maoyouquan.entity.Cat;
import com.maoyouquan.service.CatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.maoyouquan.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.entity.User;

@RestController
@RequestMapping("/api/cats")
@RequiredArgsConstructor
public class CatController {
    private final CatService catService;
    private final UserMapper userMapper;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue="1") int page,
                     @RequestParam(defaultValue="12") int size,
                     @RequestParam(required=false) String name,
                     @RequestParam(required=false) String breed,
                     @RequestParam(defaultValue="latest") String sort) {
        return R.ok(catService.listApproved(page, size, name, breed, sort));
    }

    @GetMapping("/{id}")
    public R<Cat> detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails) {
        Cat cat = catService.getById(id);
        return R.ok(cat);
    }

    @PostMapping
    public R<?> submit(@Valid @RequestBody CatSubmitRequest req,
                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        catService.submitCat(req, user.getId());
        return R.ok("提交成功，等待审核");
    }

    @PostMapping("/{id}/like")
    public R<?> like(@PathVariable Long id,
                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        return R.ok(catService.toggleLike(id, user.getId()));
    }
}
```

- [ ] **Step 7: 启动验证（需要 Task 6 的 token）**

```bash
TOKEN="登录后获取的token"
curl -s "http://localhost:8080/api/cats?page=1&size=12&sort=likes" | python -m json.tool
```

- [ ] **Step 8: 添加 MyBatis Plus 分页插件配置**

```java
// 在 config/ 新建 MybatisPlusConfig.java
package com.maoyouquan.config;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
```

- [ ] **Step 9: 提交**

```bash
git add backend/src/
git commit -m "feat: 猫咪列表/详情/提交/点赞 API，支持分页+模糊搜索+排序"
```

---

## Task 8: 评论 + 新闻 + 文件上传 + 管理员 API

**Files:**
- Create: `backend/src/main/java/com/maoyouquan/service/CommentService.java`
- Create: `backend/src/main/java/com/maoyouquan/service/NewsService.java`
- Create: `backend/src/main/java/com/maoyouquan/service/FileService.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/CommentController.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/NewsController.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/FileController.java`
- Create: `backend/src/main/java/com/maoyouquan/controller/AdminController.java`
- Create: `backend/src/main/java/com/maoyouquan/dto/CommentRequest.java`
- Create: `backend/src/main/java/com/maoyouquan/dto/NewsRequest.java`
- Test: `backend/src/test/java/com/maoyouquan/service/CommentServiceTest.java`

- [ ] **Step 1: 编写 CommentService 测试**

```java
package com.maoyouquan.service;

import com.maoyouquan.entity.Comment;
import com.maoyouquan.mapper.CommentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {
    @Mock CommentMapper commentMapper;
    @InjectMocks CommentService commentService;

    @Test
    void addComment_setsBlockedFalseByDefault() {
        when(commentMapper.insert(any())).thenReturn(1);
        commentService.addComment(1L, 2L, "好可爱！");
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getIsBlocked()).isEqualTo(0);
        assertThat(captor.getValue().getContent()).isEqualTo("好可爱！");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd backend && mvn test -Dtest=CommentServiceTest -q 2>&1 | tail -5
```

- [ ] **Step 3: 实现 CommentService**

```java
package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.maoyouquan.entity.Comment;
import com.maoyouquan.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentMapper commentMapper;

    public List<Comment> listByCat(Long catId) {
        return commentMapper.selectList(new QueryWrapper<Comment>()
            .eq("cat_id", catId).eq("is_blocked", 0).orderByAsc("created_at"));
    }

    public void addComment(Long catId, Long userId, String content) {
        Comment comment = new Comment();
        comment.setCatId(catId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setIsBlocked(0);
        commentMapper.insert(comment);
    }

    public void blockComment(Long commentId) {
        Comment c = new Comment();
        c.setId(commentId);
        c.setIsBlocked(1);
        commentMapper.updateById(c);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd backend && mvn test -Dtest=CommentServiceTest -q 2>&1 | tail -5
```

- [ ] **Step 5: 实现 NewsService**

```java
package com.maoyouquan.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.NewsRequest;
import com.maoyouquan.entity.News;
import com.maoyouquan.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsMapper newsMapper;

    public Page<News> list(int page, int size, String category) {
        QueryWrapper<News> qw = new QueryWrapper<News>().orderByDesc("created_at");
        if (StringUtils.hasText(category)) qw.eq("category", category);
        return newsMapper.selectPage(new Page<>(page, size), qw);
    }

    public News getById(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) throw new RuntimeException("新闻不存在");
        return news;
    }

    public void create(NewsRequest req, Long authorId) {
        News news = new News();
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setCoverUrl(req.getCoverUrl());
        news.setAuthorId(authorId);
        newsMapper.insert(news);
    }

    public void update(Long id, NewsRequest req) {
        News news = new News();
        news.setId(id);
        news.setTitle(req.getTitle());
        news.setContent(req.getContent());
        news.setCategory(req.getCategory());
        news.setCoverUrl(req.getCoverUrl());
        newsMapper.updateById(news);
    }

    public void delete(Long id) {
        newsMapper.deleteById(id);
    }
}
```

- [ ] **Step 6: 实现 FileService**

```java
package com.maoyouquan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileService {
    @Value("${upload.dir}")
    private String uploadDir;

    public String save(MultipartFile file) throws IOException {
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + ext;
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        Files.copy(file.getInputStream(), dir.resolve(filename));
        return "/uploads/" + filename;
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }
}
```

- [ ] **Step 7: 实现剩余 Controller 和 AdminController**

```java
// CommentController.java
package com.maoyouquan.controller;
import com.maoyouquan.dto.*;
import com.maoyouquan.entity.User;
import com.maoyouquan.mapper.UserMapper;
import com.maoyouquan.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@RestController
@RequestMapping("/api/cats/{catId}/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserMapper userMapper;

    @GetMapping
    public R<?> list(@PathVariable Long catId) {
        return R.ok(commentService.listByCat(catId));
    }

    @PostMapping
    public R<?> add(@PathVariable Long catId,
                    @RequestBody CommentRequest req,
                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        commentService.addComment(catId, user.getId(), req.getContent());
        return R.ok("评论成功");
    }
}
```

```java
// NewsController.java
package com.maoyouquan.controller;
import com.maoyouquan.dto.R;
import com.maoyouquan.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public R<?> list(@RequestParam(defaultValue="1") int page,
                     @RequestParam(defaultValue="10") int size,
                     @RequestParam(required=false) String category) {
        return R.ok(newsService.list(page, size, category));
    }

    @GetMapping("/{id}")
    public R<?> detail(@PathVariable Long id) {
        return R.ok(newsService.getById(id));
    }
}
```

```java
// FileController.java
package com.maoyouquan.controller;
import com.maoyouquan.dto.R;
import com.maoyouquan.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping("/image")
    public R<?> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String url = fileService.save(file);
        return R.ok(java.util.Map.of("url", url));
    }
}
```

```java
// AdminController.java
package com.maoyouquan.controller;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.maoyouquan.dto.*;
import com.maoyouquan.entity.*;
import com.maoyouquan.mapper.*;
import com.maoyouquan.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CatMapper catMapper;
    private final CommentService commentService;
    private final NewsService newsService;
    private final UserMapper userMapper;

    // 待审核猫咪列表
    @GetMapping("/cats/pending")
    public R<?> pendingCats(@RequestParam(defaultValue="1") int page,
                             @RequestParam(defaultValue="10") int size) {
        return R.ok(catMapper.selectPage(new Page<>(page, size),
            new QueryWrapper<Cat>().eq("status", "PENDING").orderByAsc("created_at")));
    }

    // 审核通过/拒绝
    @PutMapping("/cats/{id}/status")
    public R<?> updateCatStatus(@PathVariable Long id,
                                 @RequestBody Map<String, String> body) {
        Cat cat = new Cat();
        cat.setId(id);
        cat.setStatus(body.get("status"));
        catMapper.updateById(cat);
        return R.ok();
    }

    // 屏蔽评论
    @PutMapping("/comments/{id}/block")
    public R<?> blockComment(@PathVariable Long id) {
        commentService.blockComment(id);
        return R.ok();
    }

    // 发布新闻
    @PostMapping("/news")
    public R<?> createNews(@RequestBody NewsRequest req,
                           @AuthenticationPrincipal UserDetails userDetails) {
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", userDetails.getUsername()));
        newsService.create(req, user.getId());
        return R.ok();
    }

    // 编辑新闻
    @PutMapping("/news/{id}")
    public R<?> updateNews(@PathVariable Long id, @RequestBody NewsRequest req) {
        newsService.update(id, req);
        return R.ok();
    }

    // 删除新闻
    @DeleteMapping("/news/{id}")
    public R<?> deleteNews(@PathVariable Long id) {
        newsService.delete(id);
        return R.ok();
    }

    // 全站评论列表（管理员用，含已屏蔽的）
    @GetMapping("/comments")
    public R<?> allComments(@RequestParam(defaultValue="1") int page,
                             @RequestParam(defaultValue="20") int size) {
        Page<Comment> result = commentMapper.selectPage(new Page<>(page, size),
            new QueryWrapper<Comment>().orderByDesc("created_at"));
        return R.ok(result);
    }
}
```

- [ ] **Step 8: 创建缺少的 DTO**

```java
// CommentRequest.java
package com.maoyouquan.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class CommentRequest { @NotBlank private String content; }

// NewsRequest.java
package com.maoyouquan.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class NewsRequest {
    @NotBlank private String title;
    @NotBlank private String content;
    @NotBlank private String category;
    private String coverUrl;
}
```

- [ ] **Step 9: 运行全部测试**

```bash
cd backend && mvn test -q 2>&1 | tail -10
```

预期：所有测试通过，无失败。

- [ ] **Step 10: 提交**

```bash
git add backend/src/
git commit -m "feat: 评论/新闻/文件上传/管理员 API 完成"
```

---

## Task 9: 前端公共基础（api.js / auth.js / common.js / CSS）

**Files:**
- Create: `frontend/js/api.js`
- Create: `frontend/js/auth.js`
- Create: `frontend/js/common.js`
- Create: `frontend/css/style.css`
- Create: `frontend/css/admin.css`

- [ ] **Step 1: 创建 auth.js（登录状态管理）**

```javascript
// frontend/js/auth.js
const AUTH_KEY = 'mq_token';
const USER_KEY = 'mq_user';

const Auth = {
  save(token, user) {
    localStorage.setItem(AUTH_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  getToken() { return localStorage.getItem(AUTH_KEY); },
  getUser() {
    const u = localStorage.getItem(USER_KEY);
    return u ? JSON.parse(u) : null;
  },
  isLoggedIn() { return !!this.getToken(); },
  isAdmin() { const u = this.getUser(); return u && u.role === 'ADMIN'; },
  logout() {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(USER_KEY);
    window.location.href = '/login.html';
  },
  requireLogin() {
    if (!this.isLoggedIn()) window.location.href = '/login.html';
  },
  requireAdmin() {
    if (!this.isAdmin()) window.location.href = '/index.html';
  }
};
```

- [ ] **Step 2: 创建 api.js（统一 fetch 封装）**

```javascript
// frontend/js/api.js
const BASE = '/api';

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' };
  const token = Auth.getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message || '请求失败');
  return data.data;
}

const api = {
  // 认证
  login: (username, password) => request('POST', '/auth/login', { username, password }),
  register: (data) => request('POST', '/auth/register', data),
  me: () => request('GET', '/auth/me'),

  // 猫咪
  getCats: (params) => request('GET', '/cats?' + new URLSearchParams(params)),
  getCat: (id) => request('GET', `/cats/${id}`),
  submitCat: (data) => request('POST', '/cats', data),
  toggleLike: (id) => request('POST', `/cats/${id}/like`),

  // 评论
  getComments: (catId) => request('GET', `/cats/${catId}/comments`),
  addComment: (catId, content) => request('POST', `/cats/${catId}/comments`, { content }),

  // 新闻
  getNews: (params) => request('GET', '/news?' + new URLSearchParams(params)),
  getNewsDetail: (id) => request('GET', `/news/${id}`),

  // 文件上传
  uploadImage: async (file) => {
    const fd = new FormData();
    fd.append('file', file);
    const headers = {};
    const token = Auth.getToken();
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const res = await fetch(BASE + '/upload/image', { method: 'POST', headers, body: fd });
    const data = await res.json();
    if (!data.success) throw new Error(data.message);
    return data.data.url;
  },

  // 管理员
  getPendingCats: () => request('GET', '/admin/cats/pending'),
  updateCatStatus: (id, status) => request('PUT', `/admin/cats/${id}/status`, { status }),
  blockComment: (id) => request('PUT', `/admin/comments/${id}/block`),
  createNews: (data) => request('POST', '/admin/news', data),
  updateNews: (id, data) => request('PUT', `/admin/news/${id}`, data),
  deleteNews: (id) => request('DELETE', `/admin/news/${id}`),
};
```

- [ ] **Step 3: 创建 common.js（公共导航 + 工具）**

```javascript
// frontend/js/common.js
function renderNav() {
  const user = Auth.getUser();
  const nav = document.getElementById('main-nav');
  if (!nav) return;
  nav.innerHTML = `
    <nav class="navbar">
      <a href="/index.html" class="nav-logo">🐱 猫友圈</a>
      <div class="nav-links">
        <a href="/index.html">首页</a>
        <a href="/news.html">新闻</a>
        ${user ? `
          <a href="/submit-cat.html">提交猫咪</a>
          ${user.role === 'ADMIN' ? '<a href="/admin/index.html">后台</a>' : ''}
          <span class="nav-user">Hi, ${user.nickname}</span>
          <a href="#" onclick="Auth.logout()">退出</a>
        ` : `
          <a href="/login.html">登录</a>
          <a href="/register.html" class="btn-primary">注册</a>
        `}
      </div>
    </nav>`;
}

function renderPagination(container, current, total, onPage) {
  let html = '<div class="pagination">';
  if (current > 1) html += `<button onclick="${onPage}(${current - 1})">上一页</button>`;
  for (let i = 1; i <= total; i++) {
    html += `<button class="${i === current ? 'active' : ''}" onclick="${onPage}(${i})">${i}</button>`;
  }
  if (current < total) html += `<button onclick="${onPage}(${current + 1})">下一页</button>`;
  html += '</div>';
  container.innerHTML = html;
}

function showToast(msg, type = 'success') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 3000);
}

document.addEventListener('DOMContentLoaded', renderNav);
```

- [ ] **Step 4: 创建 style.css（用户端橙色暖调）**

```css
/* frontend/css/style.css */
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #fff7ed; color: #1c1917; }

/* 导航 */
.navbar { display: flex; justify-content: space-between; align-items: center;
  background: linear-gradient(90deg, #f97316, #fb923c); padding: 0 24px;
  height: 56px; box-shadow: 0 2px 8px rgba(249,115,22,.3); position: sticky; top: 0; z-index: 100; }
.nav-logo { color: #fff; font-size: 20px; font-weight: 700; text-decoration: none; }
.nav-links { display: flex; align-items: center; gap: 20px; }
.nav-links a { color: rgba(255,255,255,.9); text-decoration: none; font-size: 14px; }
.nav-links a:hover { color: #fff; }
.nav-user { color: rgba(255,255,255,.8); font-size: 13px; }

/* 主容器 */
.container { max-width: 1100px; margin: 0 auto; padding: 24px 16px; }

/* 猫咪卡片网格 */
.cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; }
.cat-card { background: #fff; border-radius: 12px; padding: 16px; text-align: center;
  cursor: pointer; transition: transform .2s, box-shadow .2s;
  box-shadow: 0 2px 8px rgba(0,0,0,.08); text-decoration: none; color: inherit; }
.cat-card:hover { transform: translateY(-4px); box-shadow: 0 8px 24px rgba(249,115,22,.2); }
.cat-avatar { width: 80px; height: 80px; border-radius: 50%; object-fit: cover;
  margin: 0 auto 10px; border: 3px solid #fed7aa; background: #ffedd5;
  display: flex; align-items: center; justify-content: center; font-size: 36px; }
.cat-name { font-weight: 600; font-size: 15px; color: #1c1917; }
.cat-breed { font-size: 12px; color: #a8a29e; margin-top: 2px; }

/* 搜索栏 */
.search-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.search-bar input, .search-bar select {
  padding: 8px 14px; border: 1.5px solid #fed7aa; border-radius: 20px;
  background: #fff; font-size: 14px; outline: none; }
.search-bar input:focus, .search-bar select:focus { border-color: #f97316; }

/* 按钮 */
.btn-primary { background: #f97316; color: #fff; border: none; border-radius: 20px;
  padding: 8px 20px; cursor: pointer; font-size: 14px; font-weight: 600; }
.btn-primary:hover { background: #ea6c10; }
.btn-outline { background: transparent; color: #f97316; border: 1.5px solid #f97316;
  border-radius: 20px; padding: 8px 20px; cursor: pointer; font-size: 14px; }

/* 猫咪详情 */
.cat-detail { background: #fff; border-radius: 16px; padding: 28px; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
.cat-detail-header { display: flex; gap: 24px; align-items: flex-start; margin-bottom: 24px; }
.cat-detail-avatar { width: 120px; height: 120px; border-radius: 50%; object-fit: cover;
  border: 4px solid #fed7aa; background: #ffedd5; display: flex; align-items: center;
  justify-content: center; font-size: 56px; flex-shrink: 0; }
.cat-info h1 { font-size: 24px; margin-bottom: 8px; }
.cat-info .meta { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
.cat-tag { background: #ffedd5; color: #9a3412; padding: 3px 10px; border-radius: 12px; font-size: 13px; }
.like-btn { display: flex; align-items: center; gap: 6px; background: #fff; border: 1.5px solid #fca5a5;
  color: #ef4444; border-radius: 20px; padding: 6px 16px; cursor: pointer; font-size: 15px; }
.like-btn.liked { background: #fef2f2; }

/* 评论区 */
.comment-section { margin-top: 32px; }
.comment-section h3 { margin-bottom: 16px; font-size: 18px; }
.comment-input { display: flex; gap: 10px; margin-bottom: 20px; }
.comment-input textarea { flex: 1; padding: 10px 14px; border: 1.5px solid #fed7aa;
  border-radius: 10px; resize: vertical; min-height: 60px; font-size: 14px; }
.comment-item { background: #fff; border-radius: 10px; padding: 12px 16px; margin-bottom: 10px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06); }
.comment-author { font-weight: 600; font-size: 13px; color: #f97316; margin-bottom: 4px; }
.comment-content { font-size: 14px; color: #374151; }
.comment-time { font-size: 11px; color: #a8a29e; margin-top: 4px; }

/* 新闻 */
.news-tabs { display: flex; gap: 8px; margin-bottom: 20px; }
.news-tab { padding: 6px 16px; border-radius: 16px; cursor: pointer; font-size: 14px;
  background: #ffedd5; color: #9a3412; border: none; }
.news-tab.active { background: #f97316; color: #fff; }
.news-item { display: flex; gap: 14px; background: #fff; border-radius: 12px;
  padding: 14px; margin-bottom: 12px; cursor: pointer; text-decoration: none; color: inherit;
  box-shadow: 0 1px 6px rgba(0,0,0,.07); transition: box-shadow .2s; }
.news-item:hover { box-shadow: 0 4px 16px rgba(249,115,22,.15); }
.news-cover { width: 80px; height: 60px; border-radius: 8px; object-fit: cover;
  background: #ffedd5; flex-shrink: 0; }
.news-title { font-weight: 600; font-size: 15px; margin-bottom: 6px; }
.news-meta { font-size: 12px; color: #a8a29e; }
.news-category-badge { padding: 2px 8px; border-radius: 10px; font-size: 11px; font-weight: 600; }
.badge-KNOWLEDGE { background: #dcfce7; color: #166534; }
.badge-ANNOUNCEMENT { background: #dbeafe; color: #1e40af; }
.badge-MIGRATION { background: #fef9c3; color: #854d0e; }

/* 表单 */
.form-card { background: #fff; border-radius: 16px; padding: 28px; max-width: 480px;
  margin: 40px auto; box-shadow: 0 2px 12px rgba(0,0,0,.08); }
.form-card h2 { margin-bottom: 24px; font-size: 22px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; color: #78716c; margin-bottom: 6px; font-weight: 500; }
.form-group input, .form-group select, .form-group textarea {
  width: 100%; padding: 10px 14px; border: 1.5px solid #e5e7eb; border-radius: 8px;
  font-size: 14px; transition: border-color .2s; }
.form-group input:focus, .form-group select:focus { border-color: #f97316; outline: none; }

/* 分页 */
.pagination { display: flex; gap: 6px; justify-content: center; margin-top: 24px; }
.pagination button { padding: 6px 12px; border: 1.5px solid #fed7aa; border-radius: 6px;
  background: #fff; cursor: pointer; font-size: 13px; }
.pagination button.active { background: #f97316; color: #fff; border-color: #f97316; }

/* Toast */
.toast { position: fixed; top: 70px; right: 20px; padding: 10px 20px; border-radius: 8px;
  font-size: 14px; z-index: 9999; animation: fadeIn .3s; }
.toast-success { background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; }
.toast-error { background: #fef2f2; color: #991b1b; border: 1px solid #fecaca; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
```

- [ ] **Step 5: 创建 admin.css（深色专业风）**

```css
/* frontend/css/admin.css */
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f1f5f9;
  color: #1e293b; display: flex; min-height: 100vh; }

/* 侧边栏 */
.admin-sidebar { width: 220px; background: #0f172a; min-height: 100vh; padding: 0;
  position: fixed; top: 0; left: 0; bottom: 0; overflow-y: auto; }
.sidebar-logo { padding: 20px 16px; color: #38bdf8; font-size: 16px; font-weight: 700;
  border-bottom: 1px solid #1e293b; }
.sidebar-menu { padding: 12px 0; }
.sidebar-menu a { display: flex; align-items: center; gap: 10px; padding: 10px 16px;
  color: #94a3b8; text-decoration: none; font-size: 14px; transition: all .2s; }
.sidebar-menu a:hover, .sidebar-menu a.active { background: #1e293b; color: #e2e8f0; }
.sidebar-menu a.active { border-left: 3px solid #38bdf8; color: #38bdf8; }

/* 主内容 */
.admin-main { margin-left: 220px; flex: 1; padding: 24px; }
.admin-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.admin-header h1 { font-size: 20px; font-weight: 700; }
.admin-user { color: #64748b; font-size: 13px; display: flex; align-items: center; gap: 10px; }

/* 数据表格 */
.admin-table { width: 100%; border-collapse: collapse; background: #fff;
  border-radius: 10px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,.08); }
.admin-table th { background: #f8fafc; padding: 12px 16px; text-align: left;
  font-size: 12px; font-weight: 600; color: #64748b; text-transform: uppercase; border-bottom: 1px solid #e2e8f0; }
.admin-table td { padding: 12px 16px; font-size: 14px; border-bottom: 1px solid #f1f5f9; }
.admin-table tr:last-child td { border-bottom: none; }
.admin-table tr:hover td { background: #f8fafc; }

/* 管理员按钮 */
.btn-approve { background: #dcfce7; color: #166534; border: none; border-radius: 6px;
  padding: 4px 10px; cursor: pointer; font-size: 12px; }
.btn-reject { background: #fef2f2; color: #991b1b; border: none; border-radius: 6px;
  padding: 4px 10px; cursor: pointer; font-size: 12px; }
.btn-block { background: #fef9c3; color: #854d0e; border: none; border-radius: 6px;
  padding: 4px 10px; cursor: pointer; font-size: 12px; }
.btn-delete { background: #fef2f2; color: #991b1b; border: none; border-radius: 6px;
  padding: 4px 10px; cursor: pointer; font-size: 12px; }
.btn-add { background: #0f172a; color: #38bdf8; border: 1px solid #38bdf8; border-radius: 6px;
  padding: 8px 16px; cursor: pointer; font-size: 13px; }

/* 状态徽章 */
.badge-pending { background: #fef9c3; color: #854d0e; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.badge-approved { background: #dcfce7; color: #166534; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
.badge-rejected { background: #fef2f2; color: #991b1b; padding: 2px 8px; border-radius: 10px; font-size: 12px; }
```

- [ ] **Step 6: 提交**

```bash
git add frontend/
git commit -m "feat: 前端公共模块 api.js/auth.js/common.js + CSS 样式"
```

---

## Task 10: 前端用户页面

**Files:**
- Create: `frontend/login.html`
- Create: `frontend/register.html`
- Create: `frontend/index.html`
- Create: `frontend/cat-detail.html`
- Create: `frontend/news.html`
- Create: `frontend/news-detail.html`
- Create: `frontend/submit-cat.html`

- [ ] **Step 1: 创建 login.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>登录 - 猫友圈</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div id="main-nav"></div>
  <div class="container">
    <div class="form-card">
      <h2>🐱 登录猫友圈</h2>
      <div class="form-group">
        <label>用户名</label>
        <input type="text" id="username" placeholder="请输入用户名">
      </div>
      <div class="form-group">
        <label>密码</label>
        <input type="password" id="password" placeholder="请输入密码">
      </div>
      <button class="btn-primary" style="width:100%;margin-top:8px" onclick="login()">登录</button>
      <p style="text-align:center;margin-top:16px;font-size:14px;color:#78716c">
        还没有账号？<a href="/register.html" style="color:#f97316">立即注册</a>
      </p>
    </div>
  </div>
  <script src="/js/auth.js"></script>
  <script src="/js/api.js"></script>
  <script src="/js/common.js"></script>
  <script>
    async function login() {
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value;
      if (!username || !password) return showToast('请填写用户名和密码', 'error');
      try {
        const data = await api.login(username, password);
        Auth.save(data.token, { nickname: data.nickname, role: data.role });
        showToast('登录成功');
        setTimeout(() => window.location.href = data.role === 'ADMIN' ? '/admin/index.html' : '/index.html', 800);
      } catch(e) { showToast(e.message, 'error'); }
    }
  </script>
</body>
</html>
```

- [ ] **Step 2: 创建 register.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>注册 - 猫友圈</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div id="main-nav"></div>
  <div class="container">
    <div class="form-card">
      <h2>🐾 加入猫友圈</h2>
      <div class="form-group">
        <label>用户名 *</label>
        <input type="text" id="username" placeholder="3-50个字符">
      </div>
      <div class="form-group">
        <label>密码 *</label>
        <input type="password" id="password" placeholder="至少6位">
      </div>
      <div class="form-group">
        <label>昵称</label>
        <input type="text" id="nickname" placeholder="显示名称（选填）">
      </div>
      <div class="form-group">
        <label>邮箱</label>
        <input type="email" id="email" placeholder="选填">
      </div>
      <button class="btn-primary" style="width:100%;margin-top:8px" onclick="register()">注册</button>
      <p style="text-align:center;margin-top:16px;font-size:14px;color:#78716c">
        已有账号？<a href="/login.html" style="color:#f97316">立即登录</a>
      </p>
    </div>
  </div>
  <script src="/js/auth.js"></script>
  <script src="/js/api.js"></script>
  <script src="/js/common.js"></script>
  <script>
    async function register() {
      const username = document.getElementById('username').value.trim();
      const password = document.getElementById('password').value;
      const nickname = document.getElementById('nickname').value.trim();
      const email = document.getElementById('email').value.trim();
      if (!username || !password) return showToast('用户名和密码必填', 'error');
      if (password.length < 6) return showToast('密码至少6位', 'error');
      try {
        await api.register({ username, password, nickname, email });
        showToast('注册成功，请登录');
        setTimeout(() => window.location.href = '/login.html', 800);
      } catch(e) { showToast(e.message, 'error'); }
    }
  </script>
</body>
</html>
```

- [ ] **Step 3: 创建 index.html（猫咪列表首页）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>猫友圈 - 首页</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div id="main-nav"></div>
  <div class="container">
    <div class="search-bar">
      <input type="text" id="nameInput" placeholder="🔍 搜索猫咪姓名...">
      <input type="text" id="breedInput" placeholder="品种筛选...">
      <select id="sortSelect">
        <option value="latest">最新上架</option>
        <option value="likes">点赞热度</option>
      </select>
      <button class="btn-primary" onclick="loadCats(1)">搜索</button>
    </div>
    <div class="cat-grid" id="catGrid"></div>
    <div id="pagination"></div>
  </div>
  <script src="/js/auth.js"></script>
  <script src="/js/api.js"></script>
  <script src="/js/common.js"></script>
  <script>
    let currentPage = 1;

    async function loadCats(page) {
      currentPage = page;
      const params = {
        page, size: 12,
        name: document.getElementById('nameInput').value,
        breed: document.getElementById('breedInput').value,
        sort: document.getElementById('sortSelect').value
      };
      const result = await api.getCats(params);
      const grid = document.getElementById('catGrid');
      grid.innerHTML = result.records.map(cat => `
        <a class="cat-card" href="/cat-detail.html?id=${cat.id}">
          <div class="cat-avatar">
            ${cat.avatarUrl ? `<img src="${cat.avatarUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover">` : '🐱'}
          </div>
          <div class="cat-name">${cat.name}</div>
          <div class="cat-breed">${cat.breed || ''}</div>
        </a>`).join('');
      renderPagination(document.getElementById('pagination'),
        result.current, result.pages, 'loadCats');
    }

    loadCats(1);
  </script>
</body>
</html>
```

- [ ] **Step 4: 创建 cat-detail.html（猫咪详情 + 点赞 + 评论）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>猫咪详情 - 猫友圈</title>
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  <div id="main-nav"></div>
  <div class="container">
    <div class="cat-detail" id="catDetail"></div>
    <div class="comment-section">
      <h3>评论区</h3>
      <div class="comment-input" id="commentInput" style="display:none">
        <textarea id="commentText" placeholder="写下你的评论..."></textarea>
        <button class="btn-primary" onclick="submitComment()">发送</button>
      </div>
      <p id="loginTip" style="color:#a8a29e;font-size:14px;margin-bottom:16px">
        <a href="/login.html" style="color:#f97316">登录</a> 后发表评论
      </p>
      <div id="commentList"></div>
    </div>
  </div>
  <script src="/js/auth.js"></script>
  <script src="/js/api.js"></script>
  <script src="/js/common.js"></script>
  <script>
    const catId = new URLSearchParams(location.search).get('id');
    let liked = false;

    async function load() {
      const cat = await api.getCat(catId);
      document.title = cat.name + ' - 猫友圈';
      document.getElementById('catDetail').innerHTML = `
        <div class="cat-detail-header">
          <div class="cat-detail-avatar">
            ${cat.avatarUrl ? `<img src="${cat.avatarUrl}" style="width:100%;height:100%;border-radius:50%;object-fit:cover">` : '🐱'}
          </div>
          <div class="cat-info">
            <h1>${cat.name}</h1>
            <div class="meta">
              ${cat.gender ? `<span class="cat-tag">${cat.gender === 'MALE' ? '公' : '母'}</span>` : ''}
              ${cat.age ? `<span class="cat-tag">${cat.age}岁</span>` : ''}
              ${cat.breed ? `<span class="cat-tag">${cat.breed}</span>` : ''}
            </div>
            ${cat.personality ? `<p style="color:#78716c;font-size:14px;margin-bottom:14px">${cat.personality}</p>` : ''}
            <button class="like-btn ${liked ? 'liked' : ''}" id="likeBtn" onclick="toggleLike()">
              ❤️ <span id="likeCount">${cat.likeCount}</span> 赞
            </button>
            <div style="color:#a8a29e;font-size:12px;margin-top:8px">上架时间：${new Date(cat.createdAt).toLocaleDateString()}</div>
          </div>
        </div>`;
      if (Auth.isLoggedIn()) {
        document.getElementById('commentInput').style.display = 'flex';
        document.getElementById('loginTip').style.display = 'none';
      }
      loadComments();
    }

    async function toggleLike() {
      if (!Auth.isLoggedIn()) return window.location.href = '/login.html';
      const res = await api.toggleLike(catId);
      liked = res.liked;
      document.getElementById('likeCount').textContent = res.likeCount;
      document.getElementById('likeBtn').className = 'like-btn' + (liked ? ' liked' : '');
    }

    async function loadComments() {
      const comments = await api.getComments(catId);
      document.getElementById('commentList').innerHTML = comments.map(c => `
        <div class="comment-item">
          <div class="comment-author">${c.userId}</div>
          <div class="comment-content">${c.content}</div>
          <div class="comment-time">${new Date(c.createdAt).toLocaleString()}</div>
        </div>`).join('') || '<p style="color:#a8a29e;font-size:14px">暂无评论</p>';
    }

    async function submitComment() {
      const content = document.getElementById('commentText').value.trim();
      if (!content) return showToast('评论不能为空', 'error');
      await api.addComment(catId, content);
      document.getElementById('commentText').value = '';
      showToast('评论成功');
      loadComments();
    }

    load();
  </script>
</body>
</html>
```

- [ ] **Step 5: 创建 news.html + news-detail.html + submit-cat.html**

  - **news.html**：标签栏（全部/科普/公告/迁入迁出）切换调用 `api.getNews({category})`，列表渲染同 index.html 模式。
  - **news-detail.html**：读 `?id=` 参数，调用 `api.getNewsDetail(id)` 渲染标题+内容。
  - **submit-cat.html**：调用 `Auth.requireLogin()`，表单含姓名/性别/年龄/品种/性格，图片支持上传（`api.uploadImage()`）或填 URL，提交调用 `api.submitCat()`。

- [ ] **Step 6: 提交**

```bash
git add frontend/
git commit -m "feat: 前端用户端页面（首页/详情/新闻/登录/注册/提交猫咪）"
```

---

## Task 11: 前端管理后台页面

**Files:**
- Create: `frontend/admin/index.html`
- Create: `frontend/admin/cats.html`
- Create: `frontend/admin/news.html`
- Create: `frontend/admin/comments.html`

每个页面第一行执行 `Auth.requireAdmin()`，使用 `admin.css`，侧边栏用 `common.js` 中已有导航。

- [ ] **Step 1: 创建 admin/cats.html（猫咪审核）**

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <title>猫咪审核 - 管理后台</title>
  <link rel="stylesheet" href="/css/admin.css">
</head>
<body>
  <aside class="admin-sidebar">
    <div class="sidebar-logo">🐱 猫友圈管理</div>
    <nav class="sidebar-menu">
      <a href="/admin/index.html">📊 概览</a>
      <a href="/admin/cats.html" class="active">🐱 猫咪审核</a>
      <a href="/admin/news.html">📰 新闻管理</a>
      <a href="/admin/comments.html">💬 评论管理</a>
      <a href="#" onclick="Auth.logout()">🚪 退出</a>
    </nav>
  </aside>
  <main class="admin-main">
    <div class="admin-header">
      <h1>🐱 待审核猫咪</h1>
    </div>
    <table class="admin-table">
      <thead><tr><th>ID</th><th>姓名</th><th>品种</th><th>提交时间</th><th>操作</th></tr></thead>
      <tbody id="catTable"></tbody>
    </table>
  </main>
  <script src="/js/auth.js"></script>
  <script src="/js/api.js"></script>
  <script src="/js/common.js"></script>
  <script>
    Auth.requireAdmin();
    async function load() {
      const result = await api.getPendingCats();
      document.getElementById('catTable').innerHTML = result.records.map(c => `
        <tr>
          <td>${c.id}</td>
          <td>${c.name}</td>
          <td>${c.breed || '-'}</td>
          <td>${new Date(c.createdAt).toLocaleDateString()}</td>
          <td>
            <button class="btn-approve" onclick="review(${c.id},'APPROVED')">通过</button>
            <button class="btn-reject" onclick="review(${c.id},'REJECTED')">拒绝</button>
          </td>
        </tr>`).join('');
    }
    async function review(id, status) {
      await api.updateCatStatus(id, status);
      showToast(status === 'APPROVED' ? '已通过' : '已拒绝');
      load();
    }
    load();
  </script>
</body>
</html>
```

- [ ] **Step 2: 创建 admin/news.html（新闻管理）**

  表格展示所有新闻（标题/分类/时间），右上角「+ 新增」打开模态框填写标题/内容/分类/封面URL，调用 `api.createNews()`；每行有「编辑」「删除」按钮。

- [ ] **Step 3: 创建 admin/comments.html（评论屏蔽）**

  调用 `GET /api/admin/comments?page=1&size=20` 获取全站评论（含已屏蔽）。同时在 `js/api.js` 的 `api` 对象末尾补充：
  ```javascript
  getAllComments: (page) => request('GET', `/admin/comments?page=${page}&size=20`),
  ```
  每条评论显示内容、userId、创建时间、屏蔽状态，未屏蔽的显示「屏蔽」按钮调用 `api.blockComment(id)` 后刷新列表。

- [ ] **Step 4: 创建 admin/index.html（概览）**

  简单展示欢迎语和快速入口链接即可。

- [ ] **Step 5: 提交**

```bash
git add frontend/admin/
git commit -m "feat: 管理后台页面（猫咪审核/新闻管理/评论屏蔽）"
```

---

## Task 12: Nginx 配置 + 整合验证

**Files:**
- Create: `nginx.conf`

- [ ] **Step 1: 创建 nginx.conf**

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /path/to/demo/frontend;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 反向代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # 图片上传文件访问
    location /uploads/ {
        alias /path/to/demo/backend/uploads/;
        expires 7d;
        add_header Cache-Control "public";
    }
}
```

将 `/path/to/demo` 替换为实际项目路径。

- [ ] **Step 2: 端对端验证清单**

```bash
# 1. 启动 MySQL，运行建表脚本
mysql -u root -p1234 < backend/src/main/resources/sql/init.sql

# 2. 启动后端
cd backend && mvn spring-boot:run

# 3. 配置 Nginx 并启动
nginx -c /path/to/demo/nginx.conf

# 4. 注册普通用户
curl -s -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"catfan","password":"pass123","nickname":"猫迷"}' | python -m json.tool

# 5. 登录获取 token
TOKEN=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"catfan","password":"pass123"}' | python -m json.tool | grep '"token"' | cut -d'"' -f4)

# 6. 提交猫咪
curl -s -X POST http://localhost/api/cats \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"小橘","gender":"MALE","age":2,"breed":"橘猫","personality":"活泼好动"}' | python -m json.tool

# 7. 管理员登录（密码 admin123）并审核
ADMIN_TOKEN=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | python -m json.tool | grep '"token"' | cut -d'"' -f4)

curl -s -X GET http://localhost/api/admin/cats/pending \
  -H "Authorization: Bearer $ADMIN_TOKEN" | python -m json.tool

# 审核通过（替换 {id} 为实际 ID）
curl -s -X PUT http://localhost/api/admin/cats/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status":"APPROVED"}' | python -m json.tool

# 8. 验证首页列表
curl -s "http://localhost/api/cats?sort=likes" | python -m json.tool

# 9. 测试搜索
curl -s "http://localhost/api/cats?name=小&breed=橘" | python -m json.tool

# 10. 浏览器打开 http://localhost 验证 UI
```

- [ ] **Step 3: 提交最终版**

```bash
git add nginx.conf
git commit -m "feat: Nginx 配置 + 端对端验证完成，猫友圈项目交付"
```

---

## 验证核查表

- [ ] 注册/登录，JWT 返回正常
- [ ] 提交猫咪 → 状态 PENDING，管理员审核后变 APPROVED，首页可见
- [ ] 首页分页（每页12条）
- [ ] 姓名模糊搜索 + 品种筛选 + 热度/最新排序
- [ ] 点赞 → like_count+1，再点 → like_count-1，跨用户互不干扰
- [ ] 评论发布即显示，管理员屏蔽后不可见
- [ ] 新闻三分类筛选正常
- [ ] 管理员不能访问普通接口绕权，普通用户不能访问 /api/admin/*
- [ ] 图片上传（文件 + URL）均正常显示
- [ ] 用户端橙色风格，管理端深色风格，视觉明显区分
