package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DetectionIndicatorCreateCommand {

	private Long labTypeId;
	private String indicatorName;
	private String unit;
	private List<List<BigDecimal>> safeRange;
}
