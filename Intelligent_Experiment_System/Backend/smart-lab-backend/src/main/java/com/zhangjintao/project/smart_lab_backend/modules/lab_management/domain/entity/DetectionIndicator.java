package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.entity;

import com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value.DetectionSafeRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionIndicator {

	private Long id;
	private Long labTypeId;
	private String indicatorName;
	private String unit;
	private DetectionSafeRange safeRange;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	public void applyUpdate(Long labTypeId, String indicatorName, String unit, DetectionSafeRange safeRange) {
		if (labTypeId != null) {
			this.labTypeId = labTypeId;
		}
		if (indicatorName != null) {
			this.indicatorName = indicatorName;
		}
		if (unit != null) {
			this.unit = unit;
		}
		if (safeRange != null) {
			this.safeRange = safeRange;
		}
	}
}
