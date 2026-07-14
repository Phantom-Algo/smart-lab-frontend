CREATE TABLE `task_submission` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `submission_no` VARCHAR(32) NOT NULL COMMENT '提交编号，全局唯一',
    `task_id` BIGINT UNSIGNED NOT NULL COMMENT '教学任务ID，逻辑关联 teaching_task.id',
    `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学生ID，逻辑关联 person.id',
    `content_text` MEDIUMTEXT DEFAULT NULL COMMENT '文本内容',
    `attachments` JSON DEFAULT NULL COMMENT '附件列表',
    `version_no` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '版本号',
    `parent_submission_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '上一版本提交ID',
    `current_flag` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否当前版本：0否 1是',
    `submit_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '提交状态：1已提交 2已退回 3已评分',
    `is_late` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否迟交：0否 1是',
    `submitted_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    `returned_reason` VARCHAR(255) DEFAULT NULL COMMENT '退回原因',
    `returned_at` DATETIME DEFAULT NULL COMMENT '退回时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_submission_no` (`submission_no`),
    KEY `idx_task_submission_task_student` (`task_id`, `student_id`),
    KEY `idx_task_submission_task_current` (`task_id`, `current_flag`, `submit_status`),
    KEY `idx_task_submission_student_current` (`student_id`, `current_flag`, `submit_status`),
    KEY `idx_task_submission_parent_submission_id` (`parent_submission_id`),
    KEY `idx_task_submission_submitted_at` (`submitted_at`),
    CONSTRAINT `ck_task_submission_status` CHECK (`submit_status` IN (1, 2, 3)),
    CONSTRAINT `ck_task_submission_is_late` CHECK (`is_late` IN (0, 1)),
    CONSTRAINT `ck_task_submission_current_flag` CHECK (`current_flag` IN (0, 1)),
    CONSTRAINT `ck_task_submission_version` CHECK (`version_no` >= 1)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教学任务提交表';

CREATE TABLE `grade_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `item_name` VARCHAR(128) NOT NULL COMMENT '评定项名称',
    `item_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评定项类型：1任务类 2平时类 3期末类 4其他',
    `item_weight` DECIMAL(5,2) NOT NULL COMMENT '权重（%）',
    `full_score` DECIMAL(6,2) NOT NULL DEFAULT 100.00 COMMENT '满分值',
    `related_task_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联教学任务ID',
    `sort_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grade_item_course_name` (`course_id`, `item_name`),
    UNIQUE KEY `uk_grade_item_related_task` (`related_task_id`),
    KEY `idx_grade_item_course_id` (`course_id`),
    KEY `idx_grade_item_item_type` (`item_type`),
    CONSTRAINT `ck_grade_item_type` CHECK (`item_type` IN (1, 2, 3, 4)),
    CONSTRAINT `ck_grade_item_weight` CHECK (`item_weight` > 0 AND `item_weight` <= 100),
    CONSTRAINT `ck_grade_item_full_score` CHECK (`full_score` > 0)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='成绩评定项表';

CREATE TABLE `grade_assessment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `grade_item_id` BIGINT UNSIGNED NOT NULL COMMENT '评定项ID',
    `student_id` BIGINT UNSIGNED NOT NULL COMMENT '学生ID',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `task_submission_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联提交ID',
    `score` DECIMAL(6,2) DEFAULT NULL COMMENT '得分',
    `grader_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '评分人ID',
    `graded_at` DATETIME DEFAULT NULL COMMENT '评分时间',
    `comment` VARCHAR(255) DEFAULT NULL COMMENT '评语',
    `grade_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '成绩状态：1草稿 2已发布',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_grade_assessment` (`grade_item_id`, `student_id`),
    KEY `idx_grade_assessment_course_status` (`course_id`, `grade_status`),
    KEY `idx_grade_assessment_student_status` (`student_id`, `grade_status`),
    KEY `idx_grade_assessment_grader_id` (`grader_id`),
    KEY `idx_grade_assessment_task_submission_id` (`task_submission_id`),
    CONSTRAINT `ck_grade_assessment_score` CHECK (`score` IS NULL OR `score` >= 0),
    CONSTRAINT `ck_grade_assessment_status` CHECK (`grade_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='成绩评定表';

CREATE TABLE `course_grade_publish` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `course_id` BIGINT UNSIGNED NOT NULL COMMENT '课程ID',
    `publish_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '发布状态：1未发布 2已发布 3已锁定',
    `published_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '发布人ID',
    `published_at` DATETIME DEFAULT NULL COMMENT '发布时间',
    `locked_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '锁定人ID',
    `locked_at` DATETIME DEFAULT NULL COMMENT '锁定时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_grade_publish_course_id` (`course_id`),
    KEY `idx_course_grade_publish_status` (`publish_status`),
    CONSTRAINT `ck_course_grade_publish_status` CHECK (`publish_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='课程成绩发布表';
