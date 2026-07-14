package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SensorQueryCriteria {
	String keyword;
	Long sensorTypeId;
	Integer targetType;
	Integer runningStatus;
	Long page;
	Long pageSize;
	String sortField;
	String sortOrder;
}
