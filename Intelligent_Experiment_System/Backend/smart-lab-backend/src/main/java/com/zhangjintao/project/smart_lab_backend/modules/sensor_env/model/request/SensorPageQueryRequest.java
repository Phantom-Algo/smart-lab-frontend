package com.zhangjintao.project.smart_lab_backend.modules.sensor_env.model.request;

import com.zhangjintao.project.smart_lab_backend.modules.sensor_env.constants.SensorEnvConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SensorPageQueryRequest {

	@Size(max = 64, message = "搜索关键词长度不能超过64")
	private String keyword;

	@Positive(message = "传感器类型ID必须为正数")
	private Long sensorTypeId;

	@Min(value = 1, message = "目标类型只能是1或2")
	@Max(value = 2, message = "目标类型只能是1或2")
	private Integer targetType;

	@Min(value = 1, message = "运行状态只能是1到3")
	@Max(value = 3, message = "运行状态只能是1到3")
	private Integer runningStatus;

	@Min(value = 1, message = "页码必须大于等于1")
	private Long page = SensorEnvConstants.DEFAULT_PAGE;

	@Min(value = 1, message = "每页条数必须大于等于1")
	@Max(value = 100, message = "每页条数不能超过100")
	private Long pageSize = SensorEnvConstants.DEFAULT_PAGE_SIZE;

	@Size(max = 32, message = "排序字段长度不能超过32")
	private String sortField = "created_at";

	@Pattern(regexp = "(?i)asc|desc", message = "排序方向只能是asc或desc")
	private String sortOrder = "desc";
}
