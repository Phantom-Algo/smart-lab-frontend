package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SensorTypeListItemResponse {

	Long id;
	String sensorTypeName;
	String description;
	int dataTypeCount;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
