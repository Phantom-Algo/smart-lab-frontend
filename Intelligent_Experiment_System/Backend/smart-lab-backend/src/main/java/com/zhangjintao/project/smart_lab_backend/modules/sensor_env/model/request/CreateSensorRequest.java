package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSensorRequest {

	@NotBlank(message = "传感器编号不能为空")
	@Pattern(regexp = SensorEnvConstants.SENSOR_NO_REGEX,
			message = "传感器编号只能包含大写字母、数字、短横线和下划线")
	private String sensorNo;

	@NotNull(message = "传感器类型ID不能为空")
	@Positive(message = "传感器类型ID必须为正数")
	private Long sensorTypeId;

	@NotNull(message = "目标类型不能为空")
	@Min(value = 1, message = "目标类型只能是1或2")
	@Max(value = 2, message = "目标类型只能是1或2")
	private Integer targetType;

	@NotNull(message = "目标ID不能为空")
	@Positive(message = "目标ID必须为正数")
	private Long targetId;

	@Size(max = 255, message = "备注长度不能超过255")
	private String remark;
}
