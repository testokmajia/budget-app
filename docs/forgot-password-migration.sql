-- ============================================
-- 忘记密码功能 — 数据库迁移脚本
-- 生产环境（ddl-auto: validate）需手动执行
-- dev环境（ddl-auto: update）Hibernate自动处理
-- ============================================

-- 1. users 表新增 email 字段
ALTER TABLE users ADD COLUMN email VARCHAR(100) NULL;
CREATE UNIQUE INDEX idx_users_email ON users(email);

-- 2. 新建密码重置验证码表
CREATE TABLE IF NOT EXISTS verification_codes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(128)  NOT NULL COMMENT '邮箱地址',
    code        VARCHAR(6)    NOT NULL COMMENT '6位验证码',
    used        TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否已使用',
    attempts    INT           NOT NULL DEFAULT 0 COMMENT '错误尝试次数',
    reset_token VARCHAR(64)   NULL COMMENT '验证通过后的重置令牌',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_vc_email (email),
    INDEX idx_vc_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码重置验证码表';
