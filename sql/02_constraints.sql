-- ========================================================
-- 02_constraints.sql
-- 数据库约束、索引和触发器
-- 执行顺序：在01_schema.sql之后执行
-- ========================================================

USE group15_lab;

-- ========================================================
-- 1. 检查约束 (CHECK Constraints)
-- ========================================================

-- 用户名格式验证：只允许字母、数字、下划线，长度4-20
ALTER TABLE users
    ADD CONSTRAINT chk_username_pattern
    CHECK (username REGEXP '^[a-zA-Z0-9_]{4,20}$');

-- 邮箱格式验证（可选）
ALTER TABLE user_profiles
    ADD CONSTRAINT chk_email_pattern
    CHECK (email IS NULL OR email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$');

-- 手机号格式验证（中国大陆，可选）
ALTER TABLE user_profiles
    ADD CONSTRAINT chk_phone_pattern
    CHECK (phone IS NULL OR phone REGEXP '^1[3-9]\\d{9}$');

-- add.
-- 团队角色: Owner / Admin / Member
ALTER TABLE team_member
    ADD CONSTRAINT chk_team_member_role
    CHECK (role IN ('Owner', 'Admin', 'Member'));

-- 任务状态: TODO / IN_PROGRESS / DONE
ALTER TABLE task
    ADD CONSTRAINT chk_task_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));

-- 任务优先级: low / medium / high / urgent
ALTER TABLE task
    ADD CONSTRAINT chk_task_priority
    CHECK (priority IN ('low', 'medium', 'high', 'urgent'));

-- ========================================================
-- 2. 索引 (Indexes)
-- ========================================================

-- 用户表索引
-- 用户名唯一索引（已在CREATE TABLE中定义UNIQUE，这里仅作说明）
-- CREATE UNIQUE INDEX idx_users_username ON users(username);

-- 创建时间索引（用于查询排序和分页）
CREATE INDEX idx_users_created_at ON users(created_at);

-- 更新时间索引（用于同步或审计）
CREATE INDEX idx_users_updated_at ON users(updated_at);

-- 用户资料表索引
CREATE INDEX idx_user_profiles_email ON user_profiles(email);
CREATE INDEX idx_user_profiles_phone ON user_profiles(phone);

-- add.
-- 团队表索引
CREATE INDEX idx_team_owner_id ON team(owner_id);

-- 团队成员索引
CREATE INDEX idx_team_member_team_id ON team_member(team_id);
CREATE INDEX idx_team_member_user_id ON team_member(user_id);
CREATE INDEX idx_team_member_role ON team_member(role);

-- 任务表索引
CREATE INDEX idx_task_team_id ON task(team_id);
CREATE INDEX idx_task_assignee_id ON task(assignee_id);

-- Lab2 新增索引：优化团队权限判定查询性能
CREATE INDEX idx_team_member_team_role ON team_member(team_id, role);

-- ========================================================
-- 3. 触发器 (Triggers) - 可选
-- ========================================================

-- 示例：在插入用户前验证用户名格式（双重保障）
-- 注意：MySQL的触发器不能替代CHECK约束，但可以作为额外验证
/*
DELIMITER $$

CREATE TRIGGER trg_users_before_insert
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
    IF NEW.username NOT REGEXP '^[a-zA-Z0-9_]{4,20}$' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '用户名格式错误：只允许字母、数字、下划线，长度4-20';
    END IF;

    IF LENGTH(NEW.password_hash) < 60 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '密码哈希值长度异常';
    END IF;
END$$

CREATE TRIGGER trg_users_before_update
BEFORE UPDATE ON users
FOR EACH ROW
BEGIN
    IF NEW.username NOT REGEXP '^[a-zA-Z0-9_]{4,20}$' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '用户名格式错误：只允许字母、数字、下划线，长度4-20';
    END IF;
END$$

DELIMITER ;
*/

-- ========================================================
-- 4. 存储过程 (Stored Procedures) - 可选
-- ========================================================

-- 示例：验证用户名是否可用
/*
DELIMITER $$

CREATE PROCEDURE sp_check_username_available(
    IN p_username VARCHAR(20),
    OUT p_available BOOLEAN
)
BEGIN
    DECLARE user_count INT;

    SELECT COUNT(*) INTO user_count
    FROM users
    WHERE username = p_username;

    SET p_available = (user_count = 0);
END$$

DELIMITER ;
*/

-- 示例：获取用户总数
/*
DELIMITER $$

CREATE PROCEDURE sp_get_user_count()
BEGIN
    SELECT COUNT(*) as total_users FROM users;
END$$

DELIMITER ;
*/

-- ========================================================
-- 执行说明
-- ========================================================
-- 运行方式：
--   mysql -u root -p < 02_constraints.sql
--
-- 注意事项：
--   1. 必须先执行01_schema.sql创建表
--   2. CHECK约束需要MySQL 8.0.16+
--   3. 触发器部分已被注释，如需使用请取消注释
