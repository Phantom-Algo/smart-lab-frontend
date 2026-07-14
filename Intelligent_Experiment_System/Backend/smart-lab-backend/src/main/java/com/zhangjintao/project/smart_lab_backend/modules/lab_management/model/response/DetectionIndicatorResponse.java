package com.zhangjintao.project.smart_lab_backend.modules.lab_management.model.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DetectionIndicatorResponse {

	private Long id;
	private Long labTypeId;
	private String indicatorName;
	private String unit;
	private List<List<BigDecimal>> safeRange;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
