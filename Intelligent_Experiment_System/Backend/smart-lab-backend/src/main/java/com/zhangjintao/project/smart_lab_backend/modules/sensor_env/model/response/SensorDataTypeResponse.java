package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SensorDataTypeResponse {

	Long id;
	Long sensorTypeId;
	String dataName;
	String unit;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
