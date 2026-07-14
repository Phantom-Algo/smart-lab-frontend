package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.enums.DetectionExceedDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionAlertItem {

	private String indicatorName;
	private BigDecimal actualValue;
	private String unit;
	private List<List<BigDecimal>> safeRange;
	private DetectionExceedDirection exceedDirection;
}
