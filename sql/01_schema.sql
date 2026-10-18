-- ========================================================
-- 01_schema.sql
-- 数据库表结构定义 (DDL)
-- 执行顺序：第一个执行
-- ========================================================

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS group15_lab
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE group15_lab;

-- 2. 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID，主键',

    -- 用户名：4-20位，只允许字母、数字、下划线，不得重复
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名，唯一',

    -- 密码哈希值：使用bcrypt存储（包含salt）
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',

    -- 时间戳
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间'

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户表';

-- 3. 用户资料扩展表
CREATE TABLE user_profiles (
    user_id BIGINT PRIMARY KEY COMMENT '用户ID，关联users.id',

    -- 用户资料字段
    email VARCHAR(255) COMMENT '邮箱',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    full_name VARCHAR(100) COMMENT '真实姓名',
    phone VARCHAR(20) COMMENT '手机号',

    -- 时间戳
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='用户资料扩展表';

-- 4. 登录状态维护表
CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,  -- Refresh Token字符串
    expires_at DATETIME(6) NOT NULL,       -- 过期时间
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user_id (user_id),
    INDEX idx_refresh_tokens_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- add. 团队表
CREATE TABLE team (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '团队ID',
    name VARCHAR(50) NOT NULL COMMENT '团队名称',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    owner_id BIGINT NOT NULL COMMENT '创建者（Owner）',
    created_at DATE NOT NULL,
    updated_at DATE NOT NULL,

    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队表';

-- add. 团队成员表（Owner/Admin/Member）
CREATE TABLE team_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL COMMENT '团队ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role ENUM('Owner','Admin','Member') NOT NULL COMMENT '角色：Owner/Admin/Member',
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    UNIQUE KEY uk_team_user (team_id, user_id),
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队成员与角色表';

-- modify.任务表
CREATE TABLE IF NOT EXISTS task (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    user_id       BIGINT NOT NULL COMMENT '创建者ID',
    team_id       BIGINT NULL COMMENT '所属团队,NULL=个人任务',
    assignee_id    BIGINT NULL COMMENT '被分配的用户ID',
    former_assignee_id BIGINT NULL,
    former_assignee_name VARCHAR(50) NULL,
    title         VARCHAR(100)    NOT NULL,
    description   TEXT,
    status        VARCHAR(20)     NOT NULL DEFAULT 'TODO',
    priority      VARCHAR(20)     NOT NULL DEFAULT 'medium',
    deadline      DATE,
    created_at DATE NOT NULL,
    updated_at DATE NOT NULL,

    PRIMARY KEY (id),
    KEY idx_task_user (user_id),
    KEY idx_task_team (team_id),
    KEY idx_task_assignee (assignee_id),
    KEY idx_task_team_status (team_id, status),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE CASCADE,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表（支持团队与分配）';

-- Lab4. 任务操作日志表
CREATE TABLE IF NOT EXISTS task_operation_log (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    task_id        BIGINT NOT NULL COMMENT '任务ID',
    actor_id       BIGINT NOT NULL COMMENT '操作人ID',
    actor_name     VARCHAR(50) NOT NULL COMMENT '操作人用户名',
    operation_type VARCHAR(50) NOT NULL COMMENT '操作类型',
    scope_type     VARCHAR(20) NOT NULL COMMENT '任务范围：PERSONAL/TEAM',
    team_id        BIGINT NULL COMMENT '团队ID，个人任务为空',
    summary        VARCHAR(255) NOT NULL COMMENT '简要描述',
    old_value      TEXT COMMENT '修改前内容',
    new_value      TEXT COMMENT '修改后内容',
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '日志创建时间',

    PRIMARY KEY (id),
    KEY idx_task_operation_log_task_created (task_id, created_at, id),
    KEY idx_task_operation_log_actor_id (actor_id),
    KEY idx_task_operation_log_team_id (team_id),

    FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务操作日志表';

-- Dependencies follow the JPA entity; cycle checks are enforced in the service.
CREATE TABLE task_dependency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    predecessor_task_id BIGINT NOT NULL,
    successor_task_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_task_dependency_pair UNIQUE (predecessor_task_id, successor_task_id),
    FOREIGN KEY (predecessor_task_id) REFERENCES task(id),
    FOREIGN KEY (successor_task_id) REFERENCES task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
