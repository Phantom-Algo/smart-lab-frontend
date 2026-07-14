-- 模块二传感器管理所需索引：离线扫描及按传感器维护绑定关系。
ALTER TABLE `sensor`
    ADD KEY `idx_sensor_offline_report` (`running_status`, `deleted_at`, `last_report_at`),
    ADD KEY `idx_sensor_offline_created` (`running_status`, `deleted_at`, `created_at`);

ALTER TABLE `lab_sensor_binding`
    ADD KEY `idx_lab_sensor_binding_sensor` (`sensor_id`, `bind_status`);

ALTER TABLE `device_sensor_binding`
    ADD KEY `idx_device_sensor_binding_sensor` (`sensor_id`, `bind_status`);
