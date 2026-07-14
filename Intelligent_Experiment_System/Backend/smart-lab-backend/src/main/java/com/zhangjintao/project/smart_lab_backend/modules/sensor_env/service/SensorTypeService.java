package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.service;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorDataTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeCreateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto.SensorTypeUpdateCommand;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorDataTypeResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeDetailResponse;
import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response.SensorTypeListItemResponse;

import java.util.List;

public interface SensorTypeService {

	SensorTypeDetailResponse create(SensorTypeCreateCommand command);

	List<SensorTypeListItemResponse> listAll();

	SensorTypeDetailResponse getById(Long sensorTypeId);

	SensorTypeDetailResponse update(Long sensorTypeId, SensorTypeUpdateCommand command);

	void delete(Long sensorTypeId);

	SensorDataTypeResponse createDataType(Long sensorTypeId, SensorDataTypeCreateCommand command);

	SensorDataTypeResponse updateDataType(Long dataTypeId, SensorDataTypeUpdateCommand command);

	void deleteDataType(Long dataTypeId);
}
