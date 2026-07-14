package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSensorTypeRequest {

	@NotBlank(message = "传感器类型名称不能为空")
	@Size(max = 64, message = "传感器类型名称长度不能超过64")
	private String sensorTypeName;

	@Size(max = 255, message = "传感器类型描述长度不能超过255")
	private String description;
}
