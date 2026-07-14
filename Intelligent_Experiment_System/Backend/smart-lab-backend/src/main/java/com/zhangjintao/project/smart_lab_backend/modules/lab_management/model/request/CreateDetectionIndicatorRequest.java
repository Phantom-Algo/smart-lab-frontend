package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateDetectionIndicatorRequest {

	@NotNull(message = "实验室类型ID不能为空")
	@Positive(message = "实验室类型ID必须为正数")
	private Long labTypeId;

	@NotBlank(message = "指标名称不能为空")
	@Size(max = 64, message = "指标名称长度不能超过64")
	private String indicatorName;

	@NotBlank(message = "指标单位不能为空")
	@Size(max = 16, message = "指标单位长度不能超过16")
	private String unit;

	@NotEmpty(message = "安全区间不能为空")
	private List<List<BigDecimal>> safeRange;
}
