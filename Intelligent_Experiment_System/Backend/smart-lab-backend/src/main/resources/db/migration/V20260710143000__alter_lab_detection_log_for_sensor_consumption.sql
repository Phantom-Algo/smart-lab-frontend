ALTER TABLE `lab_detection_log`
  ADD COLUMN `event_no` VARCHAR(160) NOT NULL COMMENT 'IoT事件编号，用于消费幂等' AFTER `id`,
  ADD COLUMN `message_id` VARCHAR(64) DEFAULT NULL COMMENT 'RocketMQ消息体中的messageId' AFTER `event_no`,
  ADD COLUMN `sensor_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '传感器ID快照' AFTER `lab_id`,
  ADD COLUMN `sensor_no` VARCHAR(64) DEFAULT NULL COMMENT '传感器编号快照' AFTER `sensor_id`,
  ADD COLUMN `detected_at` DATETIME(3) DEFAULT NULL COMMENT '传感器采集时间' AFTER `detected_data`,
  ADD COLUMN `alert_detail` JSON DEFAULT NULL COMMENT '异常检测详情快照' AFTER `status`,
  ADD UNIQUE KEY `uk_lab_detection_event_no` (`event_no`),
  ADD KEY `idx_lab_detection_sensor_time` (`sensor_id`, `detected_at`);
