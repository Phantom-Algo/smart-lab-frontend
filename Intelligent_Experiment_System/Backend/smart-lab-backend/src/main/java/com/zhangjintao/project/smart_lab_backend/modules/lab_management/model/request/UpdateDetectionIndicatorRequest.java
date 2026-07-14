package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateDetectionIndicatorRequest {

	@Positive(message = "实验室类型ID必须为正数")
	private Long labTypeId;

	@Size(max = 64, message = "指标名称长度不能超过64")
	private String indicatorName;

	@Size(max = 16, message = "指标单位长度不能超过16")
	private String unit;

	private List<List<BigDecimal>> safeRange;
}
