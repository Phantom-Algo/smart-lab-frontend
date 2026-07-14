CREATE TABLE `user_account` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `account_no` VARCHAR(32) NOT NULL COMMENT '账号编号',
    `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `password_salt` VARCHAR(64) NOT NULL COMMENT '密码盐值',
    `user_type` TINYINT UNSIGNED NOT NULL COMMENT '用户类型：1学生 2教师 3管理员',
    `account_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '账号状态：1正常 2停用 3锁定',
    `failed_login_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_expires_at` DATETIME DEFAULT NULL COMMENT '锁定失效时间',
    `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
    `last_password_changed_at` DATETIME DEFAULT NULL COMMENT '最后修改密码时间',
    `is_password_reset_required` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否首次登录强制改密：0否 1是',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_account_account_no` (`account_no`),
    UNIQUE KEY `uk_user_account_username` (`username`),
    KEY `idx_user_account_user_type` (`user_type`),
    KEY `idx_user_account_account_status` (`account_status`),
    KEY `idx_user_account_last_login_at` (`last_login_at`),
    CONSTRAINT `ck_user_account_user_type` CHECK (`user_type` IN (1, 2, 3)),
    CONSTRAINT `ck_user_account_account_status` CHECK (`account_status` IN (1, 2, 3)),
    CONSTRAINT `ck_user_account_is_password_reset_required` CHECK (`is_password_reset_required` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户账号表';

CREATE TABLE `user_profile` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号id',
    `real_name` VARCHAR(64) NOT NULL COMMENT '真实姓名',
    `person_id` BIGINT UNSIGNED NOT NULL COMMENT '人员id',
    `gender_type` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar_url` VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `student_no` VARCHAR(32) DEFAULT NULL COMMENT '学号',
    `employee_no` VARCHAR(32) DEFAULT NULL COMMENT '工号',
    `department_name` VARCHAR(128) DEFAULT NULL COMMENT '院系/部门名称',
    `class_name` VARCHAR(128) DEFAULT NULL COMMENT '班级名称',
    `title_name` VARCHAR(64) DEFAULT NULL COMMENT '教师职称',
    `profile_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '资料状态：1正常 2停用',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profile_user_id` (`user_id`),
    UNIQUE KEY `uk_user_profile_person_id` (`person_id`),
    UNIQUE KEY `uk_user_profile_student_no` (`student_no`),
    UNIQUE KEY `uk_user_profile_employee_no` (`employee_no`),
    KEY `idx_user_profile_real_name` (`real_name`),
    KEY `idx_user_profile_phone` (`phone`),
    KEY `idx_user_profile_department_name` (`department_name`),
    KEY `idx_user_profile_person_id` (`person_id`),
    CONSTRAINT `ck_user_profile_gender_type` CHECK (`gender_type` IN (0, 1, 2)),
    CONSTRAINT `ck_user_profile_profile_status` CHECK (`profile_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人信息表';

CREATE TABLE `login_session` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号id',
    `session_no` VARCHAR(64) NOT NULL COMMENT '会话编号',
    `access_token_hash` VARCHAR(255) NOT NULL COMMENT '访问令牌哈希',
    `refresh_token_hash` VARCHAR(255) DEFAULT NULL COMMENT '刷新令牌哈希',
    `session_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '会话状态：1在线 2已退出 3已过期 4已失效',
    `login_at` DATETIME NOT NULL COMMENT '登录时间',
    `expire_at` DATETIME NOT NULL COMMENT '过期时间',
    `logout_at` DATETIME DEFAULT NULL COMMENT '退出时间',
    `ip_address` VARCHAR(45) DEFAULT NULL COMMENT '登录IP',
    `device_type` VARCHAR(32) DEFAULT NULL COMMENT '设备类型',
    `user_agent` VARCHAR(255) DEFAULT NULL COMMENT '浏览器/客户端标识',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_login_session_session_no` (`session_no`),
    KEY `idx_login_session_user_id` (`user_id`),
    KEY `idx_login_session_session_status` (`session_status`),
    KEY `idx_login_session_expire_at` (`expire_at`),
    KEY `idx_login_session_login_at` (`login_at`),
    CONSTRAINT `ck_login_session_session_status` CHECK (`session_status` IN (1, 2, 3, 4))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='登录会话表';

CREATE TABLE `resource_permission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户账号id',
    `resource_type` TINYINT UNSIGNED NOT NULL COMMENT '资源类型：1器材 2场地',
    `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资源id',
    `action_code` VARCHAR(64) NOT NULL COMMENT '操作编码，如 view reserve use manage',
    `permission_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '权限状态：1有效 2失效',
    `effective_at` DATETIME DEFAULT NULL COMMENT '生效时间',
    `expire_at` DATETIME DEFAULT NULL COMMENT '失效时间',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '权限说明',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rp_user_res_type_res_id_action` (`user_id`, `resource_type`, `resource_id`, `action_code`),
    KEY `idx_resource_permission_user_id` (`user_id`),
    KEY `idx_resource_permission_resource_type_resource_id` (`resource_type`, `resource_id`),
    KEY `idx_resource_permission_action_code` (`action_code`),
    KEY `idx_resource_permission_permission_status` (`permission_status`),
    CONSTRAINT `ck_resource_permission_resource_type` CHECK (`resource_type` IN (1, 2)),
    CONSTRAINT `ck_resource_permission_permission_status` CHECK (`permission_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='资源权限表';
