CREATE TABLE `course` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_code` VARCHAR(32) NOT NULL COMMENT '课程编码，全局唯一',
    `course_name` VARCHAR(128) NOT NULL COMMENT '课程名称',
    `semester` VARCHAR(32) NOT NULL COMMENT '学期，如 2026-2027-1',
    `credit` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '学分',
    `total_hours` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '总学时',
    `teacher_id` BIGINT UNSIGNED NOT NULL COMMENT '授课教师ID，逻辑关联 person.id',
    `department_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '开课院系ID，逻辑关联 department.id',
    `description` VARCHAR(512) DEFAULT NULL COMMENT '课程简介',
    `syllabus` TEXT DEFAULT NULL COMMENT '教学大纲',
    `course_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '课程状态：1进行中 2已结课 3已归档',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间，软删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code` (`course_code`),
    UNIQUE KEY `uk_course_semester_name` (`semester`, `course_name`),
    KEY `idx_course_name` (`course_name`),
    KEY `idx_course_semester` (`semester`),
    KEY `idx_course_teacher_id` (`teacher_id`),
    KEY `idx_course_department_id` (`department_id`),
    KEY `idx_course_status` (`course_status`),
    KEY `idx_course_deleted_at` (`deleted_at`),
    CONSTRAINT `ck_course_credit` CHECK (`credit` > 0),
    CONSTRAINT `ck_course_total_hours` CHECK (`total_hours` > 0),
    CONSTRAINT `ck_course_status` CHECK (`course_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程表';

CREATE TABLE `course_student` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学生ID，逻辑关联 person.id',
    `enroll_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '选课状态：1在课 2退课',
    `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入课程时间',
    `withdrawn_at` DATETIME DEFAULT NULL COMMENT '退课时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_student` (`course_id`, `student_id`),
    KEY `idx_course_student_course_id` (`course_id`, `enroll_status`),
    KEY `idx_course_student_student_id` (`student_id`, `enroll_status`),
    CONSTRAINT `ck_course_student_enroll_status` CHECK (`enroll_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程学生关联表';

CREATE TABLE `course_lab` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
    `bind_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '绑定状态：1已绑定 2已解绑',
    `bound_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `unbound_at` DATETIME DEFAULT NULL COMMENT '解绑时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_lab` (`course_id`, `lab_id`),
    KEY `idx_course_lab_course_id` (`course_id`),
    KEY `idx_course_lab_lab_id` (`lab_id`),
    CONSTRAINT `ck_course_lab_bind_status` CHECK (`bind_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程实验室关联表';

CREATE TABLE `teaching_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_no` VARCHAR(32) NOT NULL COMMENT '任务编号，全局唯一',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `task_title` VARCHAR(255) NOT NULL COMMENT '任务标题',
    `task_content` MEDIUMTEXT NOT NULL COMMENT '任务内容',
    `submit_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '提交类型：1文本 2文件 3混合',
    `max_file_size` INT UNSIGNED NOT NULL DEFAULT 10 COMMENT '最大文件大小（MB）',
    `deadline` DATETIME NOT NULL COMMENT '提交截止时间',
    `task_weight` DECIMAL(5,2) DEFAULT NULL COMMENT '任务在课程总成绩中的权重（%）',
    `task_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '任务状态：1草稿 2已发布 3已关闭',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `created_by` BIGINT UNSIGNED NOT NULL COMMENT '创建教师ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间，软删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teaching_task_no` (`task_no`),
    KEY `idx_teaching_task_course_status` (`course_id`, `task_status`),
    KEY `idx_teaching_task_deadline` (`deadline`),
    KEY `idx_teaching_task_created_by` (`created_by`),
    KEY `idx_teaching_task_deleted_at` (`deleted_at`),
    CONSTRAINT `ck_teaching_task_submit_type` CHECK (`submit_type` IN (1, 2, 3)),
    CONSTRAINT `ck_teaching_task_max_file_size` CHECK (`max_file_size` > 0),
    CONSTRAINT `ck_teaching_task_weight` CHECK (`task_weight` IS NULL OR (`task_weight` >= 0 AND `task_weight` <= 100)),
    CONSTRAINT `ck_teaching_task_status` CHECK (`task_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学任务表';

CREATE TABLE `teaching_resource` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `resource_no` VARCHAR(32) NOT NULL COMMENT '资源编号，全局唯一',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `resource_name` VARCHAR(255) NOT NULL COMMENT '资源名称',
    `resource_type` TINYINT UNSIGNED NOT NULL COMMENT '资源类型：1课件PPT 2课件Word 3课件PDF 4视频链接 5参考链接 6其他',
    `file_url` VARCHAR(512) DEFAULT NULL COMMENT '文件链接',
    `external_url` VARCHAR(512) DEFAULT NULL COMMENT '外部链接',
    `file_size` BIGINT UNSIGNED DEFAULT NULL COMMENT '文件大小（字节）',
    `sort_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    `uploaded_by` BIGINT UNSIGNED NOT NULL COMMENT '上传人ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teaching_resource_no` (`resource_no`),
    KEY `idx_teaching_resource_course_id` (`course_id`, `resource_type`),
    KEY `idx_teaching_resource_uploaded_by` (`uploaded_by`),
    KEY `idx_teaching_resource_sort_no` (`sort_no`),
    CONSTRAINT `ck_teaching_resource_type` CHECK (`resource_type` IN (1, 2, 3, 4, 5, 6))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学资源表';
