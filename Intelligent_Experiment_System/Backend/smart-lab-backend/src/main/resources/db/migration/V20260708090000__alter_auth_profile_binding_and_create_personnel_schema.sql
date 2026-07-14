ALTER TABLE `user_account`
    ADD COLUMN `person_id` BIGINT UNSIGNED NULL COMMENT '人员id' AFTER `username`;

UPDATE `user_account` ua
JOIN `user_profile` up ON up.user_id = ua.id
SET ua.person_id = up.person_id
WHERE ua.person_id IS NULL;

ALTER TABLE `user_account`
    MODIFY COLUMN `person_id` BIGINT UNSIGNED NOT NULL COMMENT '人员id';

ALTER TABLE `user_account`
    ADD UNIQUE KEY `uk_user_account_person_id` (`person_id`);

ALTER TABLE `user_profile`
    DROP INDEX `uk_user_profile_person_id`;

ALTER TABLE `user_profile`
    DROP COLUMN `person_id`;

CREATE TABLE `department` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `department_no` VARCHAR(32) NOT NULL COMMENT '部门编号',
    `department_code` VARCHAR(64) NOT NULL COMMENT '部门编码',
    `department_name` VARCHAR(128) NOT NULL COMMENT '部门名称',
    `parent_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '上级部门id',
    `leader_person_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人人员id',
    `department_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '部门状态：1启用 2停用',
    `sort_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '部门描述',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_department_department_no` (`department_no`),
    UNIQUE KEY `uk_department_department_code` (`department_code`),
    KEY `idx_department_parent_id` (`parent_id`),
    KEY `idx_department_leader_person_id` (`leader_person_id`),
    KEY `idx_department_department_status` (`department_status`),
    KEY `idx_department_sort_no` (`sort_no`),
    CONSTRAINT `ck_department_department_status` CHECK (`department_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='组织部门表';

CREATE TABLE `position` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `position_no` VARCHAR(32) NOT NULL COMMENT '岗位编号',
    `position_code` VARCHAR(64) NOT NULL COMMENT '岗位编码',
    `position_name` VARCHAR(128) NOT NULL COMMENT '岗位名称',
    `position_type` TINYINT UNSIGNED NOT NULL COMMENT '岗位类型：1学生岗位 2教师岗位 3管理岗位',
    `department_id` BIGINT UNSIGNED NOT NULL COMMENT '所属部门id',
    `position_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '岗位状态：1启用 2停用',
    `sort_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '岗位描述',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_position_no` (`position_no`),
    UNIQUE KEY `uk_position_position_code` (`position_code`),
    KEY `idx_position_department_id` (`department_id`),
    KEY `idx_position_position_type` (`position_type`),
    KEY `idx_position_position_status` (`position_status`),
    KEY `idx_position_sort_no` (`sort_no`),
    CONSTRAINT `ck_position_position_type` CHECK (`position_type` IN (1, 2, 3)),
    CONSTRAINT `ck_position_position_status` CHECK (`position_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='岗位表';

CREATE TABLE `person` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `person_no` VARCHAR(32) NOT NULL COMMENT '人员编号',
    `person_name` VARCHAR(64) NOT NULL COMMENT '人员姓名',
    `person_type` TINYINT UNSIGNED NOT NULL COMMENT '人员类型：1学生 2教师 3管理员',
    `gender_type` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `id_card_no_masked` VARCHAR(32) DEFAULT NULL COMMENT '身份证号脱敏值',
    `student_no` VARCHAR(32) DEFAULT NULL COMMENT '学号',
    `employee_no` VARCHAR(32) DEFAULT NULL COMMENT '工号',
    `department_id` BIGINT UNSIGNED NOT NULL COMMENT '所属部门id',
    `position_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '岗位id',
    `class_name` VARCHAR(128) DEFAULT NULL COMMENT '班级名称',
    `title_name` VARCHAR(64) DEFAULT NULL COMMENT '教师职称',
    `person_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '人员状态：1正常 2停用 3冻结 4离组',
    `entry_at` DATETIME DEFAULT NULL COMMENT '入组/入职/入学时间',
    `leave_at` DATETIME DEFAULT NULL COMMENT '离组/离职/毕业时间',
    `source_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '来源类型：1手工新增 2批量导入 3外部同步',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_person_person_no` (`person_no`),
    UNIQUE KEY `uk_person_student_no` (`student_no`),
    UNIQUE KEY `uk_person_employee_no` (`employee_no`),
    KEY `idx_person_person_name` (`person_name`),
    KEY `idx_person_person_type` (`person_type`),
    KEY `idx_person_department_id` (`department_id`),
    KEY `idx_person_position_id` (`position_id`),
    KEY `idx_person_person_status` (`person_status`),
    KEY `idx_person_phone` (`phone`),
    CONSTRAINT `ck_person_person_type` CHECK (`person_type` IN (1, 2, 3)),
    CONSTRAINT `ck_person_gender_type` CHECK (`gender_type` IN (0, 1, 2)),
    CONSTRAINT `ck_person_person_status` CHECK (`person_status` IN (1, 2, 3, 4)),
    CONSTRAINT `ck_person_source_type` CHECK (`source_type` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='人员档案表';
