package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class SensorTypeDetailResponse {

	Long id;
	String sensorTypeName;
	String description;
	List<SensorDataTypeResponse> dataTypes;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
