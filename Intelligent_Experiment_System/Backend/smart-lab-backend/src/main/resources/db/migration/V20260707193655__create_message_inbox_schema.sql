CREATE TABLE `msg_template` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '模板主键ID',
    `template_code` VARCHAR(128) NOT NULL COMMENT '模板编码，如 equipment_loan_approved',
    `template_name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `message_type` VARCHAR(128) NOT NULL COMMENT '消息类型，如 equipment.loan.approved',
    `title_template` VARCHAR(255) NOT NULL COMMENT '标题模板',
    `content_template` MEDIUMTEXT NOT NULL COMMENT '正文模板',
    `content_format` VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '内容格式：TEXT / MARKDOWN / HTML',
    `param_schema` JSON DEFAULT NULL COMMENT '模板参数定义，如参数名、类型、是否必填',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '模板状态：0停用，1启用',
    `created_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '创建人用户ID',
    `updated_by` BIGINT UNSIGNED DEFAULT NULL COMMENT '更新人用户ID',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_template_code` (`template_code`),
    KEY `idx_msg_template_type_status` (`message_type`, `status`),
    CONSTRAINT `ck_msg_template_format` CHECK (`content_format` IN ('TEXT', 'MARKDOWN', 'HTML')),
    CONSTRAINT `ck_msg_template_status` CHECK (`status` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='消息模板表';

CREATE TABLE `msg_event` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息事件主键ID',
    `event_no` VARCHAR(160) NOT NULL COMMENT '消息事件编号，格式为 messageType:16位随机数',
    `source_system` VARCHAR(64) NOT NULL COMMENT '来源子系统，如 equipment',
    `message_type` VARCHAR(128) NOT NULL COMMENT '消息类型，如 equipment.loan.approved',
    `priority` TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '优先级：1预警通知，2系统通知，3普通通知',
    `occurred_at` DATETIME(3) NOT NULL COMMENT '原业务事件发生时间',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型：single / multi / all / all_student / all_teacher / all_manager',
    `targets` JSON DEFAULT NULL COMMENT '事件目标用户快照，single/multi 时必填',
    `template_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '正式模板ID',
    `temp_template` JSON DEFAULT NULL COMMENT '临时模板，如 title/content/format',
    `params` JSON DEFAULT NULL COMMENT '模板参数快照',
    `title` VARCHAR(255) DEFAULT NULL COMMENT '最终渲染后的标题快照',
    `content` MEDIUMTEXT DEFAULT NULL COMMENT '最终渲染后的正文快照',
    `content_format` VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '内容格式：TEXT / MARKDOWN / HTML',
    `action_url` VARCHAR(512) DEFAULT NULL COMMENT '用户点击消息后的跳转路径',
    `dispatch_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '分发状态：0待处理，1处理中，2已完成，3部分失败，4失败',
    `retry_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
    `processed_at` DATETIME(3) DEFAULT NULL COMMENT '消息分发完成时间',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_event_no` (`event_no`),
    KEY `idx_msg_event_source_type_time` (`source_system`, `message_type`, `occurred_at`),
    KEY `idx_msg_event_dispatch_status` (`dispatch_status`, `updated_at`),
    KEY `idx_msg_event_template_id` (`template_id`),
    CONSTRAINT `fk_msg_event_template_id` FOREIGN KEY (`template_id`) REFERENCES `msg_template` (`id`),
    CONSTRAINT `ck_msg_event_priority` CHECK (`priority` IN (1, 2, 3)),
    CONSTRAINT `ck_msg_event_target_type` CHECK (`target_type` IN ('single', 'multi', 'all', 'all_student', 'all_teacher', 'all_manager')),
    CONSTRAINT `ck_msg_event_content_format` CHECK (`content_format` IN ('TEXT', 'MARKDOWN', 'HTML')),
    CONSTRAINT `ck_msg_event_dispatch_status` CHECK (`dispatch_status` IN (0, 1, 2, 3, 4))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='消息主体与内部事件落库记录表';

CREATE TABLE `user_inbox_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收件箱消息主键ID',
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '接收用户ID，统一关联登录鉴权子系统的 user_id',
    `message_id` BIGINT UNSIGNED NOT NULL COMMENT '关联消息主体ID',
    `is_read` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    `read_at` DATETIME(3) DEFAULT NULL COMMENT '首次阅读时间',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否删除：0正常，1用户删除',
    `deleted_at` DATETIME(3) DEFAULT NULL COMMENT '用户删除时间',
    `received_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '写入收件箱时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_inbox_message_user_message` (`user_id`, `message_id`),
    KEY `idx_user_inbox_message_list` (`user_id`, `is_deleted`, `received_at` DESC, `id` DESC),
    KEY `idx_user_inbox_message_unread` (`user_id`, `is_read`, `is_deleted`, `received_at` DESC),
    KEY `idx_user_inbox_message_message_id` (`message_id`),
    KEY `idx_user_inbox_message_deleted_at` (`is_deleted`, `deleted_at`),
    CONSTRAINT `fk_user_inbox_message_message_id` FOREIGN KEY (`message_id`) REFERENCES `msg_event` (`id`),
    CONSTRAINT `ck_user_inbox_message_read` CHECK (`is_read` IN (0, 1)),
    CONSTRAINT `ck_user_inbox_message_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户收件箱表';

CREATE TABLE `msg_dead_letter` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '死信记录主键ID',
    `event_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '关联消息事件ID，事件落库前失败时为空',
    `event_no` VARCHAR(160) DEFAULT NULL COMMENT '消息事件编号快照',
    `failure_stage` VARCHAR(64) NOT NULL COMMENT '失败阶段：VALIDATE / RENDER / ROUTE / PERSIST / DISPATCH',
    `error_code` VARCHAR(64) DEFAULT NULL COMMENT '错误编码',
    `error_message` TEXT NOT NULL COMMENT '错误信息',
    `raw_payload` JSON DEFAULT NULL COMMENT '原始事件内容快照',
    `retryable` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否可重试：0否，1是',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理，1已解决，2已忽略',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_dead_letter_event_id` (`event_id`),
    KEY `idx_msg_dead_letter_event_no` (`event_no`),
    KEY `idx_msg_dead_letter_status` (`status`, `created_at`),
    KEY `idx_msg_dead_letter_retryable` (`retryable`, `status`, `created_at`),
    CONSTRAINT `fk_msg_dead_letter_event_id` FOREIGN KEY (`event_id`) REFERENCES `msg_event` (`id`),
    CONSTRAINT `ck_msg_dead_letter_retryable` CHECK (`retryable` IN (0, 1)),
    CONSTRAINT `ck_msg_dead_letter_status` CHECK (`status` IN (0, 1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='消息死信记录表';
