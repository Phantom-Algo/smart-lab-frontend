package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionSafeRangeSegment {

	private BigDecimal min;
	private BigDecimal max;
}
