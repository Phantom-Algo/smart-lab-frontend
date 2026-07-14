CREATE TABLE `lab_status_change_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人用户ID，系统触发时可为空',
  `from_running_status` TINYINT UNSIGNED NOT NULL COMMENT '变更前使用状态',
  `to_running_status` TINYINT UNSIGNED NOT NULL COMMENT '变更后使用状态',
  `from_open_status` TINYINT UNSIGNED NOT NULL COMMENT '变更前开放状态',
  `to_open_status` TINYINT UNSIGNED NOT NULL COMMENT '变更后开放状态',
  `change_reason` VARCHAR(255) NOT NULL COMMENT '变更原因',
  `trigger_source` VARCHAR(32) NOT NULL COMMENT '触发来源：MANUAL/AUTO_CHECKOUT/AUTO_ALERT',
  `forced_checkout_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '强制签退数量',
  `cancelled_reservation_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '取消预约数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_lab_status_log_lab_time` (`lab_id`, `created_at`),
  KEY `idx_lab_status_log_operator_time` (`operator_id`, `created_at`),
  CONSTRAINT `ck_lab_status_log_from_running` CHECK (`from_running_status` IN (1, 2, 3, 4)),
  CONSTRAINT `ck_lab_status_log_to_running` CHECK (`to_running_status` IN (1, 2, 3, 4)),
  CONSTRAINT `ck_lab_status_log_from_open` CHECK (`from_open_status` IN (1, 2, 3)),
  CONSTRAINT `ck_lab_status_log_to_open` CHECK (`to_open_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室状态变更日志表';
