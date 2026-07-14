-- ============================================================
-- 智能化设备管理子系统 - 数据库Schema
-- 版本: V20260710000001
-- 包含9张表:
--   1. equipment_type          设备类型表
--   2. equipment_metric_indicator  设备监测指标表
--   3. device                  设备主表
--   4. device_borrow_apply     设备借用申请表
--   5. device_borrow_log       设备借用日志表
--   6. device_repair_record    设备维修记录表
--   7. device_repair_cost      设备维修费用明细表
--   8. device_data_monitor_log 设备监测日志表
--   9. device_alert_record     设备告警记录表
-- ============================================================

-- 1. 设备类型表
CREATE TABLE `equipment_type` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type_name` VARCHAR(64) NOT NULL COMMENT '设备类型名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '类型描述',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_name` (`type_name`) COMMENT '类型名全局唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备类型表';

-- 2. 设备监测指标表
CREATE TABLE `equipment_metric_indicator` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `equipment_type_id` BIGINT UNSIGNED NOT NULL COMMENT '关联设备类型ID，逻辑外键→equipment_type.id',
  `metric_name` VARCHAR(64) NOT NULL COMMENT '指标名称，如 temperature / speed / vibration',
  `unit` VARCHAR(32) DEFAULT NULL COMMENT '指标单位，如 ℃ / rpm / %',
  `safe_scope` JSON DEFAULT NULL COMMENT '安全范围，JSON格式：{"min":30,"max":70} 或 {"max":100} 或 {"min":0}',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emi_equipment_type_id` (`equipment_type_id`) COMMENT '按设备类型查询指标',
  UNIQUE KEY `uk_emi_type_metric` (`equipment_type_id`, `metric_name`) COMMENT '同一类型下指标名唯一'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备监测指标表，定义各类设备可监测的指标及安全范围';

-- 3. 设备主表
CREATE TABLE `device` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_no` VARCHAR(64) NOT NULL COMMENT '设备编号，全局唯一',
  `name` VARCHAR(128) NOT NULL COMMENT '设备名称',
  `device_type_id` BIGINT UNSIGNED NOT NULL COMMENT '设备类型ID，逻辑外键→equipment_type.id',
  `lab_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '所属实验室ID，逻辑外键→实验场地管理子系统.lab.id',
  `purchase_date` DATE DEFAULT NULL COMMENT '购入日期',
  `purchase_price` DECIMAL(10, 2) DEFAULT NULL COMMENT '购置价格',
  `expire_date` DATE DEFAULT NULL COMMENT '保修到期日',
  `device_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '设备状态：1采购待入库 2闲置 3借用中 4维修中 5报废',
  `health_score` TINYINT UNSIGNED NOT NULL DEFAULT 100 COMMENT '健康分，取值范围0-100',
  `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注，可不填',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_no` (`device_no`) COMMENT '设备编号全局唯一',
  KEY `idx_device_type_id` (`device_type_id`) COMMENT '按设备类型查询',
  KEY `idx_lab_id` (`lab_id`) COMMENT '按所属实验室查询',
  KEY `idx_device_status` (`device_status`) COMMENT '按设备状态筛选',
  CONSTRAINT `ck_device_status` CHECK (`device_status` BETWEEN 1 AND 5),
  CONSTRAINT `ck_device_health_score` CHECK (`health_score` BETWEEN 0 AND 100)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备主表';

-- 4. 设备借用申请表
CREATE TABLE `device_borrow_apply` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_no` VARCHAR(64) NOT NULL COMMENT '申请单号，全局唯一',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
  `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人ID',
  `approver_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审批人ID（管理员）',
  `expected_start_time` DATETIME(3) NOT NULL COMMENT '预计开始使用时间',
  `expected_return_time` DATETIME(3) NOT NULL COMMENT '预计归还时间',
  `purpose` VARCHAR(255) DEFAULT NULL COMMENT '借用用途',
  `apply_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '申请审批状态：1待审核 2审批通过 3已驳回 4已取消',
  `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '驳回原因',
  `is_overdue` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否逾期：0否 1是',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_apply_no` (`apply_no`) COMMENT '申请单号全局唯一',
  KEY `idx_apply_device_id` (`device_id`) COMMENT '按设备查询',
  KEY `idx_apply_applicant_id` (`applicant_id`) COMMENT '按申请人查询',
  KEY `idx_apply_status` (`apply_status`) COMMENT '按状态筛选',
  CONSTRAINT `ck_apply_status` CHECK (`apply_status` BETWEEN 1 AND 4)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备借用申请表';

