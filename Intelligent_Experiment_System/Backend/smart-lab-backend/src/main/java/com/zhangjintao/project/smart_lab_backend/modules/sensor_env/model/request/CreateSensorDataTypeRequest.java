package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSensorDataTypeRequest {

	@NotBlank(message = "检测数据名称不能为空")
	@Size(max = 64, message = "检测数据名称长度不能超过64")
	@Pattern(regexp = SensorEnvConstants.DATA_NAME_REGEX, message = "检测数据名称必须使用lower_snake_case")
	private String dataName;

	@NotBlank(message = "检测数据单位不能为空")
	@Size(max = 16, message = "检测数据单位长度不能超过16")
	private String unit;
}
