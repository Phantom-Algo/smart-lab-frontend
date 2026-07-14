package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorRuntimeSnapshot;

import java.time.LocalDateTime;

public interface SensorRuntimeService {
	SensorRuntimeSnapshot getBySensorNo(String sensorNo);
	SensorRuntimeSnapshot recordSuccessfulReport(String sensorNo, LocalDateTime reportedAt);
}