-- 5. 设备借用日志表
CREATE TABLE `device_borrow_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `apply_id` BIGINT UNSIGNED NOT NULL COMMENT '关联申请单ID',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
  `borrower_id` BIGINT UNSIGNED NOT NULL COMMENT '借用人ID',
  `out_operator_id` BIGINT UNSIGNED NOT NULL COMMENT '领出登记人ID（管理员）',
  `return_operator_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '归还登记人ID（管理员）',
  `expected_return_time` DATETIME(3) NOT NULL COMMENT '预计归还时间',
  `actual_out_time` DATETIME(3) NOT NULL COMMENT '实际领出时间',
  `actual_return_time` DATETIME(3) DEFAULT NULL COMMENT '实际归还时间',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '借用状态：1借出中 2已归还 3逾期未还',
  `is_overdue` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否逾期：0否 1是',
  `return_remark` VARCHAR(255) DEFAULT NULL COMMENT '归还备注',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_apply_id` (`apply_id`) COMMENT '关联申请单索引',
  KEY `idx_log_device_id` (`device_id`) COMMENT '按设备查询',
  KEY `idx_log_borrower_id` (`borrower_id`) COMMENT '按借用人查询',
  KEY `idx_log_status` (`status`) COMMENT '按状态筛选',
  CONSTRAINT `ck_borrow_log_status` CHECK (`status` BETWEEN 1 AND 3)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备借用日志表';

-- 6. 设备维修记录表
CREATE TABLE `device_repair_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_no` VARCHAR(64) NOT NULL COMMENT '维修单号，全局唯一',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
  `reporter_id` BIGINT UNSIGNED NOT NULL COMMENT '报修人ID',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID（管理员）',
  `report_time` DATETIME(3) NOT NULL COMMENT '报修时间',
  `fault_desc` VARCHAR(500) NOT NULL COMMENT '故障描述',
  `repair_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '维修状态：1待处理 2维修中 3已修复 4已验收',
  `repair_desc` VARCHAR(500) DEFAULT NULL COMMENT '维修处理描述',
  `completed_at` DATETIME(3) DEFAULT NULL COMMENT '维修完成时间',
  `accepted_at` DATETIME(3) DEFAULT NULL COMMENT '验收时间',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_no` (`repair_no`) COMMENT '维修单号全局唯一',
  KEY `idx_repair_device_id` (`device_id`) COMMENT '按设备查询',
  KEY `idx_repair_reporter_id` (`reporter_id`) COMMENT '按报修人查询',
  KEY `idx_repair_status` (`repair_status`) COMMENT '按状态筛选',
  CONSTRAINT `ck_repair_status` CHECK (`repair_status` BETWEEN 1 AND 4)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备维修记录表';

-- 7. 设备维修费用明细表
CREATE TABLE `device_repair_cost` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_id` BIGINT UNSIGNED NOT NULL COMMENT '关联维修单ID，唯一',
  `cost` DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '维修费用',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '费用备注',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_repair_cost_repair_id` (`repair_id`) COMMENT '一维修单对应一条费用记录',
  CONSTRAINT `fk_repair_cost_repair_id` FOREIGN KEY (`repair_id`) REFERENCES `device_repair_record` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备维修费用明细表';

-- 8. 设备监测日志表
CREATE TABLE `device_data_monitor_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
  `sensor_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源传感器ID（物联网侧）',
  `detected_data` JSON NOT NULL COMMENT '监测数据快照，如 {"temperature": {"quantity": 38.5, "unit": "℃"}}',
  `is_safe` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否安全：0有参数超限 1全部安全',
  `detected_at` DATETIME(3) NOT NULL COMMENT '数据采集时间（物联网上报时间）',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_monitor_device_id` (`device_id`) COMMENT '按设备查询监测历史',
  KEY `idx_monitor_device_time` (`device_id`, `detected_at`) COMMENT '按设备+时间范围查询',
  KEY `idx_monitor_device_safe` (`device_id`, `is_safe`, `detected_at`) COMMENT '按设备+安全状态+时间查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备监测日志表，IoT推送的时序监测历史记录';

-- 9. 设备告警记录表
CREATE TABLE `device_alert_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID',
  `sensor_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '来源传感器ID（物联网侧）',
  `param_name` VARCHAR(64) NOT NULL COMMENT '超限参数名称',
  `actual_value` DECIMAL(10, 2) NOT NULL COMMENT '实际监测值',
  `safe_scope` JSON NOT NULL COMMENT '安全范围快照（告警时的配置，不随后续修改变化）',
  `exceed_direction` TINYINT UNSIGNED NOT NULL COMMENT '超限方向：1超上限 2低下限',
  `alert_time` DATETIME(3) NOT NULL COMMENT '告警发生时间',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '处理状态：0未处理 1已处理',
  `handler_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '处理人ID',
  `handle_result` VARCHAR(255) DEFAULT NULL COMMENT '处理结果',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_alert_device_id` (`device_id`) COMMENT '按设备查询告警',
  KEY `idx_alert_device_status` (`device_id`, `status`) COMMENT '按设备+处理状态查询',
  KEY `idx_alert_device_param` (`device_id`, `param_name`, `alert_time`) COMMENT '按设备+参数+时间查询'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '设备告警记录表，监测值超出安全范围时记录';
