package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensorRuntimeSnapshot {
	private Long sensorId;
	private String sensorNo;
	private Long sensorTypeId;
	private Integer targetType;
	private Long targetId;
	private Integer runningStatus;
}
