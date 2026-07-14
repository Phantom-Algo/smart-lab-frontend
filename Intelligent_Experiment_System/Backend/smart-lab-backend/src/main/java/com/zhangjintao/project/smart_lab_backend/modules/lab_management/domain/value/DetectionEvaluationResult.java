package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionEvaluationResult {

	private List<DetectionAlertItem> alertItems;
	private List<String> unknownIndicatorNames;

	public boolean hasAlert() {
		return alertItems != null && !alertItems.isEmpty();
	}
}
