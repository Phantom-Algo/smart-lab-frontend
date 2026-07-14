CREATE TABLE `lab` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_name` VARCHAR(128) NOT NULL COMMENT '实验室名称',
  `lab_type_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室类型ID，逻辑关联 lab_type.id',
  `campus` VARCHAR(64) DEFAULT NULL COMMENT '校区',
  `building` VARCHAR(64) DEFAULT NULL COMMENT '楼栋',
  `floor` TINYINT UNSIGNED DEFAULT NULL COMMENT '楼层',
  `room_no` VARCHAR(32) DEFAULT NULL COMMENT '房间号',
  `capacity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '最大容纳人数',
  `area_m2` DECIMAL(8, 2) DEFAULT NULL COMMENT '面积，单位为平方米',
  `duty_user_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人用户ID，逻辑关联 user_account.id',
  `running_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '使用状态：1空闲 2使用中 3维护中 4停用',
  `open_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '开放状态：1正常开放 2临时关闭 3永久关闭',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间，软删除',
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  KEY `idx_lab_type_id` (`lab_type_id`) COMMENT '实验室类型ID索引，优化按类型查询',
  KEY `idx_lab_campus` (`campus`) COMMENT '校区索引',
  KEY `idx_lab_building_floor_room` (`building`, `floor`, `room_no`) COMMENT '楼栋楼层房间号联合索引',
  KEY `idx_lab_duty_user_id` (`duty_user_id`) COMMENT '负责人ID索引',
  KEY `idx_lab_running_status` (`running_status`) COMMENT '使用状态索引',
  KEY `idx_lab_open_status` (`open_status`) COMMENT '开放状态索引',
  KEY `idx_lab_deleted_at` (`deleted_at`) COMMENT '软删除索引',
  CONSTRAINT `ck_lab_capacity` CHECK (`capacity` >= 0),
  CONSTRAINT `ck_lab_area` CHECK (`area_m2` IS NULL OR `area_m2` > 0),
  CONSTRAINT `ck_lab_running_status` CHECK (`running_status` IN (1, 2, 3, 4)),
  CONSTRAINT `ck_lab_open_status` CHECK (`open_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室表';



CREATE TABLE `lab_type` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_type_name` VARCHAR(64) NOT NULL COMMENT '实验室类型名称',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_type_name` (`lab_type_name`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室类型表';



  CREATE TABLE `lab_detection_indicator` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `lab_type_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室类型id，关联 lab_type.id',
    `indicator_name` VARCHAR(64) NOT NULL COMMENT '指标名称',
    `unit` VARCHAR(16) NOT NULL COMMENT '单位',
    `safe_range` JSON NOT NULL COMMENT '安全范围,如[[-10,30], [50, 100], [120, null]]',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_lab_type_indicator` (`lab_type_id`, `indicator_name`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室检测指标表';



CREATE TABLE `lab_detection_log` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `detected_data` JSON NOT NULL COMMENT '被检测的数据记录',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态：1正常 2数据异常/触发告警',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检测记录时间（也是写入时间）',
  
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  -- 核心索引：支撑“特定实验室指定指标的历史趋势图”查询
  KEY `idx_lab_indicator_time` (`lab_id`, `created_at`),
  -- 辅助索引：用于管理后台或大屏快速筛选“未处理的异常告警日志”
  KEY `idx_status_time` (`status`, `created_at`)
) ENGINE = InnoDB 
DEFAULT CHARSET = utf8mb4 
COLLATE = utf8mb4_0900_ai_ci 
COMMENT = '实验室检测日志表';



CREATE TABLE `lab_reservation` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reservation_no` VARCHAR(32) NOT NULL COMMENT '预约单号',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `applicant_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人ID，逻辑关联 user_account.id',
  `reservation_date` DATE NOT NULL COMMENT '预约日期',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间，需大于 start_time',
  `purpose` VARCHAR(512) DEFAULT NULL COMMENT '实验内容/目的',
  `course_name` VARCHAR(128) DEFAULT NULL COMMENT '关联课程名称',
  `reservation_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '预约状态：1待审核 2已通过 3已驳回 4已取消 5已签到 6已爽约',
  `approver_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '审核人ID，逻辑关联 user_account.id',
  `approve_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `approve_comment` VARCHAR(255) DEFAULT NULL COMMENT '审核意见',
  `check_in_at` DATETIME DEFAULT NULL COMMENT '签到时间',
  `check_out_at` DATETIME DEFAULT NULL COMMENT '签退时间',
  `cancel_reason` VARCHAR(255) DEFAULT NULL COMMENT '取消原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reservation_no` (`reservation_no`) COMMENT '预约单号全局唯一约束',
  KEY `idx_reservation_lab_date` (`lab_id`, `reservation_date`) COMMENT '实验室日期联合索引，优化按实验室+日期查询',
  KEY `idx_reservation_lab_time` (`lab_id`, `reservation_date`, `start_time`, `end_time`) COMMENT '实验室时间段联合索引，用于冲突检测',
  KEY `idx_reservation_applicant` (`applicant_id`, `reservation_status`) COMMENT '申请人索引，优化个人预约列表查询',
  KEY `idx_reservation_status` (`reservation_status`, `reservation_date`) COMMENT '预约状态索引，优化待审核列表查询',
  KEY `idx_reservation_approver` (`approver_id`, `reservation_status`) COMMENT '审核人索引',
  CONSTRAINT `ck_reservation_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `ck_reservation_status` CHECK (`reservation_status` IN (1, 2, 3, 4, 5, 6))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室预约表';



CREATE TABLE `lab_sensor_binding` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `sensor_id` BIGINT UNSIGNED NOT NULL COMMENT '传感器ID，关联 IoT 子系统传感器',
  `bind_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '绑定状态：1已绑定 2已解绑',
  `bound_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `unbound_at` DATETIME DEFAULT NULL COMMENT '解绑时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_sensor_bind` (`lab_id`, `sensor_id`) COMMENT '实验室传感器绑定唯一约束，同一传感器只能绑定一次',
  KEY `idx_sensor_binding_lab` (`lab_id`) COMMENT '实验室ID索引，查询实验室下所有传感器',
  CONSTRAINT `ck_sensor_bind_status` CHECK (`bind_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室传感器绑定表';



CREATE TABLE `lab_schedule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `day_of_week` TINYINT UNSIGNED NOT NULL COMMENT '星期几：1周一 2周二 3周三 4周四 5周五 6周六 7周日',
  `open_time` TIME NOT NULL COMMENT '开放时间',
  `close_time` TIME NOT NULL COMMENT '关闭时间，需大于 open_time',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lab_schedule` (`lab_id`, `day_of_week`) COMMENT '实验室日程唯一约束，同一实验室同一天不能有重复设置',
  KEY `idx_schedule_lab` (`lab_id`) COMMENT '实验室ID索引',
  CONSTRAINT `ck_schedule_time` CHECK (`close_time` > `open_time`),
  CONSTRAINT `ck_schedule_day` CHECK (`day_of_week` BETWEEN 1 AND 7)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='实验室开放时间表';



CREATE TABLE `lab_close_schedule` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `lab_id` BIGINT UNSIGNED NOT NULL COMMENT '实验室ID，逻辑关联 lab.id',
  `close_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '不开放类型：1节假日 2设备维保 3临时关闭 4其他',
  `start_time` DATETIME NOT NULL COMMENT '不开放开始时间（含年月日时分）',
  `end_time` DATETIME NOT NULL COMMENT '不开放结束时间（需大于 start_time）',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '不开放原因/备注说明',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  -- 约束与索引定义
  PRIMARY KEY (`id`),
  KEY `idx_close_lab_time` (`lab_id`, `start_time`, `end_time`) COMMENT '实验室及不开放时间段索引，用于冲突校验与查询',
  CONSTRAINT `ck_close_time` CHECK (`end_time` > `start_time`),
  CONSTRAINT `ck_close_type` CHECK (`close_type` IN (1, 2, 3, 4))
) ENGINE = InnoDB 
  DEFAULT CHARSET = utf8mb4 
  COLLATE = utf8mb4_0900_ai_ci 
  COMMENT = '实验室临时不开放时间表';
