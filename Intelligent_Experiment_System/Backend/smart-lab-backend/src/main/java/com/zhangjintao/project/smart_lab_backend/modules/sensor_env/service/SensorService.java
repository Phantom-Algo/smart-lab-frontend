package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.common.result.PageResult;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorQueryCriteria;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorStatusUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorResponse;

public interface SensorService {
	SensorResponse create(SensorCreateCommand command);
	SensorResponse getById(Long sensorId);
	PageResult<SensorResponse> page(SensorQueryCriteria criteria);
	SensorResponse update(Long sensorId, SensorUpdateCommand command);
	SensorResponse updateStatus(Long sensorId, SensorStatusUpdateCommand command);
	void delete(Long sensorId);
}
