package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SensorUpdateCommand {
	Long sensorTypeId;
	Integer targetType;
	Long targetId;
	String remark;
}
