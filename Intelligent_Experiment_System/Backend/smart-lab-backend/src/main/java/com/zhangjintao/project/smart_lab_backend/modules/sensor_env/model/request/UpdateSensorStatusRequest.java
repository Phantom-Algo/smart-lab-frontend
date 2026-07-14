package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateSensorStatusRequest {

	@NotNull(message = "运行状态不能为空")
	private Integer runningStatus;

	@Size(max = 255, message = "状态变更原因长度不能超过255")
	private String reason;
}
