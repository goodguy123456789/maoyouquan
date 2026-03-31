CREATE DATABASE IF NOT EXISTS maoyouquan DEFAULT CHARACTER SET utf8mb4;
USE maoyouquan;

CREATE TABLE IF NOT EXISTS users (
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

CREATE TABLE IF NOT EXISTS cats (
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

CREATE TABLE IF NOT EXISTS cat_likes (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    UNIQUE KEY uk_cat_user (cat_id, user_id),
    FOREIGN KEY (cat_id) REFERENCES cats(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    is_blocked TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT NOW(),
    FOREIGN KEY (cat_id) REFERENCES cats(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS news (
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

-- 初始管理员账号（密码 admin123，MD5）
INSERT IGNORE INTO users (username, password_hash, nickname, role)
VALUES ('admin', '0192023a7bbd73250516f069df18b500', '管理员', 'ADMIN');
