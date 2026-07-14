-- ============================================================
-- 物联网环境子系统（sensor_env）数据库 Schema
-- 负责人：张津涛
-- 创建时间：2026-07-09
-- ============================================================

-- -----------------------------------------------------------
-- 1. 传感器类型表
-- -----------------------------------------------------------
CREATE TABLE `sensor_type` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sensor_type_name` VARCHAR(64) NOT NULL COMMENT '传感器类型名称，如：温湿度传感器、烟雾传感器、气体传感器',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '传感器类型描述',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 约束与索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensor_type_name` (`sensor_type_name`) COMMENT '传感器类型名称全局唯一'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='传感器类型表';

-- -----------------------------------------------------------
-- 2. 传感器检测数据类型表
--    关联关系：sensor_type 1 : N sensor_data_type
-- -----------------------------------------------------------
CREATE TABLE `sensor_data_type` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sensor_type_id` BIGINT UNSIGNED NOT NULL COMMENT '传感器类型ID，逻辑关联 sensor_type.id',
    `data_name` VARCHAR(64) NOT NULL COMMENT '检测数据名称，如：temperature/humidity/smoke/gas_concentration',
    `unit` VARCHAR(16) NOT NULL COMMENT '检测数据单位，如：°C/%RH/ppm',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 约束与索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensor_type_data` (`sensor_type_id`, `data_name`) COMMENT '同一传感器类型下数据名称唯一',
    KEY `idx_sensor_data_type_type` (`sensor_type_id`) COMMENT '按传感器类型查询检测数据项'
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='传感器检测数据类型表';

-- -----------------------------------------------------------
-- 3. 传感器表
--    关联关系：
--      sensor_type 1 : N sensor
--      sensor.target_id → device.id（target_type=1） 或 lab.id（target_type=2）
-- -----------------------------------------------------------
CREATE TABLE `sensor` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sensor_no` VARCHAR(64) NOT NULL COMMENT '传感器编号，全局唯一',
    `sensor_type_id` BIGINT UNSIGNED NOT NULL COMMENT '传感器类型ID，逻辑关联 sensor_type.id',
    `target_type` TINYINT UNSIGNED NOT NULL COMMENT '绑定目标类型：1=设备 2=实验室',
    `target_id` BIGINT UNSIGNED NOT NULL COMMENT '绑定目标ID，逻辑关联 device.id 或 lab.id（由 target_type 决定）',
    `running_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '运行状态：1=正常 2=离线 3=故障',
    `last_report_at` DATETIME DEFAULT NULL COMMENT '最近一次数据上报时间',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间（软删除）',
    -- 约束与索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sensor_no` (`sensor_no`) COMMENT '传感器编号全局唯一',
    KEY `idx_sensor_type_id` (`sensor_type_id`) COMMENT '按传感器类型查询',
    KEY `idx_sensor_target` (`target_type`, `target_id`) COMMENT '按目标类型+ID联合查询',
    KEY `idx_sensor_running_status` (`running_status`) COMMENT '按运行状态查询（离线检测定时任务使用）',
    KEY `idx_sensor_deleted_at` (`deleted_at`) COMMENT '软删除索引',
    CONSTRAINT `ck_sensor_target_type` CHECK (`target_type` IN (1, 2)),
    CONSTRAINT `ck_sensor_running_status` CHECK (`running_status` IN (1, 2, 3))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='传感器表';

-- -----------------------------------------------------------
-- 4. 传感器工作日志表
--    关联关系：sensor 1 : N sensor_work_log
-- -----------------------------------------------------------
CREATE TABLE `sensor_work_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sensor_id` BIGINT UNSIGNED NOT NULL COMMENT '传感器ID，逻辑关联 sensor.id',
    `sensor_no` VARCHAR(64) NOT NULL COMMENT '传感器编号（冗余，便于直接查询）',
    `detected_data` JSON NOT NULL COMMENT '检测数据快照，格式：{"temperature": 25.6, "humidity": 68.0}',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '数据状态：1=正常 2=异常（超出阈值）',
    `detected_at` DATETIME NOT NULL COMMENT '传感器采集时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '写入数据库时间',
    -- 约束与索引
    PRIMARY KEY (`id`),
    KEY `idx_worklog_sensor_time` (`sensor_id`, `detected_at`) COMMENT '按传感器+时间查询历史趋势',
    KEY `idx_worklog_sensor_no_time` (`sensor_no`, `detected_at`) COMMENT '按传感器编号+时间查询',
    KEY `idx_worklog_status_time` (`status`, `detected_at`) COMMENT '按状态+时间筛选异常日志',
    KEY `idx_worklog_detected_at` (`detected_at`) COMMENT '按检测时间范围查询（数据归档使用）',
    CONSTRAINT `ck_worklog_status` CHECK (`status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='传感器工作日志表';

-- -----------------------------------------------------------
-- 5. 设备-传感器绑定表
--    关联关系：device 1 : N device_sensor_binding, sensor 1 : 1 device_sensor_binding
--    说明：设备表（device）属于智能化设备管理子系统，当前尚未创建
-- -----------------------------------------------------------
CREATE TABLE `device_sensor_binding` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `device_id` BIGINT UNSIGNED NOT NULL COMMENT '设备ID（逻辑关联设备管理子系统的 device.id）',
    `sensor_id` BIGINT UNSIGNED NOT NULL COMMENT '传感器ID，逻辑关联 sensor.id',
    `bind_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '绑定状态：1=已绑定 2=已解绑',
    `bound_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `unbound_at` DATETIME DEFAULT NULL COMMENT '解绑时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- 约束与索引
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_device_sensor_bind` (`device_id`, `sensor_id`) COMMENT '设备-传感器绑定唯一约束，同一传感器只能绑定一次',
    KEY `idx_sensor_binding_device` (`device_id`) COMMENT '按设备ID查询其下所有传感器',
    CONSTRAINT `ck_device_sensor_bind_status` CHECK (`bind_status` IN (1, 2))
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='设备传感器绑定表';
