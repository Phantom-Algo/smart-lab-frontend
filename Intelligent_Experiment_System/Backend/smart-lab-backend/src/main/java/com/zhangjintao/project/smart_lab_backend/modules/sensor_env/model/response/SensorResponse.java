package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.response;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SensorResponse {
	Long id;
	String sensorNo;
	Long sensorTypeId;
	String sensorTypeName;
	Integer targetType;
	String targetTypeDescription;
	Long targetId;
	Integer runningStatus;
	String runningStatusDescription;
	LocalDateTime lastReportAt;
	String remark;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
}
