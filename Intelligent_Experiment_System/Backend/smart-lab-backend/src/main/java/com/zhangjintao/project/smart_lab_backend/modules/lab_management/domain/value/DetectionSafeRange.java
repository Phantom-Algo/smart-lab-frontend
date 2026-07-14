package com.zhangjintao.project.smart_lab_backend.modules.lab_management.domain.value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionSafeRange {

	private List<DetectionSafeRangeSegment> segments;

	public List<List<BigDecimal>> toExternalValue() {
		if (segments == null) {
			return List.of();
		}
		return segments.stream()
				.map(segment -> Arrays.asList(segment.getMin(), segment.getMax()))
				.toList();
	}

	public boolean contains(BigDecimal value) {
		if (value == null) {
			throw new IllegalArgumentException("检测值不能为空");
		}
		if (segments == null || segments.isEmpty()) {
			throw new IllegalStateException("安全区间不能为空");
		}
		segments.forEach(this::validateSegment);
		return segments.stream().anyMatch(segment -> contains(segment, value));
	}

	private boolean contains(DetectionSafeRangeSegment segment, BigDecimal value) {
		validateSegment(segment);
		BigDecimal min = segment.getMin();
		BigDecimal max = segment.getMax();
		boolean greaterThanOrEqualMin = min == null || value.compareTo(min) >= 0;
		boolean lessThanOrEqualMax = max == null || value.compareTo(max) <= 0;
		return greaterThanOrEqualMin && lessThanOrEqualMax;
	}

	private void validateSegment(DetectionSafeRangeSegment segment) {
		if (segment == null) {
			throw new IllegalStateException("安全区间段不能为空");
		}
		BigDecimal min = segment.getMin();
		BigDecimal max = segment.getMax();
		if (min == null && max == null) {
			throw new IllegalStateException("安全区间段至少需要一个边界");
		}
		if (min != null && max != null && min.compareTo(max) > 0) {
			throw new IllegalStateException("安全区间下限不能大于上限");
		}
	}
}
