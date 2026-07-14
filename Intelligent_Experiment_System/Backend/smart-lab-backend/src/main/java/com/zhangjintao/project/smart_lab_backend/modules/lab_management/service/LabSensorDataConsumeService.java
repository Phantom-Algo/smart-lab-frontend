package com.zhangjintao.project.smart_lab_backend.modules.lab_management.service;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto.LabSensorDataConsumeResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.domain.entity.SensorDataMessage;

public interface LabSensorDataConsumeService {

	LabSensorDataConsumeResult recordAndEvaluate(SensorDataMessage message);
}
